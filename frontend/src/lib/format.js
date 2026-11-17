export const money = (value, currency = 'USD') =>
  new Intl.NumberFormat('en-US', { style: 'currency', currency, maximumFractionDigits: Number(value) % 1 === 0 ? 0 : 2 }).format(Number(value || 0));

export const shortId = (id) => `#${String(id || '').slice(0, 8).toUpperCase()}`;

export const dateTime = (value) => (value ? new Date(value).toLocaleString('en-US', { dateStyle: 'medium', timeStyle: 'short' }) : '');

/** "41mm" and "41" both read as "41 mm". */
export const withUnit = (value, unit) => {
  if (!value) return null;
  const text = String(value).trim();
  return new RegExp(`${unit}$`, 'i').test(text) ? text.replace(new RegExp(`\\s*${unit}$`, 'i'), ` ${unit}`) : `${text} ${unit}`;
};

export const ORDER_STATUS = {
  PENDING: { label: 'Awaiting payment', tone: 'amber' },
  PAYMENT_PENDING: { label: 'Payment in process', tone: 'amber' },
  PAYMENT_FAILED: { label: 'Payment not completed', tone: 'red' },
  PAID: { label: 'Paid', tone: 'green' },
  PROCESSING: { label: 'Preparing', tone: 'blue' },
  SHIPPED: { label: 'Shipped', tone: 'blue' },
  DELIVERED: { label: 'Delivered', tone: 'green' },
  CANCELLED: { label: 'Cancelled', tone: 'gray' },
  REFUNDED: { label: 'Refunded', tone: 'gray' },
};

export const statusLabel = (status) => ORDER_STATUS[status]?.label || status;
