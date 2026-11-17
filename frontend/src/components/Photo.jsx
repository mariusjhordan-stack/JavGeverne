/** A photograph with a graceful, request-free fallback: no external placeholder service is ever contacted. */
export default function Photo({ src, alt = '', className = '', imgClassName = '', position = 'center', ratio, eager = false }) {
  return (
    <div className={`photo ${src ? '' : 'photo-empty'} ${className}`} style={ratio ? { aspectRatio: ratio } : undefined}>
      {src && (
        <img
          src={src}
          alt={alt}
          loading={eager ? 'eager' : 'lazy'}
          decoding="async"
          className={imgClassName}
          style={{ objectPosition: position }}
        />
      )}
    </div>
  );
}
