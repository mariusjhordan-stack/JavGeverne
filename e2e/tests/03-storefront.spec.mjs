import { test, expect } from 'playwright/test';
import { ADMIN, login, createProduct, retireProduct, getProduct, unique, STUB } from './helpers.mjs';

// The whole purchase in a real browser: storefront -> gateway -> services -> PostgreSQL,
// with the Stripe test double standing in for the provider's hosted checkout.
test.describe('storefront purchase flow', () => {
  let admin; let watch; let other;
  const email = `${unique('shopper')}@cronos.test`;
  const password = 'Shopper-Passw0rd!';

  test.beforeAll(async ({ request }) => {
    admin = await login(request, ADMIN.email, ADMIN.password);
    const tag = unique('Meridian');
    watch = await createProduct(request, admin.token, { stock: 5, price: 4321, name: `${tag} Chronometer` });
    other = await createProduct(request, admin.token, { stock: 5, price: 8765, name: `${tag} Tourbillon` });
    watch.tag = tag;
  });
  test.afterAll(async ({ request }) => {
    await retireProduct(request, admin.token, watch.id);
    await retireProduct(request, admin.token, other.id);
  });

  async function signIn(page) {
    await page.goto('/login');
    await page.getByLabel('Email').fill(email);
    await page.getByLabel('Password').fill(password);
    await page.getByRole('button', { name: /sign in/i }).click();
    await expect(page).toHaveURL(/\/$/);
  }

  async function checkoutFromCart(page) {
    await page.goto('/checkout');
    await page.getByLabel(/full name/i).fill('Sam Shopper');
    await page.getByLabel(/street address/i).fill('Av. Larco 123');
    await page.getByLabel(/^city/i).fill('Lima');
    await page.getByLabel(/country/i).fill('Peru');
    await page.getByRole('button', { name: /place order/i }).click();
    await page.waitForURL(new RegExp(`${STUB.replace(/[.:/]/g, '\\$&').replace('127\\.0\\.0\\.1', '(127\\.0\\.0\\.1|localhost)')}/checkout`));
  }

  test('1-2. register, then sign out and sign in again', async ({ page }) => {
    await page.goto('/register');
    await page.getByLabel('First Name').fill('Sam');
    await page.getByLabel('Last Name').fill('Shopper');
    await page.getByLabel('Email').fill(email);
    await page.getByLabel('Password', { exact: true }).fill('short');
    await page.getByLabel('Confirm Password').fill('short');
    await page.getByRole('button', { name: /create account/i }).click();
    await expect(page.getByRole('alert')).toContainText(/at least 8/i);
    await page.getByLabel('Password', { exact: true }).fill(password);
    await page.getByLabel('Confirm Password').fill(password);
    await page.getByRole('button', { name: /create account/i }).click();
    await expect(page).toHaveURL(/\/$/);

    await page.getByRole('button', { name: /account menu/i }).click();
    await page.getByRole('button', { name: /logout|sign out/i }).click();
    await signIn(page);
  });

  test('3. catalogue search and sorting really filter and order', async ({ page }) => {
    await page.goto('/collection');
    await expect(page.getByTestId('product-card').first()).toBeVisible();
    await page.getByLabel('Search watches').fill(watch.tag);
    await page.getByLabel('Search watches').press('Enter');
    await expect(page.getByTestId('product-card')).toHaveCount(2);
    await page.getByLabel('Sort products').selectOption('price_desc');
    await expect(page.getByTestId('product-card').first()).toContainText('Tourbillon');
    await page.getByLabel('Sort products').selectOption('price_asc');
    await expect(page.getByTestId('product-card').first()).toContainText('Chronometer');
    await page.getByLabel('Search watches').fill('no-such-watch-xyz');
    await page.getByLabel('Search watches').press('Enter');
    await expect(page.getByTestId('product-card')).toHaveCount(0);
  });

  test('4. product detail and comparison are connected', async ({ page }) => {
    await page.goto(`/product/${watch.id}`);
    await expect(page.getByRole('heading', { level: 1 })).toContainText('Chronometer');
    await page.getByRole('button', { name: /compare/i }).click();
    await page.goto(`/product/${other.id}`);
    await page.getByRole('button', { name: /compare/i }).click();
    await page.goto('/compare');
    await expect(page.getByTestId('compare-table')).toContainText('$4,321');
    await expect(page.getByTestId('compare-table')).toContainText('$8,765');
  });

  test('5-10. cart, checkout, order, payment approved, real status, order history', async ({ page, request }) => {
    await signIn(page);
    await page.goto(`/product/${watch.id}`);
    await page.getByRole('button', { name: /add to (cart|bag)/i }).click();
    await page.goto('/cart');
    await expect(page.getByRole('main')).toContainText('Chronometer');
    await checkoutFromCart(page);

    await expect(page.locator('#total')).toContainText('4321');
    await page.locator('#pay-approved').click();

    const result = page.getByTestId('checkout-result');
    await expect(result).toHaveAttribute('data-order-status', 'PAID', { timeout: 30_000 });
    await expect(result).toContainText(/payment confirmed/i);
    expect((await getProduct(request, watch.id)).stock).toBe(4);

    await page.getByRole('link', { name: /view order/i }).click();
    await expect(page.getByTestId('order-status')).toContainText(/paid/i);
    await expect(page.getByTestId('order-total')).toContainText('$4,321');
    await expect(page.getByTestId('payment-status')).toContainText(/approved/i);
    await expect(page.getByTestId('order-timeline')).toBeVisible();

    await page.goto('/orders');
    await expect(page.getByTestId('order-row').first()).toContainText(/paid/i);
  });

  test('a rejected payment is shown as not completed and can be retried', async ({ page, request }) => {
    await signIn(page);
    await page.goto(`/product/${watch.id}`);
    await page.getByRole('button', { name: /add to (cart|bag)/i }).click();
    await checkoutFromCart(page);
    await page.locator('#pay-rejected').click();

    const result = page.getByTestId('checkout-result');
    await expect(result).toHaveAttribute('data-order-status', 'PAYMENT_FAILED', { timeout: 30_000 });
    await expect(result).toContainText(/not completed/i);
    await expect(result).not.toContainText(/payment confirmed/i);
    expect((await getProduct(request, watch.id)).stock).toBe(3);

    await page.getByRole('link', { name: /view order/i }).click();
    await page.getByRole('button', { name: /try payment again/i }).click();
    await page.locator('#pay-approved').click();
    await expect(page.getByTestId('checkout-result')).toHaveAttribute('data-order-status', 'PAID', { timeout: 30_000 });
  });

  test('a return URL alone never shows a confirmed payment', async ({ page }) => {
    await signIn(page);
    await page.goto(`/product/${watch.id}`);
    await page.getByRole('button', { name: /add to (cart|bag)/i }).click();
    await checkoutFromCart(page);
    const sessionId = new URL((await page.locator('form').first().getAttribute('action')), STUB).searchParams.get('session');
    expect(sessionId).toBeTruthy();
    // Leave the provider without paying and open the "success" page by hand, as an attacker would.
    const sessions = await (await page.request.get(`${STUB}/__control/sessions`)).json();
    const mine = sessions.find((session) => session.id === sessionId);
    await page.goto(`/checkout/success?order=${mine.client_reference_id}&status=approved&payment_id=1`);
    const result = page.getByTestId('checkout-result');
    await expect(result).toHaveAttribute('data-order-status', 'PENDING', { timeout: 40_000 });
    await expect(result).not.toContainText(/payment confirmed/i);
    await page.goto('/checkout/success?order=00000000-0000-4000-8000-000000000000');
    await expect(page.getByRole('main')).toContainText(/could not find that order/i);
  });

  test('unknown routes show a 404 page and private pages require sign-in', async ({ page }) => {
    await page.goto('/this-route-does-not-exist');
    await expect(page.getByRole('heading', { level: 1 })).toContainText(/does not exist/i);
    await page.goto('/orders');
    await expect(page).toHaveURL(/\/login/);
    await page.goto('/admin');
    await expect(page).toHaveURL(/\/login/);
  });

  test('an expired session is refreshed silently; a dead session returns to sign-in', async ({ page }) => {
    await signIn(page);
    await page.evaluate(() => localStorage.setItem('token', 'expired.invalid.token'));
    await page.goto('/orders');
    await expect(page.getByTestId('order-row').first()).toBeVisible();
    expect(await page.evaluate(() => localStorage.getItem('token'))).not.toBe('expired.invalid.token');

    await page.evaluate(() => { localStorage.setItem('token', 'expired.invalid.token'); localStorage.setItem('refreshToken', 'dead'); });
    await page.goto('/orders');
    await expect(page).toHaveURL(/\/login/);
    await expect(page.getByRole('status')).toContainText(/session expired/i);
  });

  test('editing the stored role does not give admin powers', async ({ page }) => {
    await signIn(page);
    await page.evaluate(() => { const u = JSON.parse(localStorage.getItem('user')); u.role = 'ADMIN'; localStorage.setItem('user', JSON.stringify(u)); });
    const response = page.waitForResponse((r) => r.url().includes('/api/admin/'));
    await page.goto('/admin');
    expect((await response).status()).toBe(403);
  });
});
