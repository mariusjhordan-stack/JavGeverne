import { useEffect, useRef } from 'react';

const reduced = () => typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

/** Fades content in as it scrolls into view. Without JavaScript or with reduced motion it is simply shown. */
export default function Reveal({ as: Tag = 'div', variant = '', delay = 0, className = '', children, ...rest }) {
  const ref = useRef(null);

  useEffect(() => {
    const el = ref.current;
    if (!el) return undefined;
    document.documentElement.classList.add('js-motion');
    if (reduced() || !('IntersectionObserver' in window)) {
      el.classList.add('is-visible');
      return undefined;
    }
    const observer = new IntersectionObserver(([entry]) => {
      if (entry.isIntersecting) { el.classList.add('is-visible'); observer.disconnect(); }
    }, { rootMargin: '0px 0px -8% 0px', threshold: 0.08 });
    observer.observe(el);
    return () => observer.disconnect();
  }, []);

  const variantClass = variant === 'fade' ? 'reveal-fade' : variant === 'left' ? 'reveal-left' : '';
  return <Tag ref={ref} className={`reveal ${variantClass} ${className}`} data-delay={delay || undefined} {...rest}>{children}</Tag>;
}
