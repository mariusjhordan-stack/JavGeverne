import { Link } from 'react-router-dom';
import { useLanguage } from '../context/LanguageContext';

export default function NotFoundPage() {
  const { t } = useLanguage();
  return (
    <div className="min-h-[60vh] flex flex-col items-center justify-center px-6 text-center">
      <p className="text-gold tracking-[0.3em] uppercase text-xs mb-4">{t('Error 404')}</p>
      <h1 className="font-serif text-4xl text-navy mb-4">{t('This page does not exist')}</h1>
      <p className="text-gray-600 mb-8 max-w-md">{t('The address may have changed, or the piece you were looking for is no longer available.')}</p>
      <Link to="/collection" className="bg-navy text-white px-8 py-3 text-sm uppercase tracking-[0.15em] hover:bg-navy-light transition-colors">
        {t('Explore the collection')}
      </Link>
    </div>
  );
}
