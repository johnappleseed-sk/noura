import { configureStore } from '@reduxjs/toolkit'
import { render } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Provider } from 'react-redux'
import { MemoryRouter } from 'react-router-dom'
import { ProductCard } from '@/components/product/ProductCard'
import { authReducer } from '@/features/auth/authSlice'
import { cartReducer } from '@/features/cart/cartSlice'
import { productsReducer } from '@/features/products/productsSlice'
import { recommendationsReducer } from '@/features/recommendations/recommendationsSlice'
import { storesReducer } from '@/features/stores/storesSlice'
import { uiReducer } from '@/features/ui/uiSlice'
import { wishlistReducer } from '@/features/wishlist/wishlistSlice'
import { mockProducts } from '@/data'

/**
 * Creates create test store.
 *
 * @returns The result of create test store.
 */
const createTestStore = () =>
  configureStore({
    reducer: {
      auth: authReducer,
      cart: cartReducer,
      products: productsReducer,
      recommendations: recommendationsReducer,
      stores: storesReducer,
      ui: uiReducer,
      wishlist: wishlistReducer,
    },
  })

describe('ProductCard', () => {
  it('renders product details and supports adding to cart', async () => {
    const store = createTestStore()
    const user = userEvent.setup()

    const { getByText, getByRole } = render(
      <Provider store={store}>
        <MemoryRouter>
          <ProductCard product={mockProducts[0]} />
        </MemoryRouter>
      </Provider>,
    )

    expect(getByText(mockProducts[0].name)).toBeTruthy()
    await user.click(getByRole('button', { name: /add to cart/i }))
    expect(store.getState().cart.items).toHaveLength(1)
  })
})
