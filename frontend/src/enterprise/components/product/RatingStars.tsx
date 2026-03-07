interface RatingStarsProps {
  rating: number
}

/**
 * Renders the RatingStars component.
 *
 * @param param1 The param1 value.
 * @returns The rendered component tree.
 */
export const RatingStars = ({ rating }: RatingStarsProps): JSX.Element => {
  const rounded = Math.round(rating)

  return (
    <div aria-label={`Rated ${rating} out of 5`} className="flex items-center gap-1 text-amber-500">
      {Array.from({ length: 5 }, (_, index) => (
        <span className={index < rounded ? 'opacity-100' : 'opacity-30'} key={index}>
          ★
        </span>
      ))}
      <span className="ml-1 text-xs" style={{ color: 'var(--m3-on-surface-variant)' }}>
        {rating.toFixed(1)}
      </span>
    </div>
  )
}
