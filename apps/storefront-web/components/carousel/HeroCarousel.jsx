'use client'

import { useState, useRef, useEffect, useCallback } from 'react'
import Link from 'next/link'
import { Carousel } from './Carousel'

function HeroAction({ action, className }) {
  if (!action?.text || !action?.href) return null

  if (action.external || action.openInNewTab) {
    return (
      <a
        href={action.href}
        className={className}
        target={action.openInNewTab ? '_blank' : undefined}
        rel={action.openInNewTab ? 'noreferrer noopener' : undefined}
      >
        {action.text}
      </a>
    )
  }

  return (
    <Link href={action.href} className={className}>
      {action.text}
    </Link>
  )
}

/**
 * HeroSlide - Individual hero slide with background image/video support
 */
function HeroSlide({
  slide,
  isActive,
  showSearch = false,
  onSearch,
  priority = false,
}) {
  const videoRef = useRef(null)
  const [videoLoaded, setVideoLoaded] = useState(false)
  const [searchQuery, setSearchQuery] = useState('')

  const {
    id,
    title,
    subtitle,
    description,
    imageUrl,
    imageMobileUrl,
    videoUrl,
    altText,
    cta,
    ctaText,
    ctaLink,
    ctaExternal,
    ctaOpenInNewTab,
    secondaryCta,
    secondaryCtaText,
    secondaryCtaLink,
    secondaryCtaExternal,
    secondaryCtaOpenInNewTab,
    overlay = 'gradient', // 'none' | 'gradient' | 'dark' | 'light'
    textAlign = 'left', // 'left' | 'center' | 'right'
    textColor = 'light', // 'light' | 'dark'
  } = slide

  const primaryAction = cta || (ctaText && ctaLink
    ? { text: ctaText, href: ctaLink, external: Boolean(ctaExternal), openInNewTab: Boolean(ctaOpenInNewTab) }
    : null)
  const secondaryAction = secondaryCta || (secondaryCtaText && secondaryCtaLink
    ? { text: secondaryCtaText, href: secondaryCtaLink, external: Boolean(secondaryCtaExternal), openInNewTab: Boolean(secondaryCtaOpenInNewTab) }
    : null)

  // Handle video playback
  useEffect(() => {
    if (videoRef.current) {
      if (isActive) {
        videoRef.current.play().catch(() => {})
      } else {
        videoRef.current.pause()
        videoRef.current.currentTime = 0
      }
    }
  }, [isActive])

  const handleSearchSubmit = (e) => {
    e.preventDefault()
    if (searchQuery.trim()) {
      onSearch?.(searchQuery)
    }
  }

  const overlayClass = {
    none: '',
    gradient: 'hero-slide__overlay--gradient',
    dark: 'hero-slide__overlay--dark',
    light: 'hero-slide__overlay--light',
  }[overlay]

  const alignClass = {
    left: 'hero-slide__content--left',
    center: 'hero-slide__content--center',
    right: 'hero-slide__content--right',
  }[textAlign]

  const textColorClass = textColor === 'dark' ? 'hero-slide__content--dark' : ''

  return (
    <div className={`hero-slide ${isActive ? 'hero-slide--active' : ''}`}>
      {/* Background */}
      <div className="hero-slide__bg">
        {videoUrl ? (
          <>
            {/* Fallback image while video loads */}
            {imageUrl && !videoLoaded && (
              <picture>
                {imageMobileUrl ? <source media="(max-width: 767px)" srcSet={imageMobileUrl} /> : null}
                <img
                  src={imageUrl}
                  alt={altText || title || ''}
                  className="hero-slide__image"
                  loading={priority ? 'eager' : 'lazy'}
                />
              </picture>
            )}
            <video
              ref={videoRef}
              className={`hero-slide__video ${videoLoaded ? 'hero-slide__video--loaded' : ''}`}
              src={videoUrl}
              muted
              loop
              playsInline
              preload={priority ? 'auto' : 'metadata'}
              onLoadedData={() => setVideoLoaded(true)}
            />
          </>
        ) : imageUrl ? (
          <picture>
            {imageMobileUrl ? <source media="(max-width: 767px)" srcSet={imageMobileUrl} /> : null}
            <img
              src={imageUrl}
              alt={altText || title || ''}
              className="hero-slide__image"
              loading={priority ? 'eager' : 'lazy'}
            />
          </picture>
        ) : (
          <div className="hero-slide__placeholder" />
        )}
      </div>

      {/* Overlay */}
      {overlay !== 'none' && (
        <div className={`hero-slide__overlay ${overlayClass}`} />
      )}

      {/* Content */}
      <div className={`hero-slide__content ${alignClass} ${textColorClass}`}>
        <div className="hero-slide__inner">
          {subtitle && (
            <span className="hero-slide__eyebrow">{subtitle}</span>
          )}
          {title && (
            <h1 className="hero-slide__title">{title}</h1>
          )}
          {description && (
            <p className="hero-slide__description">{description}</p>
          )}

          {/* Search Bar */}
          {showSearch && (
            <form className="hero-slide__search" onSubmit={handleSearchSubmit}>
              <input
                type="text"
                placeholder="Search for products..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="hero-slide__search-input"
              />
              <button type="submit" className="hero-slide__search-btn">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <circle cx="11" cy="11" r="8" />
                  <path d="M21 21l-4.35-4.35" />
                </svg>
              </button>
            </form>
          )}

          {/* CTAs */}
          {(primaryAction || secondaryAction) && (
            <div className="hero-slide__actions">
              <HeroAction action={primaryAction} className="button primary lg hero-slide__cta" />
              <HeroAction action={secondaryAction} className="button ghost lg hero-slide__cta hero-slide__cta--secondary" />
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

/**
 * HeroCarousel - Full-width hero carousel with video support
 * 
 * @param {Object} props
 * @param {Array} props.slides - Array of slide objects
 * @param {boolean} props.autoplay - Enable autoplay
 * @param {number} props.autoplayInterval - Autoplay interval in ms
 * @param {boolean} props.showSearch - Show search bar on slides
 * @param {Function} props.onSearch - Search callback
 * @param {string} props.variant - A/B test variant
 * @param {string} props.className - Additional CSS classes
 */
export function HeroCarousel({
  slides = [],
  autoplay = true,
  autoplayInterval = 6000,
  showSearch = false,
  onSearch,
  variant = 'default',
  className = '',
}) {
  const [activeIndex, setActiveIndex] = useState(0)

  const handleSearch = useCallback((query) => {
    if (onSearch) {
      onSearch(query)
    } else {
      // Default behavior: navigate to products page with search
      window.location.href = `/products?q=${encodeURIComponent(query)}`
    }
  }, [onSearch])

  if (!slides.length) return null

  // A/B test variants
  const variantStyles = {
    default: '',
    compact: 'hero-carousel--compact',
    fullscreen: 'hero-carousel--fullscreen',
    split: 'hero-carousel--split',
  }

  return (
    <div className={`hero-carousel ${variantStyles[variant] || ''} ${className}`}>
      <Carousel
        autoplay={autoplay}
        autoplayInterval={autoplayInterval}
        loop={true}
        showArrows={true}
        showDots={true}
        arrowStyle="hero"
        onSlideChange={setActiveIndex}
        pauseOnHover={true}
        ariaLabel="Hero banner carousel"
      >
        {slides.map((slide, index) => (
          <HeroSlide
            key={slide.id || index}
            slide={slide}
            isActive={index === activeIndex}
            showSearch={showSearch && index === 0}
            onSearch={handleSearch}
            priority={index === 0}
          />
        ))}
      </Carousel>

      {/* Progress bar */}
      {autoplay && slides.length > 1 && (
        <div className="hero-carousel__progress">
          <div
            className="hero-carousel__progress-bar"
            style={{
              animation: `heroProgress ${autoplayInterval}ms linear`,
              animationPlayState: 'running',
            }}
            key={activeIndex}
          />
        </div>
      )}
    </div>
  )
}

export default HeroCarousel
