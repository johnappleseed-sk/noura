'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { addCartItem, resolveCustomerToken } from '@/lib/api'

export default function AddToCartButton({ productId, disabled = false }) {
  const router = useRouter()
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState('')

  const handleAdd = async () => {
    const token = resolveCustomerToken()
    if (!token) {
      router.push('/auth/login')
      return
    }

    setLoading(true)
    setMessage('')
    try {
      await addCartItem(token, { productId, quantity: 1 })
      setMessage('Added to cart!')
      setTimeout(() => setMessage(''), 2500)
    } catch (err) {
      setMessage(err.message || 'Failed to add to cart.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div>
      <button
        type="button"
        className="button primary lg"
        style={{ width: '100%' }}
        onClick={handleAdd}
        disabled={disabled || loading}
      >
        {loading ? 'Adding...' : 'Add to Cart'}
      </button>
      {message && (
        <p style={{
          fontSize: '0.82rem',
          marginTop: 6,
          marginBottom: 0,
          color: message.includes('Added') ? 'var(--success)' : 'var(--danger)',
          fontWeight: 600
        }}>
          {message}
        </p>
      )}
    </div>
  )
}
