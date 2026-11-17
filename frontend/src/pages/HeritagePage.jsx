import PageHero from '../components/PageHero';
import Reveal from '../components/Reveal';
import Photo from '../components/Photo';
import { EDITORIAL } from '../lib/images';

const CHAPTERS = [
  ['1987', 'A small room in Geneva', 'Cronos Genève began as a workbench and a conviction: that a watch should be chosen slowly, and explained honestly.'],
  ['1999', 'The catalogue as a craft', 'Every reference was documented by hand: movement, case, dial, strap. Specifications are never an afterthought here.'],
  ['2012', 'Beyond the counter', 'We opened the house to collectors everywhere, keeping the same slow, personal manner of advice.'],
  ['Today', 'Still unhurried', 'A tighter selection, better explained. We would rather show fewer pieces than rush a decision.'],
];

export default function HeritagePage() {
  return (
    <div>
      <PageHero eyebrow="Heritage" title={<>A house built on <em className="text-gold">restraint</em></>}
        intro="The story of Cronos Genève, told as it is lived: in small decisions, repeated for decades." image={EDITORIAL.heritage} position="center 40%" />

      <section className="max-w-[88rem] mx-auto px-6 lg:px-12 py-28 lg:py-40">
        <ol className="space-y-24 lg:space-y-36">
          {CHAPTERS.map(([year, title, text], i) => (
            <Reveal as="li" key={year} className={`grid lg:grid-cols-12 gap-8 items-baseline ${i % 2 ? 'lg:text-right' : ''}`}>
              <p className={`display text-7xl lg:text-9xl text-gold-dark/80 lg:col-span-4 ${i % 2 ? 'lg:col-start-9 lg:order-2' : ''}`}>{year}</p>
              <div className={`lg:col-span-5 ${i % 2 ? 'lg:col-start-4 lg:order-1' : 'lg:col-start-6'}`}>
                <h2 className="font-serif text-3xl lg:text-4xl text-ink mb-4">{title}</h2>
                <p className="text-charcoal/80 leading-relaxed">{text}</p>
              </div>
            </Reveal>
          ))}
        </ol>
      </section>

      <section className="bg-cream">
        <div className="max-w-[88rem] mx-auto grid lg:grid-cols-2 items-center">
          <Photo src={EDITORIAL.lifestyle} alt="" ratio="4 / 3" />
          <Reveal className="px-6 lg:px-20 py-20">
            <p className="eyebrow mb-5">Philosophy</p>
            <blockquote className="font-serif text-3xl lg:text-4xl text-ink leading-snug">
              “A fine watch is not bought to tell the time. It is kept to <em>mark</em> it.”
            </blockquote>
          </Reveal>
        </div>
      </section>
    </div>
  );
}
