import PageHero from '../components/PageHero';
import Reveal from '../components/Reveal';
import Photo from '../components/Photo';
import { EDITORIAL } from '../lib/images';
import { useLanguage } from '../context/LanguageContext';

const TOPICS = [
  ['Movement', 'The calibre is the heart of the watch. Automatic movements wind themselves from the motion of the wrist; manual calibres ask for a daily ritual; quartz trades ceremony for accuracy.'],
  ['Case & crystal', 'Oystersteel, gold and titanium each wear differently. Sapphire crystal resists scratches almost as hard as diamond, so a dial stays clear for decades.'],
  ['Water resistance', 'Quoted in metres, but meant as a pressure rating, not a depth you should swim to. A screw-down crown and gaskets are what actually keep water out.'],
  ['Power reserve', 'How long a wound watch keeps running when it is taken off. Seventy hours means it will carry on through a long weekend without a wrist.'],
];

export default function CraftsmanshipPage() {
  const { t } = useLanguage();
  return (
    <div>
      <PageHero eyebrow="Craftsmanship" title={<>{t('What the')} <em className="text-gold">{t('numbers')}</em> {t('mean')}</>}
        intro="A short guide to reading a specification, so every detail on a product page is something you can judge for yourself." image={EDITORIAL.craft} position="center 30%" />

      <section className="max-w-[88rem] mx-auto px-6 lg:px-12 py-28 lg:py-40 grid lg:grid-cols-12 gap-16">
        <div className="lg:col-span-6 space-y-20">
          {TOPICS.map(([title, text], i) => (
            <Reveal key={title} delay={i % 2}>
              <p className="eyebrow mb-4">0{i + 1}</p>
              <h2 className="font-serif text-4xl text-ink mb-4">{t(title)}</h2>
              <p className="text-charcoal/80 leading-relaxed max-w-lg">{t(text)}</p>
            </Reveal>
          ))}
        </div>
        <Reveal variant="fade" className="lg:col-span-5 lg:col-start-8 lg:sticky lg:top-32 self-start space-y-8">
          <Photo src={EDITORIAL.craftDetail} alt="" ratio="4 / 5" />
          <p className="text-xs text-charcoal/60">{t('Photographs are credited in the project’s image register.')}</p>
        </Reveal>
      </section>
    </div>
  );
}
