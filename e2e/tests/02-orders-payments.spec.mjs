import { test, expect } from 'playwright/test';
import { API, STUB, ADMIN, bearer, login, registerUser, createProduct, retireProduct, getProduct, placeOrder, providerPayment } from './helpers.mjs';

// Contracts between order, catalog, payment and notification, exercised over real HTTP.
// Mercado Pago is replaced by the test double in e2e/mp-stub: this proves OUR handling, not the provider.
test.describe('orders, stock, payments and notifications', () => {
  let admin; let buyer; let product;
  const order = async (request, id) => (await request.get(`${API}/api/orders/${id}`, { headers: bearer(buyer.token) })).json();
  const checkout = async (request, id) => (await request.post(`${API}/api/payments/preference`, { headers: bearer(buyer.token), data: { orderId: id } })).json();
  const stock = async (request) => (await getProduct(request, product.id)).stock;
  const newOrder = async (request, quantity = 1) => (await placeOrder(request, buyer.token, product.id, quantity)).json();

  test.beforeAll(async ({ request }) => {
    admin = await login(request, ADMIN.email, ADMIN.password);
    buyer = await registerUser(request, 'Buyer');
    product = await createProduct(request, admin.token, { stock: 10, price: 1500 });
  });
  test.afterAll(async ({ request }) => retireProduct(request, admin.token, product.id));

  test('the server prices the order; tampered prices and bad quantities are useless', async ({ request }) => {
    const before = await stock(request);
    const r = await placeOrder(request, buyer.token, product.id, 2, { unitPrice: 0.01, productName: 'Free' });
    expect(r.status()).toBe(201);
    const o = await r.json();
    expect(o.total).toBe(3000);
    expect(o.items[0].unitPrice).toBe(1500);
    expect(await stock(request)).toBe(before - 2);

    for (const quantity of [0, -5]) expect((await placeOrder(request, buyer.token, product.id, quantity)).status()).toBe(400);
    const tooMany = await placeOrder(request, buyer.token, product.id, 99);
    expect(tooMany.status()).toBe(409);
    expect((await tooMany.json()).code).toBe('INSUFFICIENT_STOCK');
    expect(await stock(request)).toBe(before - 2);

    expect((await request.post(`${API}/api/orders/${o.id}/cancel`, { headers: bearer(buyer.token) })).status()).toBe(200);
    expect(await stock(request)).toBe(before);
    expect((await request.post(`${API}/api/orders/${o.id}/cancel`, { headers: bearer(buyer.token) })).status()).toBe(409);
    expect(await stock(request)).toBe(before);
  });

  test('simultaneous orders never oversell', async ({ request }) => {
    const scarce = await createProduct(request, admin.token, { stock: 3, price: 900 });
    const results = await Promise.all(Array.from({ length: 10 }, () => placeOrder(request, buyer.token, scarce.id)));
    const statuses = results.map((r) => r.status());
    expect(statuses.filter((s) => s === 201).length).toBe(3);
    expect(statuses.filter((s) => s === 409).length).toBe(7);
    expect((await getProduct(request, scarce.id)).stock).toBe(0);
    await retireProduct(request, admin.token, scarce.id);
  });

  test('checkout uses the order total and configured URLs, not client input', async ({ request }) => {
    const o = await newOrder(request);
    const r = await request.post(`${API}/api/payments/preference`, { headers: bearer(buyer.token), data: { orderId: o.id, totalAmount: 1, currency: 'XXX' } });
    expect(r.status()).toBe(200);
    const [pref] = await (await request.get(`${STUB}/__control/preferences?external_reference=${o.id}`)).json();
    expect(pref.items[0].unit_price).toBe(1500);
    expect(pref.external_reference).toBe(o.id);
    expect(pref.back_urls.success).toContain(`/checkout/success?order=${o.id}`);
    expect(pref.notification_url).toContain('/api/payments/webhook');
  });

  test('approved payment: verified, order paid, stock confirmed, applied exactly once', async ({ request }) => {
    const before = await stock(request);
    const o = await newOrder(request);
    const c = await checkout(request, o.id);
    const { payment, webhook } = await providerPayment(request, { preferenceId: c.preferenceId, status: 'approved' });
    expect(webhook.status).toBe(200);
    expect((await order(request, o.id)).status).toBe('PAID');
    await providerPayment(request, { paymentId: payment.id, status: 'approved' });
    await providerPayment(request, { paymentId: payment.id, status: 'approved' });
    const view = await (await request.get(`${API}/api/payments/order/${o.id}`, { headers: bearer(buyer.token) })).json();
    expect(view.status).toBe('APPROVED');
    expect(view.events.length).toBe(1);
    expect(await stock(request)).toBe(before - 1);
    const mine = await (await request.get(`${API}/api/notifications`, { headers: bearer(buyer.token) })).json();
    expect(mine.filter((n) => n.orderId === o.id).map((n) => n.type).sort()).toEqual(['ORDER_CREATED', 'PAYMENT_APPROVED']);
  });

  test('pending then approved on the same payment ends as paid', async ({ request }) => {
    const o = await newOrder(request);
    const c = await checkout(request, o.id);
    const { payment } = await providerPayment(request, { preferenceId: c.preferenceId, status: 'pending' });
    expect((await order(request, o.id)).status).toBe('PAYMENT_PENDING');
    await providerPayment(request, { paymentId: payment.id, status: 'approved' });
    expect((await order(request, o.id)).status).toBe('PAID');
  });

  test('pending then rejected keeps the reservation for a retry, which can then be approved', async ({ request }) => {
    const before = await stock(request);
    const o = await newOrder(request);
    const c = await checkout(request, o.id);
    const { payment } = await providerPayment(request, { preferenceId: c.preferenceId, status: 'pending' });
    await providerPayment(request, { paymentId: payment.id, status: 'rejected' });
    expect((await order(request, o.id)).status).toBe('PAYMENT_FAILED');
    expect(await stock(request)).toBe(before - 1);
    const retry = await checkout(request, o.id);
    await providerPayment(request, { preferenceId: retry.preferenceId, status: 'approved' });
    expect((await order(request, o.id)).status).toBe('PAID');
    // A late notice about the failed attempt must not undo it.
    await providerPayment(request, { paymentId: payment.id, status: 'cancelled' });
    expect((await order(request, o.id)).status).toBe('PAID');
  });

  test('refund returns the stock', async ({ request }) => {
    const before = await stock(request);
    const o = await newOrder(request);
    const c = await checkout(request, o.id);
    const { payment } = await providerPayment(request, { preferenceId: c.preferenceId, status: 'approved' });
    expect(await stock(request)).toBe(before - 1);
    await providerPayment(request, { paymentId: payment.id, status: 'refunded' });
    expect((await order(request, o.id)).status).toBe('REFUNDED');
    expect(await stock(request)).toBe(before);
  });

  test('unsigned webhooks and wrong amounts never mark an order paid', async ({ request }) => {
    const o = await newOrder(request);
    const c = await checkout(request, o.id);
    const unsigned = await providerPayment(request, { preferenceId: c.preferenceId, status: 'approved', notify: 'unsigned' });
    expect(unsigned.webhook.status).toBe(401);
    expect((await order(request, o.id)).status).toBe('PENDING');

    const forged = await request.post(`${API}/api/payments/webhook?data.id=${unsigned.payment.id}&type=payment`, {
      headers: { 'x-signature': 'ts=1,v1=00', 'x-request-id': 'r' }, data: { type: 'payment', data: { id: String(unsigned.payment.id) } },
    });
    expect(forged.status()).toBe(401);

    const o2 = await newOrder(request);
    const c2 = await checkout(request, o2.id);
    const cheap = await providerPayment(request, { preferenceId: c2.preferenceId, status: 'approved', amount: 1 });
    expect(cheap.webhook.status).toBe(200);
    expect((await order(request, o2.id)).status).toBe('PENDING');

    // A webhook that never arrived is recovered by asking the provider (what the return page does).
    const synced = await request.post(`${API}/api/payments/order/${o.id}/sync`, { headers: bearer(buyer.token) });
    expect((await synced.json()).status).toBe('APPROVED');
    expect((await order(request, o.id)).status).toBe('PAID');
    await request.post(`${API}/api/orders/${o2.id}/cancel`, { headers: bearer(buyer.token) });
  });

  test('administrator fulfils a paid order through the allowed steps only', async ({ request }) => {
    const o = await newOrder(request);
    const c = await checkout(request, o.id);
    await providerPayment(request, { preferenceId: c.preferenceId, status: 'approved' });
    const set = (status) => request.put(`${API}/api/admin/orders/${o.id}/status`, { headers: bearer(admin.token), data: { status } });
    expect((await set('DELIVERED')).status()).toBe(409);
    for (const s of ['PROCESSING', 'SHIPPED', 'DELIVERED']) expect((await set(s)).status()).toBe(200);
    const final = await order(request, o.id);
    expect(final.history.map((h) => h.to)).toEqual(['PENDING', 'PAID', 'PROCESSING', 'SHIPPED', 'DELIVERED']);
  });
});
