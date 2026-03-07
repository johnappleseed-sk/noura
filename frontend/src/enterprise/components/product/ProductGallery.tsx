import { useMemo, useState } from 'react'

interface ProductGalleryProps {
  images: string[]
  productName: string
  videos?: string[]
}

type MediaItem = {
  id: string
  kind: 'image' | 'video'
  src: string
}

/**
 * Determines whether is embeddable video.
 *
 * @param src The src value.
 * @returns True when the condition is satisfied; otherwise false.
 */
const isEmbeddableVideo = (src: string): boolean =>
  src.includes('youtube.com') || src.includes('youtu.be') || src.includes('vimeo.com')

export const ProductGallery = ({
  images,
  productName,
  videos = [],
}: ProductGalleryProps): JSX.Element => {
  const media = useMemo<MediaItem[]>(
    () => [
      ...images.map((src, index) => ({ id: `img-${index}`, kind: 'image' as const, src })),
      ...videos.map((src, index) => ({ id: `vid-${index}`, kind: 'video' as const, src })),
    ],
    [images, videos],
  )
  const [activeMediaId, setActiveMediaId] = useState(media[0]?.id ?? '')

  const activeMedia = useMemo(
    () => media.find((entry) => entry.id === activeMediaId) ?? media[0],
    [activeMediaId, media],
  )

  return (
    <section aria-label="Product image gallery" className="space-y-3">
      <div className="panel group relative overflow-hidden">
        {activeMedia?.kind === 'video' ? (
          isEmbeddableVideo(activeMedia.src) ? (
            <iframe
              allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
              allowFullScreen
              className="h-[420px] w-full rounded-3xl"
              src={activeMedia.src}
              title={`${productName} video`}
            />
          ) : (
            <video className="h-[420px] w-full rounded-3xl object-cover" controls src={activeMedia.src} />
          )
        ) : (
          <>
            <img
              alt={`${productName} preview`}
              className="h-[420px] w-full object-cover transition duration-300 group-hover:scale-125"
              src={activeMedia?.src}
            />
            <span className="m3-chip absolute bottom-3 right-3 border-0 bg-black/65 text-white">
              Hover to zoom
            </span>
          </>
        )}
      </div>
      <div className="flex flex-wrap gap-2">
        {media.map((entry, index) => (
          <button
            className={`h-16 w-16 overflow-hidden rounded-2xl border-2 transition ${
              activeMedia?.id === entry.id
                ? 'border-brand-500'
                : 'border-[color:var(--m3-outline-variant)] hover:border-brand-300'
            }`}
            key={entry.id}
            onClick={() => setActiveMediaId(entry.id)}
            type="button"
          >
            {entry.kind === 'image' ? (
              <img
                alt={`${productName} thumbnail ${index + 1}`}
                className="h-full w-full object-cover"
                src={entry.src}
              />
            ) : (
              <span
                aria-label={`${productName} video ${index + 1}`}
                className="flex h-full w-full items-center justify-center bg-gradient-to-br from-slate-700 to-slate-900 text-[11px] font-semibold text-white"
              >
                Video
              </span>
            )}
          </button>
        ))}
      </div>
    </section>
  )
}
