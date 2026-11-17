import { test, expect } from 'playwright/test';
import { API, ADMIN, bearer, login, registerUser, createProduct, retireProduct, getProduct, placeOrder } from './helpers.mjs';

// Real gateway -> real services -> PostgreSQL. Nothing here is mocked.
test.describe('identity and authorization through the gateway', () => {
  let admin; let alice; let mallory; let product;

  test.beforeAll(async ({ request }) => {
    admin = await login(request, ADMIN.email, ADMIN.password);
    alice = await registerUser(request, 'Alice');
    mallory = await registerUser(request, 'Mallory');
    product = await createProduct(request, admin.token, { stock: 4, price: 2500 });
  });
  test.afterAll(async ({ request }) => retireProduct(request, admin.token, product.id));

  test('anyone can browse the catalogue', async ({ request }) => {
    const r = await request.get(`${API}/api/products?q=${encodeURIComponent(product.referenceNumber)}`);
    expect(r.status()).toBe(200);
    expect((await r.json()).totalElements).toBe(1);
  });

  test('forged identity headers grant nothing', async ({ request }) => {
    const forged = { 'X-User-Role': 'ADMIN', 'X-User-Id': crypto.randomUUID(), 'X-User-Email': 'x@x.test', 'X-Internal-Token': 'guess' };
    const body = { name: 'Forged', brandId: 1, categoryId: 1, price: 1 };
    expect((await request.post(`${API}/api/admin/products`, { headers: forged, data: body })).status()).toBe(401);
    expect((await request.post(`${API}/api/admin/products`, { headers: { ...forged, ...bearer(alice.token) }, data: body })).status()).toBe(403);
    expect((await request.get(`${API}/api/orders`, { headers: forged })).status()).toBe(401);
  });

  test('a normal user cannot use admin functions, an administrator can', async ({ request }) => {
    for (const path of ['/api/admin/products', '/api/admin/orders', '/api/admin/payments', '/api/admin/users', '/api/admin/inventory/summary', '/api/admin/notifications']) {
      expect((await request.get(API + path, { headers: bearer(alice.token) })).status(), path).toBe(403);
      expect((await request.get(API + path, { headers: bearer(admin.token) })).status(), path).toBe(200);
      expect((await request.get(API + path)).status(), path).toBe(401);
    }
  });

  test('a normal user cannot change products or stock', async ({ request }) => {
    const h = bearer(alice.token);
    expect((await request.put(`${API}/api/admin/products/${product.id}`, { headers: h, data: { price: 1 } })).status()).toBe(403);
    expect((await request.post(`${API}/api/admin/products/${product.id}/stock-adjustments`, { headers: h, data: { delta: -4 } })).status()).toBe(403);
    expect((await request.patch(`${API}/api/products/${product.id}/stock`, { headers: h, data: { action: 'reserve', quantity: 4 } })).status()).toBeGreaterThanOrEqual(400);
    expect((await request.post(`${API}/internal/stock/reservations`, { headers: h, data: { orderId: crypto.randomUUID(), items: [{ productId: product.id, quantity: 4 }] } })).status()).toBe(404);
    const current = await getProduct(request, product.id);
    expect(current.stock).toBe(4);
    expect(current.price).toBe(2500);
  });

  test("a user cannot read, cancel or pay for someone else's order, nor mark any order paid", async ({ request }) => {
    const created = await placeOrder(request, alice.token, product.id);
    expect(created.status()).toBe(201);
    const order = await created.json();

    const m = bearer(mallory.token);
    expect((await request.get(`${API}/api/orders/${order.id}`, { headers: m })).status()).toBe(404);
    expect((await request.post(`${API}/api/orders/${order.id}/cancel`, { headers: m })).status()).toBe(404);
    expect((await request.post(`${API}/api/payments/preference`, { headers: m, data: { orderId: order.id } })).status()).toBe(404);
    expect((await request.get(`${API}/api/payments/order/${order.id}`, { headers: m })).status()).toBe(404);
    expect((await (await request.get(`${API}/api/orders`, { headers: m })).json()).length).toBe(0);

    const paid = { status: 'PAID' };
    expect((await request.put(`${API}/api/orders/${order.id}/status`, { headers: bearer(alice.token), data: paid })).status()).toBe(404);
    expect((await request.put(`${API}/api/admin/orders/${order.id}/status`, { headers: bearer(alice.token), data: paid })).status()).toBe(403);
    expect((await request.put(`${API}/api/admin/orders/${order.id}/status`, { headers: bearer(admin.token), data: paid })).status()).toBe(409);
    expect((await request.post(`${API}/internal/orders/${order.id}/payment-result`, { headers: bearer(admin.token), data: { result: 'APPROVED', paymentId: 'x' } })).status()).toBe(404);

    const mine = await (await request.get(`${API}/api/orders/${order.id}`, { headers: bearer(alice.token) })).json();
    expect(mine.status).toBe('PENDING');
    expect((await request.post(`${API}/api/orders/${order.id}/cancel`, { headers: bearer(alice.token) })).status()).toBe(200);
  });

  test('sessions: expired or invalid tokens get 401, refresh tokens are single use', async ({ request }) => {
    expect((await request.get(`${API}/api/orders`, { headers: bearer('not.a.token') })).status()).toBe(401);
    const session = await registerUser(request, 'Sess');
    const refreshed = await request.post(`${API}/api/auth/refresh`, { data: { refreshToken: session.refreshToken } });
    expect(refreshed.status()).toBe(200);
    expect((await request.post(`${API}/api/auth/refresh`, { data: { refreshToken: session.refreshToken } })).status()).toBe(401);
    expect((await request.get(`${API}/api/orders`, { headers: bearer(session.refreshToken) })).status()).toBe(401);
  });

  test('validation errors are explained and internals are not leaked', async ({ request }) => {
    const weak = await request.post(`${API}/api/auth/register`, { data: { email: 'weak@cronos.test', password: 'abc', firstName: 'a', lastName: 'b' } });
    expect(weak.status()).toBe(400);
    expect((await weak.json()).code).toBe('VALIDATION');
    const bad = await request.get(`${API}/api/products?sort=passwordHash`);
    expect(bad.status()).toBe(400);
    expect(JSON.stringify(await bad.json())).not.toContain('Product');
  });
});
