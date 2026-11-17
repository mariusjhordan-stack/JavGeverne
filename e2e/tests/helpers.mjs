import crypto from 'node:crypto';

export const API = process.env.E2E_API_URL || 'http://localhost:8080';
export const STUB = process.env.E2E_MP_STUB_URL || 'http://127.0.0.1:9099';
export const ADMIN = {
  email: process.env.E2E_ADMIN_EMAIL || 'admin@cronos.com',
  password: process.env.E2E_ADMIN_PASSWORD || 'Admin123!',
};

export const unique = (prefix) => `${prefix}-${Date.now().toString(36)}-${crypto.randomBytes(3).toString('hex')}`;
export const bearer = (token) => ({ Authorization: `Bearer ${token}` });

export async function login(request, email, password) {
  const r = await request.post(`${API}/api/auth/login`, { data: { email, password } });
  if (!r.ok()) throw new Error(`login failed for ${email}: ${r.status()}`);
  return r.json();
}

export async function registerUser(request, name = 'Test') {
  const email = `${unique('e2e')}@cronos.test`;
  const password = 'E2e-Passw0rd!';
  const r = await request.post(`${API}/api/auth/register`, { data: { email, password, firstName: name, lastName: 'Buyer' } });
  if (r.status() !== 201) throw new Error(`register failed: ${r.status()}`);
  return { email, password, ...(await r.json()) };
}

/** A product that only this test run uses, so stock assertions are exact. */
export async function createProduct(request, adminToken, { stock = 5, price = 1234.5, name } = {}) {
  const ref = unique('E2E');
  const r = await request.post(`${API}/api/admin/products`, {
    headers: bearer(adminToken),
    data: { name: name || `E2E Watch ${ref}`, referenceNumber: ref, brandId: 1, categoryId: 1, price, stock, description: 'Created by the automated test suite.' },
  });
  if (r.status() !== 201) throw new Error(`product create failed: ${r.status()} ${await r.text()}`);
  return r.json();
}

export async function retireProduct(request, adminToken, id) {
  await request.put(`${API}/api/admin/products/${id}`, { headers: bearer(adminToken), data: { active: false } });
}

export const getProduct = async (request, id) => (await request.get(`${API}/api/products/${id}`)).json();

export async function placeOrder(request, token, productId, quantity = 1, extra = {}) {
  return request.post(`${API}/api/orders`, {
    headers: bearer(token),
    data: { items: [{ productId, quantity, ...extra }], shippingAddress: 'Av. Larco 123, Lima, Peru' },
  });
}

/** Tells the Mercado Pago test double what happened to a payment; it then calls our webhook like the provider would. */
export async function providerPayment(request, data) {
  const r = await request.post(`${STUB}/__control/payments`, { data });
  return r.json();
}
