'use client'

import Link from 'next/link'
import { useEffect, useState } from 'react'
import { Breadcrumbs } from '@/components/navigation'
import AddToCartButton from '@/components/product/AddToCartButton'
import { formatCurrency } from '@/lib/format'
import { clearWishlist, getWishlistItems, removeWishlistItem, subscribeWishlist } from '@/lib/wishlist'

export default function WishlistPage() {
  const [items, setItems] = useState([])

  useEffect(() => {
    const sync = () => setItems(getWishlistItems())
    sync()
    return subscribeWishlist(sync)
  }, [])

  const handleRemove = (id) => removeWishlistItem(id)
  const handleClear = () => clearWishlist()

  return (
    <>
      <section className="hero-compact" style={{ paddingBlock: 20 }}>
        <div className="container">
          <Breadcrumbs items={[{ label: 'Home', href: '/' }, { label: 'Wishlist' }]} />
        </div>
      </section>

      <section className="featured-section" style={{ paddingTop: 32, paddingBottom: 48 }}>
        <div className="container">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12, marginBottom: 20 }}>
            <h1 style={{ margin: 0 }}>Wishlist</h1>
            {items.length > 0 ? (
              <button type="button" className="button ghost sm" onClick={handleClear}>Clear wishlist</button>
            ) : null}
          </div>

          {items.length === 0 ? (
            <div className="panel" style={{ padding: 36, textAlign: 'center' }}>
              <h2 style={{ marginBottom: 8 }}>Your wishlist is empty</h2>
              <p style={{ color: 'var(--muted)', marginBottom: 20 }}>Save products to compare or buy later.</p>
              <Link href="/products" className="button primary">Browse products</Link>
            </div>
          ) : (
            <div className="product-grid catalog-product-grid">
              {items.map((item) => (
                <article key={item.id} className="product-card catalog-card">
                  <Link href={`/products/${item.id}`} className="product-visual" style={item.imageUrl ? { backgroundImage: `url(${item.imageUrl})` } : undefined}>
                    {!item.imageUrl ? <span>{item.categoryName || 'Product'}</span> : null}
                  </Link>
                  <div className="product-meta">
                    <span className="product-category">{item.categoryName || 'Saved item'}</span>
                    <strong>
                      <Link href={`/products/${item.id}`}>{item.name || 'Product'}</Link>
                    </strong>
                    <p style={{ margin: 0 }}>{formatCurrency(item.price || 0)}</p>
                    <div style={{ display: 'grid', gap: 8 }}>
                      <AddToCartButton productId={item.id} />
                      <button type="button" className="button ghost sm" onClick={() => handleRemove(item.id)}>
                        Remove
                      </button>
                    </div>
                  </div>
                </article>
              ))}
            </div>
          )}
        </div>
      </section>
    </>
  )
}

