import Photo from './Photo';
import Reveal from './Reveal';
import { useLanguage } from '../context/LanguageContext';

/** Full-bleed opening for editorial pages. The navbar is transparent over it. */
export default function PageHero({ eyebrow, title, intro, image, position = 'center' }) {
  const { t } = useLanguage();
  return (
    <section className="relative bg-ink text-white min-h-[70svh] flex items-end overflow-hidden">
      <Photo src={image} alt="" eager className="absolute inset-0 !bg-ink" imgClassName="opacity-60" position={position} />
      <div className="absolute inset-0 bg-gradient-to-t from-ink via-ink/50 to-ink/20" aria-hidden="true" />
      <Reveal variant="fade" className="relative z-10 max-w-[88rem] w-full mx-auto px-6 lg:px-12 pb-16 lg:pb-24 pt-40">
        <p className="eyebrow eyebrow-light mb-6">{t(eyebrow)}</p>
        <h1 className="display text-5xl sm:text-6xl lg:text-8xl max-w-4xl">{title}</h1>
        {intro && <p className="text-white/75 leading-relaxed max-w-xl mt-8">{t(intro)}</p>}
      </Reveal>
    </section>
  );
}
