// Test double for Stripe Checkout; it exercises our integration without real credentials.
import http from 'node:http';
import crypto from 'node:crypto';

const PORT = Number(process.env.STRIPE_STUB_PORT || 9099);
const WEBHOOK_SECRET = process.env.STRIPE_WEBHOOK_SECRET || 'whsec_e2e-webhook-secret';
const sessions = new Map();
let nextSession = Date.now();

const readBody = (req) => new Promise((resolve) => {
  const chunks = [];
  req.on('data', (c) => chunks.push(c));
  req.on('end', () => resolve(Buffer.concat(chunks).toString('utf8')));
});
const json = (res, status, body) => {
  res.writeHead(status, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify(body));
};
const exponent = (currency) => ['jpy', 'krw'].includes(currency.toLowerCase()) ? 0 : 2;
const amountFor = (form) => {
  let total = 0;
  for (let i = 0; form.has(`line_items[${i}][quantity]`); i += 1) {
    const currency = form.get(`line_items[${i}][price_data][currency]`);
    total += Number(form.get(`line_items[${i}][price_data][unit_amount]`))
      * Number(form.get(`line_items[${i}][quantity]`));
  }
  return total;
};

async function notify(eventType, object, { sign = true } = {}) {
  const session = sessions.get(object.id) || [...sessions.values()].find((item) => item.payment_intent === object.payment_intent);
  if (!session) return { skipped: 'unknown checkout session' };
  const payload = JSON.stringify({ id: `evt_${crypto.randomUUID()}`, type: eventType, data: { object } });
  const timestamp = Math.floor(Date.now() / 1000);
  const signature = crypto.createHmac('sha256', WEBHOOK_SECRET).update(`${timestamp}.${payload}`).digest('hex');
  const webhookUrl = new URL(session.success_url).origin + '/api/payments/webhook';
  const headers = { 'Content-Type': 'application/json' };
  if (sign) headers['stripe-signature'] = `t=${timestamp},v1=${signature}`;
  try {
    const response = await fetch(webhookUrl, {
      method: 'POST',
      headers,
      body: payload,
    });
    return { status: response.status };
  } catch (error) {
    return { error: String(error) };
  }
}

function setStatus(session, status, overrides = {}) {
  session.status = status === 'rejected' || status === 'cancelled' ? 'expired' : 'complete';
  session.payment_status = status === 'approved' ? 'paid' : 'unpaid';
  if (overrides.amount !== undefined) session.amount_total = Math.round(Number(overrides.amount) * (10 ** exponent(session.currency)));
  if (overrides.currency) session.currency = overrides.currency.toLowerCase();
  return session;
}

const page = (session) => `<!doctype html><html><head><meta charset="utf-8"><title>Stripe Checkout (test double)</title></head>
<body style="font-family:sans-serif;max-width:520px;margin:60px auto">
<h1>Stripe Checkout — test double</h1><p>Session <code>${session.id}</code>, order <code>${session.client_reference_id}</code></p>
<p>Total: <strong id="total">${(session.amount_total / (10 ** exponent(session.currency))).toFixed(exponent(session.currency))} ${session.currency.toUpperCase()}</strong></p>
${['approved', 'rejected', 'pending'].map((status) => `<form method="post" action="/checkout/pay?session=${session.id}&status=${status}"><button id="pay-${status}" style="padding:10px 20px;margin:6px 0">Simulate ${status}</button></form>`).join('')}
</body></html>`;

http.createServer(async (req, res) => {
  const url = new URL(req.url, `http://localhost:${PORT}`);
  const path = url.pathname;
  try {
    if (req.method === 'POST' && path === '/v1/checkout/sessions') {
      if (!(req.headers.authorization || '').startsWith('Bearer ')) return json(res, 401, { error: 'unauthorized' });
      const form = new URLSearchParams(await readBody(req));
      const id = `cs_test_${nextSession++}`;
      const session = {
        id,
        url: `http://localhost:${PORT}/checkout?session=${id}`,
        status: 'open',
        payment_status: 'unpaid',
        amount_total: amountFor(form),
        currency: form.get('line_items[0][price_data][currency]'),
        client_reference_id: form.get('client_reference_id'),
        customer_email: form.get('customer_email'),
        customer_details: { email: form.get('customer_email') },
        payment_method_types: ['card'],
        payment_intent: `pi_${id}`,
        success_url: form.get('success_url'),
        cancel_url: form.get('cancel_url'),
        metadata: { orderId: form.get('metadata[orderId]') },
      };
      sessions.set(id, session);
      return json(res, 200, session);
    }
    if (req.method === 'GET' && path === '/v1/checkout/sessions') {
      const orderId = url.searchParams.get('client_reference_id');
      const paymentIntent = url.searchParams.get('payment_intent');
      return json(res, 200, { data: [...sessions.values()].filter((session) =>
        (!orderId || session.client_reference_id === orderId)
        && (!paymentIntent || session.payment_intent === paymentIntent)) });
    }
    if (req.method === 'GET' && path.startsWith('/v1/checkout/sessions/')) {
      const session = sessions.get(path.split('/').pop());
      return session ? json(res, 200, session) : json(res, 404, { error: 'not found' });
    }
    if (req.method === 'GET' && path === '/checkout') {
      const session = sessions.get(url.searchParams.get('session'));
      if (!session) return json(res, 404, {});
      res.writeHead(200, { 'Content-Type': 'text/html' });
      return res.end(page(session));
    }
    if (req.method === 'POST' && path === '/checkout/pay') {
      const session = sessions.get(url.searchParams.get('session'));
      if (!session) return json(res, 404, {});
      const status = url.searchParams.get('status');
      setStatus(session, status);
      const eventType = status === 'approved' ? 'checkout.session.completed'
        : status === 'rejected' ? 'checkout.session.async_payment_failed' : 'checkout.session.completed';
      await notify(eventType, session);
      const target = status === 'approved' ? session.success_url : session.cancel_url;
      res.writeHead(303, { Location: target });
      return res.end();
    }
    if (req.method === 'GET' && path === '/__control/sessions') {
      const orderId = url.searchParams.get('client_reference_id');
      return json(res, 200, [...sessions.values()].filter((session) => !orderId || session.client_reference_id === orderId));
    }
    if (req.method === 'POST' && path === '/__control/payments') {
      const body = JSON.parse(await readBody(req));
      let session = body.sessionId ? sessions.get(String(body.sessionId)) : sessions.get(String(body.paymentId));
      if (!session && body.preferenceId) session = sessions.get(String(body.preferenceId));
      if (!session) session = sessions.get(String(body.session));
      if (!session) return json(res, 404, { error: 'unknown session' });
      const status = body.status || 'approved';
      setStatus(session, status, body);
      let eventType = status === 'approved' ? 'checkout.session.completed'
        : status === 'rejected' ? 'checkout.session.async_payment_failed'
          : status === 'refunded' ? 'charge.refunded'
            : status === 'cancelled' ? 'checkout.session.expired' : 'checkout.session.completed';
      const object = status === 'refunded'
        ? { id: `ch_${session.id}`, payment_intent: session.payment_intent }
        : session;
      const delivery = body.notify === false ? { skipped: 'requested' }
        : await notify(eventType, object, { sign: body.notify !== 'unsigned' });
      return json(res, 200, { payment: session, webhook: delivery });
    }
    json(res, 404, { error: 'not found' });
  } catch (error) {
    json(res, 500, { error: String(error) });
  }
}).listen(PORT, '127.0.0.1', () => console.log(`Stripe test double on http://127.0.0.1:${PORT}`));
