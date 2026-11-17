import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { ArrowLeft, Plus, Minus, ShoppingBag, BarChart3 } from 'lucide-react';
import { toast } from 'sonner';
import { getProductById, getProducts } from '../api/catalog';
import { useCart } from '../context/CartContext';
import { useCompare } from '../context/CompareContext';
import { withUnit } from '../lib/format';

export default function ProductDetailPage() {
  const { id } = useParams();
  const { addItem } = useCart();
  const compare = useCompare();
  const [product, setProduct] = useState(null);
  const [related, setRelated] = useState([]);
  const [loading, setLoading] = useState(true);
  const [quantity, setQuantity] = useState(1);

  useEffect(() => {
    setLoading(true);
    setQuantity(1);
    getProductById(id)
      .then((p) => {
        setProduct(p);
        if (p.categoryId) {
          getProducts({ categoryId: p.categoryId, size: 4 })
            .then((d) => {
              const list = d.content ?? (Array.isArray(d) ? d : []);
              setRelated(list.filter((x) => x.id !== p.id).slice(0, 3));
            })
            .catch(() => {});
        }
      })
      .catch(() => setProduct(null))
      .finally(() => setLoading(false));
  }, [id]);

  if (loading)
    return (
      <div className="flex justify-center items-center min-h-[60vh]">
        <div className="w-10 h-10 border-2 border-gold border-t-transparent rounded-full animate-spin" />
      </div>
    );

  if (!product)
    return (
      <div className="max-w-4xl mx-auto px-6 py-20 text-center">
        <h2 className="font-serif text-3xl text-navy mb-4">Watch Not Found</h2>
        <Link to="/catalog" className="text-gold hover:underline">
          Return to Collection
        </Link>
      </div>
    );

  const specs = [
    ['Case Material', product.caseMaterial],
    ['Case Diameter', withUnit(product.caseDiameter, 'mm')],
    ['Movement', product.movementType],
    ['Water Resistance', product.waterResistance],
    ['Crystal', product.crystalType],
    ['Dial Color', product.dialColor],
    ['Strap Material', product.strapMaterial],
    ['Power Reserve', product.powerReserve],
    ['Reference', product.referenceNumber],
  ].filter(([, v]) => v);

  const handleAddToCart = () => {
    addItem(product, quantity);
    toast.success(`${product.name} added to cart`);
  };

  const handleCompare = () => {
    const result = compare.add(product.id);
    if (result === 'exists') toast.info('Already in comparison');
    else if (result === 'full') toast.error('You can compare up to 3 watches');
    else toast.success('Added to comparison');
  };

  return (
    <div className="min-h-screen bg-white">
      <div className="max-w-7xl mx-auto px-6 py-10">
        <Link to="/catalog" className="inline-flex items-center gap-2 text-sm text-gray-500 hover:text-gold mb-10">
          <ArrowLeft size={16} /> Back to Collection
        </Link>

        <div className="grid md:grid-cols-2 gap-16">
          {/* Image */}
          <div className="aspect-square bg-cream">
            <img
              src={product.imageUrl || 'https://placehold.co/800x800/1a1a2e/c9a94e?text=Watch'}
              alt={product.name}
              className="w-full h-full object-cover"
            />
          </div>

          {/* Details */}
          <div>
            <p className="text-gold tracking-[0.25em] uppercase text-xs mb-2">{product.brandName}</p>
            <h1 className="font-serif text-3xl md:text-4xl text-navy mb-2">{product.name}</h1>
            {product.referenceNumber && (
              <p className="text-sm text-gray-400 mb-6">Ref. {product.referenceNumber}</p>
            )}
            <p className="text-3xl text-navy font-light mb-6">${Number(product.price).toLocaleString()}</p>

            {product.description && (
              <p className="text-gray-600 font-light leading-relaxed mb-8">{product.description}</p>
            )}

            <div className="flex items-center gap-2 mb-4 text-sm">
              <span className={`w-2 h-2 rounded-full ${product.stock > 0 ? 'bg-green-500' : 'bg-red-500'}`} />
              {product.stock > 0 ? `${product.stock} in stock` : 'Out of stock'}
            </div>

            {product.stock > 0 && (
              <div className="flex items-center gap-4 mb-8">
                <div className="flex items-center border border-gray-200">
                  <button onClick={() => setQuantity(Math.max(1, quantity - 1))} className="px-3 py-2 hover:bg-gray-50">
                    <Minus size={16} />
                  </button>
                  <span className="px-4 py-2 min-w-[3rem] text-center text-sm">{quantity}</span>
                  <button onClick={() => setQuantity(Math.min(product.stock, quantity + 1))} className="px-3 py-2 hover:bg-gray-50">
                    <Plus size={16} />
                  </button>
                </div>
                <button
                  onClick={handleAddToCart}
                  className="flex-1 flex items-center justify-center gap-2 bg-navy text-white py-3 text-sm uppercase tracking-[0.15em] hover:bg-navy-light transition-colors"
                >
                  <ShoppingBag size={16} /> Add to Cart
                </button>
              </div>
            )}

            <button
              onClick={handleCompare}
              className="flex items-center gap-2 text-sm text-gray-500 hover:text-gold transition-colors mb-10"
            >
              <BarChart3 size={16} /> Add to Compare
            </button>

            {/* Specs */}
            {specs.length > 0 && (
              <div className="border-t border-gray-100 pt-8">
                <h3 className="font-serif text-xl text-navy mb-4">Specifications</h3>
                <dl className="grid grid-cols-2 gap-y-3 text-sm">
                  {specs.map(([label, value]) => (
                    <div key={label} className="contents">
                      <dt className="text-gray-500">{label}</dt>
                      <dd className="text-charcoal">{value}</dd>
                    </div>
                  ))}
                </dl>
              </div>
            )}
          </div>
        </div>

        {/* Related */}
        {related.length > 0 && (
          <section className="mt-24">
            <h2 className="font-serif text-2xl text-navy mb-8">You May Also Like</h2>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-8">
              {related.map((p) => (
                <Link key={p.id} to={`/product/${p.id}`} className="group block">
                  <div className="aspect-square bg-cream overflow-hidden mb-3">
                    <img
                      src={p.imageUrl || 'https://placehold.co/600x600/1a1a2e/c9a94e?text=Watch'}
                      alt={p.name}
                      className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                    />
                  </div>
                  <p className="text-xs text-gold tracking-[0.15em] uppercase">{p.brandName}</p>
                  <h3 className="font-serif text-base text-navy mt-1">{p.name}</h3>
                  <p className="text-sm mt-1">${Number(p.price).toLocaleString()}</p>
                </Link>
              ))}
            </div>
          </section>
        )}
      </div>
    </div>
  );
}
