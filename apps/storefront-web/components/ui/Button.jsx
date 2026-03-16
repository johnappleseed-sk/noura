'use client'

import { forwardRef } from 'react'
import Link from 'next/link'

/**
 * Button — Primary interactive element.
 *
 * Variants: primary | accent | success | ghost | white | danger | link
 * Sizes:    sm | md (default) | lg
 * States:   loading | disabled | icon-only (square)
 *
 * Pass `href` to render a Next.js Link instead of a <button>.
 */
const Button = forwardRef(function Button(
  {
    children,
    variant = 'primary',
    size,
    pill,
    loading,
    disabled,
    iconOnly,
    href,
    className = '',
    ...rest
  },
  ref
) {
  const cls = [
    'button',
    variant,
    size && size,
    pill && 'pill',
    iconOnly && 'icon-only',
    loading && 'is-loading',
    className,
  ]
    .filter(Boolean)
    .join(' ')

  if (href) {
    return (
      <Link ref={ref} href={href} className={cls} {...rest}>
        {loading && <span className="btn-spinner" aria-hidden="true" />}
        {children}
      </Link>
    )
  }

  return (
    <button ref={ref} className={cls} disabled={disabled || loading} {...rest}>
      {loading && <span className="btn-spinner" aria-hidden="true" />}
      {children}
    </button>
  )
})

export default Button
