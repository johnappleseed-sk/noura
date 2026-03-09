'use client'

/**
 * Avatar — User profile image/initials display.
 *
 * Sizes: xs (24) | sm (32) | md (40) | lg (56) | xl (80)
 */
export default function Avatar({ src, alt, name, size = 'md', className = '' }) {
  const initials = name
    ? name
        .split(' ')
        .map((w) => w[0])
        .join('')
        .slice(0, 2)
        .toUpperCase()
    : '?'

  return (
    <div className={`avatar avatar-${size} ${className}`} title={name || alt}>
      {src ? (
        <img src={src} alt={alt || name || 'Avatar'} className="avatar-img" />
      ) : (
        <span className="avatar-initials">{initials}</span>
      )}
    </div>
  )
}

/**
 * AvatarGroup — Stacked avatar list.
 */
export function AvatarGroup({ items = [], max = 4, size = 'sm', className = '' }) {
  const visible = items.slice(0, max)
  const overflow = items.length - max

  return (
    <div className={`avatar-group ${className}`}>
      {visible.map((item, i) => (
        <Avatar key={i} {...item} size={size} />
      ))}
      {overflow > 0 && (
        <div className={`avatar avatar-${size} avatar-overflow`}>
          <span className="avatar-initials">+{overflow}</span>
        </div>
      )}
    </div>
  )
}
