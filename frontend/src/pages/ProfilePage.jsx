import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { toast } from 'sonner';
import { useAuth } from '../context/AuthContext';
import { getProfile, updateProfile, changePassword, listAddresses, createAddress, updateAddress, deleteAddress, getNotifications } from '../api/account';
import { listFavorites, removeFavorite } from '../api/favorites';
import { errorMessage } from '../api/client';
import { money, dateTime } from '../lib/format';
import Photo from '../components/Photo';

const input = 'w-full border-b border-ink/25 bg-transparent py-2.5 text-sm focus:border-gold-dark focus:outline-none';
const Field = ({ id, label, ...props }) => (
  <div><label htmlFor={id} className="eyebrow block mb-1">{label}</label><input id={id} className={input} {...props} /></div>
);
const BLANK = { label: '', recipient: '', line1: '', line2: '', city: '', region: '', postalCode: '', country: '', phone: '' };

export default function ProfilePage() {
  const { updateUser, isAdmin } = useAuth();
  const [profile, setProfile] = useState(null);
  const [addresses, setAddresses] = useState([]);
  const [favorites, setFavorites] = useState([]);
  const [notices, setNotices] = useState([]);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(BLANK);
  const [pw, setPw] = useState({ currentPassword: '', newPassword: '' });

  const reloadAddresses = () => listAddresses().then(setAddresses).catch(() => {});
  useEffect(() => {
    getProfile().then(setProfile).catch(() => {});
    reloadAddresses();
    listFavorites().then(setFavorites).catch(() => {});
    getNotifications().then(setNotices).catch(() => {});
  }, []);

  const saveProfile = async (e) => {
    e.preventDefault();
    try {
      const saved = await updateProfile({ firstName: profile.firstName, lastName: profile.lastName, phone: profile.phone });
      setProfile(saved); updateUser({ firstName: saved.firstName, lastName: saved.lastName }); toast.success('Profile updated');
    } catch (err) { toast.error(errorMessage(err)); }
  };
  const savePassword = async (e) => {
    e.preventDefault();
    try { await changePassword(pw.currentPassword, pw.newPassword); setPw({ currentPassword: '', newPassword: '' }); toast.success('Password changed. Other devices were signed out.'); }
    catch (err) { toast.error(errorMessage(err)); }
  };
  const saveAddress = async (e) => {
    e.preventDefault();
    try {
      if (editing === 'new') await createAddress(form); else await updateAddress(editing, form);
      setEditing(null); reloadAddresses(); toast.success('Address saved');
    } catch (err) { toast.error(errorMessage(err)); }
  };

  if (!profile) return <p className="py-32 text-center text-sm text-charcoal/60" role="status">Loading…</p>;

  return (
    <div className="max-w-5xl mx-auto px-6 lg:px-12 py-10 pb-28">
      <p className="eyebrow mb-4">Your account</p>
      <h1 className="display text-5xl lg:text-6xl text-ink mb-4">{profile.firstName} {profile.lastName}</h1>
      <p className="text-sm text-charcoal/70 mb-14">{profile.email} · <Link to="/orders" className="underline hover:text-gold-dark">Order history</Link>{isAdmin && <> · <Link to="/admin" className="underline hover:text-gold-dark">Administration</Link></>}</p>

      <div className="grid lg:grid-cols-2 gap-x-20 gap-y-16">
        <form onSubmit={saveProfile} className="space-y-5" aria-label="Personal details">
          <h2 className="font-serif text-3xl text-ink">Personal details</h2>
          <Field id="pf-first" label="First name" required value={profile.firstName} onChange={(e) => setProfile({ ...profile, firstName: e.target.value })} />
          <Field id="pf-last" label="Last name" required value={profile.lastName} onChange={(e) => setProfile({ ...profile, lastName: e.target.value })} />
          <Field id="pf-phone" label="Phone" value={profile.phone || ''} onChange={(e) => setProfile({ ...profile, phone: e.target.value })} />
          <button className="btn-solid">Save</button>
        </form>

        <form onSubmit={savePassword} className="space-y-5" aria-label="Password">
          <h2 className="font-serif text-3xl text-ink">Password</h2>
          <Field id="pw-cur" type="password" autoComplete="current-password" label="Current password" required value={pw.currentPassword} onChange={(e) => setPw({ ...pw, currentPassword: e.target.value })} />
          <Field id="pw-new" type="password" autoComplete="new-password" label="New password (8+ characters)" required minLength={8} value={pw.newPassword} onChange={(e) => setPw({ ...pw, newPassword: e.target.value })} />
          <button className="btn-solid">Change password</button>
        </form>

        <section>
          <div className="flex items-baseline justify-between mb-5">
            <h2 className="font-serif text-3xl text-ink">Addresses</h2>
            <button onClick={() => { setEditing('new'); setForm(BLANK); }} className="text-xs tracking-[0.18em] uppercase underline hover:text-gold-dark">Add</button>
          </div>
          {editing && (
            <form onSubmit={saveAddress} className="space-y-4 border hairline p-5 mb-6" aria-label="Address form">
              <Field id="ad-rec" label="Recipient" required value={form.recipient} onChange={(e) => setForm({ ...form, recipient: e.target.value })} />
              <Field id="ad-l1" label="Address" required value={form.line1} onChange={(e) => setForm({ ...form, line1: e.target.value })} />
              <div className="grid grid-cols-2 gap-4">
                <Field id="ad-city" label="City" required value={form.city} onChange={(e) => setForm({ ...form, city: e.target.value })} />
                <Field id="ad-post" label="Postal code" value={form.postalCode || ''} onChange={(e) => setForm({ ...form, postalCode: e.target.value })} />
              </div>
              <Field id="ad-country" label="Country" required value={form.country} onChange={(e) => setForm({ ...form, country: e.target.value })} />
              <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={!!form.defaultAddress} onChange={(e) => setForm({ ...form, defaultAddress: e.target.checked })} className="accent-gold-dark" /> Use by default</label>
              <div className="flex gap-4"><button className="btn-solid !py-3">Save address</button><button type="button" className="text-sm underline" onClick={() => setEditing(null)}>Cancel</button></div>
            </form>
          )}
          <ul className="divide-y hairline border-y hairline">
            {addresses.length === 0 && <li className="py-6 text-sm text-charcoal/60">No saved addresses.</li>}
            {addresses.map((a) => (
              <li key={a.id} className="py-4 text-sm flex justify-between gap-4">
                <div><p className="text-ink">{a.recipient}{a.defaultAddress && <span className="ml-2 text-xs text-gold-dark">default</span>}</p><p className="text-charcoal/70">{a.line1}, {a.city}, {a.country}</p></div>
                <div className="space-x-3 whitespace-nowrap"><button className="underline" onClick={() => { setEditing(a.id); setForm({ ...BLANK, ...a }); }}>Edit</button><button className="underline text-red-700" onClick={() => deleteAddress(a.id).then(reloadAddresses)}>Delete</button></div>
              </li>
            ))}
          </ul>
        </section>

        <section>
          <h2 className="font-serif text-3xl text-ink mb-5">Saved watches</h2>
          {favorites.length === 0 ? <p className="text-sm text-charcoal/60">Nothing saved yet. Use “Save” on any watch.</p> : (
            <ul className="space-y-4">
              {favorites.map((f) => (
                <li key={f.id} className="flex items-center gap-4">
                  <Photo src={f.imageUrl} alt="" ratio="1 / 1" className="w-16 shrink-0" />
                  <div className="flex-1"><Link to={`/product/${f.id}`} className="font-serif text-lg hover:text-gold-dark">{f.name}</Link><p className="text-sm text-charcoal/70">{money(f.price, f.currency)}</p></div>
                  <button className="text-xs underline" onClick={() => removeFavorite(f.id).then(() => setFavorites(favorites.filter((x) => x.id !== f.id)))}>Remove</button>
                </li>
              ))}
            </ul>
          )}
        </section>

        <section className="lg:col-span-2">
          <h2 className="font-serif text-3xl text-ink mb-5">Messages about your orders</h2>
          <ul className="divide-y hairline border-y hairline text-sm">
            {notices.length === 0 && <li className="py-6 text-charcoal/60">No messages yet.</li>}
            {notices.slice(0, 8).map((n) => (
              <li key={n.id} className="py-3 flex justify-between gap-6"><span><span className="text-ink">{n.subject}</span><span className="block text-charcoal/60">{n.status === 'SENT' ? 'Sent by e-mail' : n.status === 'FAILED' ? 'E-mail could not be sent' : 'Recorded in your account'}</span></span><span className="text-charcoal/60 whitespace-nowrap">{dateTime(n.createdAt)}</span></li>
            ))}
          </ul>
        </section>
      </div>
    </div>
  );
}
