'use client'

import { useState, useEffect, useRef, useCallback, useMemo, createContext, useContext } from 'react'

// ─────────────────────────────────────────────────────────────────────────────
// Carousel Context for A/B Testing and Configuration
// ─────────────────────────────────────────────────────────────────────────────
const CarouselContext = createContext({
  variant: 'default',
  autoplayEnabled: true,
  transitionDuration: 500,
})

export function CarouselProvider({ children, variant = 'default', config = {} }) {
  const value = useMemo(() => ({
    variant,
    autoplayEnabled: config.autoplayEnabled ?? true,
    transitionDuration: config.transitionDuration ?? 500,
    ...config,
  }), [variant, config])

  return (
    <CarouselContext.Provider value={value}>
      {children}
    </CarouselContext.Provider>
  )
}

export function useCarouselConfig() {
  return useContext(CarouselContext)
}

// ─────────────────────────────────────────────────────────────────────────────
// Custom Hooks
// ─────────────────────────────────────────────────────────────────────────────

/**
 * useDebounce - Debounces a value
 */
function useDebounce(value, delay) {
  const [debouncedValue, setDebouncedValue] = useState(value)

  useEffect(() => {
    const handler = setTimeout(() => setDebouncedValue(value), delay)
    return () => clearTimeout(handler)
  }, [value, delay])

  return debouncedValue
}

/**
 * useResizeObserver - Observes element size changes with debouncing
 */
function useResizeObserver(ref, callback, delay = 100) {
  const debouncedCallback = useDebounce(callback, delay)

  useEffect(() => {
    if (!ref.current) return

    const observer = new ResizeObserver((entries) => {
      if (debouncedCallback && entries[0]) {
        debouncedCallback(entries[0].contentRect)
      }
    })

    observer.observe(ref.current)
    return () => observer.disconnect()
  }, [ref, debouncedCallback])
}

/**
 * useSwipe - Touch/swipe gesture handler
 */
function useSwipe(ref, { onSwipeLeft, onSwipeRight, threshold = 50 }) {
  const touchStart = useRef({ x: 0, y: 0 })
  const touchEnd = useRef({ x: 0, y: 0 })

  useEffect(() => {
    const element = ref.current
    if (!element) return

    const handleTouchStart = (e) => {
      touchStart.current = { x: e.touches[0].clientX, y: e.touches[0].clientY }
      touchEnd.current = { x: e.touches[0].clientX, y: e.touches[0].clientY }
    }

    const handleTouchMove = (e) => {
      touchEnd.current = { x: e.touches[0].clientX, y: e.touches[0].clientY }
    }

    const handleTouchEnd = () => {
      const deltaX = touchStart.current.x - touchEnd.current.x
      const deltaY = touchStart.current.y - touchEnd.current.y

      // Only trigger if horizontal swipe is dominant
      if (Math.abs(deltaX) > Math.abs(deltaY) && Math.abs(deltaX) > threshold) {
        if (deltaX > 0) {
          onSwipeLeft?.()
        } else {
          onSwipeRight?.()
        }
      }
    }

    element.addEventListener('touchstart', handleTouchStart, { passive: true })
    element.addEventListener('touchmove', handleTouchMove, { passive: true })
    element.addEventListener('touchend', handleTouchEnd, { passive: true })

    return () => {
      element.removeEventListener('touchstart', handleTouchStart)
      element.removeEventListener('touchmove', handleTouchMove)
      element.removeEventListener('touchend', handleTouchEnd)
    }
  }, [ref, onSwipeLeft, onSwipeRight, threshold])
}

/**
 * useIntersectionObserver - Lazy loading trigger
 */
function useIntersectionObserver(ref, options = {}) {
  const [isIntersecting, setIsIntersecting] = useState(false)

  useEffect(() => {
    if (!ref.current) return

    const observer = new IntersectionObserver(([entry]) => {
      setIsIntersecting(entry.isIntersecting)
    }, { threshold: 0.1, ...options })

    observer.observe(ref.current)
    return () => observer.disconnect()
  }, [ref, options])

  return isIntersecting
}

// ─────────────────────────────────────────────────────────────────────────────
// Base Carousel Component
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Carousel - Base carousel component with full accessibility support
 * 
 * @param {Object} props
 * @param {React.ReactNode[]} props.children - Carousel slides
 * @param {boolean} props.autoplay - Enable autoplay
 * @param {number} props.autoplayInterval - Autoplay interval in ms
 * @param {boolean} props.loop - Enable infinite loop
 * @param {boolean} props.showArrows - Show navigation arrows
 * @param {boolean} props.showDots - Show dot indicators
 * @param {string} props.arrowStyle - Arrow style variant
 * @param {Function} props.onSlideChange - Callback when slide changes
 * @param {number} props.slidesToShow - Number of slides visible
 * @param {number} props.slidesToScroll - Number of slides to scroll
 * @param {string} props.className - Additional CSS classes
 * @param {string} props.ariaLabel - Accessibility label
 */
export function Carousel({
  children,
  autoplay = false,
  autoplayInterval = 5000,
  loop = true,
  showArrows = true,
  showDots = true,
  arrowStyle = 'default',
  onSlideChange,
  slidesToShow = 1,
  slidesToScroll = 1,
  gap = 0,
  className = '',
  ariaLabel = 'Carousel',
  pauseOnHover = true,
  transitionDuration,
  lazyLoad = true,
}) {
  const config = useCarouselConfig()
  const duration = transitionDuration ?? config.transitionDuration

  const [currentIndex, setCurrentIndex] = useState(0)
  const [isPlaying, setIsPlaying] = useState(autoplay && config.autoplayEnabled)
  const [isPaused, setIsPaused] = useState(false)
  const [isTransitioning, setIsTransitioning] = useState(false)
  const [loadedSlides, setLoadedSlides] = useState(new Set([0, 1]))

  const carouselRef = useRef(null)
  const trackRef = useRef(null)
  const autoplayRef = useRef(null)

  const slides = useMemo(() => Array.isArray(children) ? children : [children], [children])
  const totalSlides = slides.length
  const maxIndex = Math.max(0, totalSlides - slidesToShow)

  // Lazy load slides as they come into view
  const isVisible = useIntersectionObserver(carouselRef)

  useEffect(() => {
    if (isVisible && lazyLoad) {
      setLoadedSlides(prev => {
        const slidesToLoad = new Set(prev)
        let hasNew = false
        for (let i = Math.max(0, currentIndex - 1); i <= Math.min(totalSlides - 1, currentIndex + slidesToShow); i++) {
          if (!slidesToLoad.has(i)) {
            slidesToLoad.add(i)
            hasNew = true
          }
        }
        return hasNew ? slidesToLoad : prev
      })
    }
  }, [isVisible, currentIndex, slidesToShow, totalSlides, lazyLoad])

  // Navigation functions
  const goToSlide = useCallback((index, skipTransition = false) => {
    if (isTransitioning && !skipTransition) return

    let newIndex = index
    if (loop) {
      if (index < 0) newIndex = maxIndex
      else if (index > maxIndex) newIndex = 0
    } else {
      newIndex = Math.max(0, Math.min(index, maxIndex))
    }

    if (!skipTransition) {
      setIsTransitioning(true)
      setTimeout(() => setIsTransitioning(false), duration)
    }

    setCurrentIndex(newIndex)
    onSlideChange?.(newIndex)
  }, [isTransitioning, loop, maxIndex, duration, onSlideChange])

  const goToNext = useCallback(() => {
    goToSlide(currentIndex + slidesToScroll)
  }, [currentIndex, slidesToScroll, goToSlide])

  const goToPrev = useCallback(() => {
    goToSlide(currentIndex - slidesToScroll)
  }, [currentIndex, slidesToScroll, goToSlide])

  // Swipe support
  useSwipe(carouselRef, {
    onSwipeLeft: goToNext,
    onSwipeRight: goToPrev,
  })

  // Autoplay
  useEffect(() => {
    if (isPlaying && !isPaused && isVisible) {
      autoplayRef.current = setInterval(goToNext, autoplayInterval)
    }
    return () => {
      if (autoplayRef.current) clearInterval(autoplayRef.current)
    }
  }, [isPlaying, isPaused, isVisible, goToNext, autoplayInterval])

  // Keyboard navigation
  const handleKeyDown = useCallback((e) => {
    switch (e.key) {
      case 'ArrowLeft':
        e.preventDefault()
        goToPrev()
        break
      case 'ArrowRight':
        e.preventDefault()
        goToNext()
        break
      case 'Home':
        e.preventDefault()
        goToSlide(0)
        break
      case 'End':
        e.preventDefault()
        goToSlide(maxIndex)
        break
      case ' ':
        e.preventDefault()
        setIsPlaying((p) => !p)
        break
    }
  }, [goToPrev, goToNext, goToSlide, maxIndex])

  // Pause on hover
  const handleMouseEnter = useCallback(() => {
    if (pauseOnHover) setIsPaused(true)
  }, [pauseOnHover])

  const handleMouseLeave = useCallback(() => {
    if (pauseOnHover) setIsPaused(false)
  }, [pauseOnHover])

  // Calculate track transform
  const slideWidth = slidesToShow > 1 ? `calc((100% - ${gap * (slidesToShow - 1)}px) / ${slidesToShow})` : '100%'
  const trackTransform = `translateX(calc(-${currentIndex} * (${slideWidth} + ${gap}px)))`

  return (
    <div
      ref={carouselRef}
      className={`carousel carousel--${arrowStyle} ${className}`}
      role="region"
      aria-roledescription="carousel"
      aria-label={ariaLabel}
      onKeyDown={handleKeyDown}
      onMouseEnter={handleMouseEnter}
      onMouseLeave={handleMouseLeave}
      tabIndex={0}
    >
      <div className="carousel__viewport">
        <div
          ref={trackRef}
          className="carousel__track"
          style={{
            transform: trackTransform,
            transition: isTransitioning ? `transform ${duration}ms cubic-bezier(0.4, 0, 0.2, 1)` : 'none',
            gap: `${gap}px`,
          }}
          aria-live={isPlaying ? 'off' : 'polite'}
        >
          {slides.map((slide, index) => (
            <div
              key={index}
              className={`carousel__slide ${index === currentIndex ? 'carousel__slide--active' : ''}`}
              role="group"
              aria-roledescription="slide"
              aria-label={`Slide ${index + 1} of ${totalSlides}`}
              aria-hidden={index < currentIndex || index >= currentIndex + slidesToShow}
              style={{ width: slideWidth, flexShrink: 0 }}
            >
              {(!lazyLoad || loadedSlides.has(index)) ? slide : (
                <div className="carousel__slide-placeholder" />
              )}
            </div>
          ))}
        </div>
      </div>

      {/* Navigation Arrows */}
      {showArrows && totalSlides > slidesToShow && (
        <>
          <button
            type="button"
            className="carousel__arrow carousel__arrow--prev"
            onClick={goToPrev}
            disabled={!loop && currentIndex === 0}
            aria-label="Previous slide"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M15 18l-6-6 6-6" />
            </svg>
          </button>
          <button
            type="button"
            className="carousel__arrow carousel__arrow--next"
            onClick={goToNext}
            disabled={!loop && currentIndex === maxIndex}
            aria-label="Next slide"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M9 18l6-6-6-6" />
            </svg>
          </button>
        </>
      )}

      {/* Dot Indicators */}
      {showDots && totalSlides > 1 && slidesToShow === 1 && (
        <div className="carousel__dots" role="tablist" aria-label="Slide indicators">
          {slides.map((_, index) => (
            <button
              key={index}
              type="button"
              className={`carousel__dot ${index === currentIndex ? 'carousel__dot--active' : ''}`}
              onClick={() => goToSlide(index)}
              role="tab"
              aria-selected={index === currentIndex}
              aria-label={`Go to slide ${index + 1}`}
            />
          ))}
        </div>
      )}

      {/* Autoplay Control */}
      {autoplay && (
        <button
          type="button"
          className="carousel__autoplay-toggle"
          onClick={() => setIsPlaying((p) => !p)}
          aria-label={isPlaying ? 'Pause autoplay' : 'Start autoplay'}
        >
          {isPlaying ? (
            <svg viewBox="0 0 24 24" fill="currentColor">
              <rect x="6" y="4" width="4" height="16" />
              <rect x="14" y="4" width="4" height="16" />
            </svg>
          ) : (
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M8 5v14l11-7z" />
            </svg>
          )}
        </button>
      )}

      {/* Screen reader announcements */}
      <div className="sr-only" aria-live="polite" aria-atomic="true">
        Showing slide {currentIndex + 1} of {totalSlides}
      </div>
    </div>
  )
}

export default Carousel
