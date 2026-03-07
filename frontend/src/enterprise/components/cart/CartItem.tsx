import { CartItem as CartItemType } from '@/features/cart/cartSlice'
import { formatCurrency } from '@/utils/currency'

interface CartItemProps {
  item: CartItemType
  onUpdateQuantity: (quantity: number) => void
  onRemove: () => void
}

/**
 * Renders the CartItem component.
 *
 * @param param1 The param1 value.
 * @returns The rendered component tree.
 */
export const CartItem = ({ item, onUpdateQuantity, onRemove }: CartItemProps): JSX.Element => (
  <article className="panel flex flex-col gap-4 p-4 sm:flex-row sm:items-center sm:justify-between">
    <div className="flex items-center gap-4">
      <img alt={item.name} className="h-20 w-20 rounded-2xl object-cover" src={item.image} />
      <div>
        <h3 className="font-semibold">{item.name}</h3>
        <p className="m3-subtitle">{formatCurrency(item.price)} each</p>
      </div>
    </div>

    <div className="flex items-center gap-3">
      <label className="sr-only" htmlFor={`qty-${item.productId}`}>
        Quantity
      </label>
      <input
        className="m3-input !h-10 w-20 !rounded-xl !px-2 !py-1 text-sm"
        id={`qty-${item.productId}`}
        min={1}
        onChange={(event) => onUpdateQuantity(Number(event.target.value))}
        type="number"
        value={item.quantity}
      />
      <div className="w-24 text-right text-sm font-semibold">{formatCurrency(item.price * item.quantity)}</div>
      <button
        className="m3-btn m3-btn-outlined !h-10 !rounded-full !px-4 !py-1 text-sm text-rose-700"
        onClick={onRemove}
        type="button"
      >
        Remove
      </button>
    </div>
  </article>
)
