// Test double for the Mercado Pago API. It lives outside the application and exists only so the
// checkout, webhook and reconciliation code paths can be exercised without real credentials.
// It is NOT evidence that the real Mercado Pago integration works: that needs the Sandbox test in docs/PAYMENTS.md.
import http from 'node:http';
import crypto from 'node:crypto';

const PORT = Number(process.env.MP_STUB_PORT || 9099);
const WEBHOOK_SECRET = process.env.MERCADOPAGO_WEBHOOK_SECRET || 'e2e-webhook-secret';
const preferences = new Map();
const payments = new Map();
// Unique per run: the services keep provider events in PostgreSQL, so ids must never repeat across stub restarts.
let nextPayment = Date.now() * 100;

const readBody = (req) => new Promise((resolve) => {
  const chunks = [];
  req.on('data', (c) => chunks.push(c));
  req.on('end', () => resolve(Buffer.concat(chunks).toString('utf8')));
});
const json = (res, status, body) => {
  res.writeHead(status, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify(body));
};

async function notify(payment, { sign = true } = {}) {
  const pref = preferences.get(payment.preference_id);
  if (!pref?.notification_url) return { skipped: 'no notification_url' };
  const ts = Math.floor(Date.now() / 1000);
  const requestId = crypto.randomUUID();
  const v1 = crypto.createHmac('sha256', WEBHOOK_SECRET).update(`id:${payment.id};request-id:${requestId};ts:${ts};`).digest('hex');
  const url = `${pref.notification_url}?data.id=${payment.id}&type=payment`;
  const headers = { 'Content-Type': 'application/json', 'x-request-id': requestId };
  if (sign) headers['x-signature'] = `ts=${ts},v1=${v1}`;
  try {
    const r = await fetch(url, { method: 'POST', headers, body: JSON.stringify({ action: 'payment.updated', type: 'payment', data: { id: String(payment.id) } }) });
    return { status: r.status };
  } catch (e) {
    return { error: String(e) };
  }
}

function pay(preferenceId, status, overrides = {}) {
  const pref = preferences.get(preferenceId);
  const amount = pref.items.reduce((sum, i) => sum + Number(i.unit_price) * Number(i.quantity), 0);
  const payment = {
    id: nextPayment++, status, status_detail: status === 'approved' ? 'accredited' : status,
    transaction_amount: overrides.amount ?? amount, currency_id: overrides.currency ?? pref.items[0].currency_id,
    external_reference: pref.external_reference, payment_method_id: 'visa', payer: { email: pref.payer?.email },
    preference_id: preferenceId, date_created: new Date().toISOString(),
  };
  payments.set(String(payment.id), payment);
  return payment;
}

const page = (pref) => `<!doctype html><html><head><meta charset="utf-8"><title>Mercado Pago (test double)</title></head>
<body style="font-family:sans-serif;max-width:520px;margin:60px auto">
<h1>Mercado Pago — test double</h1><p>Preference <code>${pref.id}</code>, order <code>${pref.external_reference}</code></p>
<p>Total: <strong id="total">${pref.items.reduce((s, i) => s + i.unit_price * i.quantity, 0)} ${pref.items[0].currency_id}</strong></p>
${['approved', 'rejected', 'pending'].map((s) => `<form method="post" action="/checkout/pay?pref=${pref.id}&status=${s}"><button id="pay-${s}" style="padding:10px 20px;margin:6px 0">Simulate ${s}</button></form>`).join('')}
</body></html>`;

http.createServer(async (req, res) => {
  const url = new URL(req.url, `http://localhost:${PORT}`);
  const path = url.pathname;
  try {
    if (req.method === 'POST' && path === '/checkout/preferences') {
      if (!(req.headers.authorization || '').startsWith('Bearer ')) return json(res, 401, { message: 'unauthorized' });
      const body = JSON.parse(await readBody(req));
      const id = `STUB-${crypto.randomUUID()}`;
      const init = `http://localhost:${PORT}/checkout?pref=${id}`;
      preferences.set(id, { ...body, id });
      return json(res, 201, { id, init_point: init, sandbox_init_point: init });
    }
    if (req.method === 'GET' && path === '/checkout') {
      const pref = preferences.get(url.searchParams.get('pref'));
      if (!pref) return json(res, 404, {});
      res.writeHead(200, { 'Content-Type': 'text/html' });
      return res.end(page(pref));
    }
    if (req.method === 'POST' && path === '/checkout/pay') {
      const pref = preferences.get(url.searchParams.get('pref'));
      const status = url.searchParams.get('status');
      const payment = pay(pref.id, status);
      await notify(payment);
      const back = pref.back_urls[status === 'approved' ? 'success' : status === 'rejected' ? 'failure' : 'pending'];
      const target = `${back}${back.includes('?') ? '&' : '?'}payment_id=${payment.id}&status=${status}&external_reference=${pref.external_reference}`;
      res.writeHead(303, { Location: target });
      return res.end();
    }
    if (req.method === 'GET' && path === '/v1/payments/search') {
      const ref = url.searchParams.get('external_reference');
      return json(res, 200, { results: [...payments.values()].filter((p) => p.external_reference === ref) });
    }
    if (req.method === 'GET' && path.startsWith('/v1/payments/')) {
      const payment = payments.get(path.split('/').pop());
      return payment ? json(res, 200, payment) : json(res, 404, { message: 'not found' });
    }
    // ---- control API used by automated tests ----
    if (req.method === 'GET' && path === '/__control/preferences') {
      const ref = url.searchParams.get('external_reference');
      return json(res, 200, [...preferences.values()].filter((p) => !ref || p.external_reference === ref));
    }
    if (req.method === 'POST' && path === '/__control/payments') {
      // {preferenceId, status, amount?, currency?, paymentId?, notify?: true|false|"unsigned"}
      const body = JSON.parse(await readBody(req));
      let payment;
      if (body.paymentId) {
        payment = payments.get(String(body.paymentId));
        payment.status = body.status;
      } else {
        payment = pay(body.preferenceId, body.status, body);
      }
      const delivery = body.notify === false ? { skipped: 'requested' } : await notify(payment, { sign: body.notify !== 'unsigned' });
      return json(res, 200, { payment, webhook: delivery });
    }
    json(res, 404, { message: 'not found' });
  } catch (e) {
    json(res, 500, { message: String(e) });
  }
}).listen(PORT, '127.0.0.1', () => console.log(`Mercado Pago test double on http://127.0.0.1:${PORT}`));
