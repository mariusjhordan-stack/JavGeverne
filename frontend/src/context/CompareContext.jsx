import { createContext, useContext, useState, useEffect, useCallback } from 'react';

const CompareContext = createContext(null);
export const MAX_COMPARE = 3;

const read = () => {
  try {
    const stored = JSON.parse(localStorage.getItem('compare') || '[]');
    return Array.isArray(stored) ? stored.filter((x) => typeof x === 'string').slice(0, MAX_COMPARE) : [];
  } catch {
    return [];
  }
};

/** Holds product ids only; details are always loaded fresh from the catalog. */
export function CompareProvider({ children }) {
  const [ids, setIds] = useState(read);

  useEffect(() => { localStorage.setItem('compare', JSON.stringify(ids)); }, [ids]);

  const add = useCallback((id) => {
    let result = 'added';
    setIds((prev) => {
      if (prev.includes(id)) { result = 'exists'; return prev; }
      if (prev.length >= MAX_COMPARE) { result = 'full'; return prev; }
      return [...prev, id];
    });
    return result;
  }, []);
  const remove = useCallback((id) => setIds((prev) => prev.filter((x) => x !== id)), []);
  const clear = useCallback(() => setIds([]), []);

  return <CompareContext.Provider value={{ ids, add, remove, clear }}>{children}</CompareContext.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useCompare() {
  const ctx = useContext(CompareContext);
  if (!ctx) throw new Error('useCompare must be used within CompareProvider');
  return ctx;
}
