import { FormEvent, useState } from 'react'

interface CouponInputProps {
  activeCode: string | null
  onApply: (couponCode: string) => void
}

/**
 * Renders the CouponInput component.
 *
 * @param param1 The param1 value.
 * @returns The rendered component tree.
 */
export const CouponInput = ({ activeCode, onApply }: CouponInputProps): JSX.Element => {
  const [coupon, setCoupon] = useState('')

  /**
   * Handles submit coupon.
   */
  const submitCoupon = (event: FormEvent<HTMLFormElement>): void => {
    event.preventDefault()
    onApply(coupon)
  }

  return (
    <form className="space-y-2" onSubmit={submitCoupon}>
      <label className="block text-sm font-medium" htmlFor="coupon-code">
        Coupon code
      </label>
      <div className="flex gap-2">
        <input
          className="m3-input flex-1"
          id="coupon-code"
          onChange={(event) => setCoupon(event.target.value)}
          placeholder="SAVE10"
          type="text"
          value={coupon}
        />
        <button className="m3-btn m3-btn-filled !h-11 !px-5 !py-2" type="submit">
          Apply
        </button>
      </div>
      {activeCode ? (
        <p className="text-xs text-emerald-600 dark:text-emerald-300">Applied coupon: {activeCode}</p>
      ) : (
        <p className="m3-subtitle text-xs">Supported codes: SAVE10, PREMIUM15, FREESHIP5</p>
      )}
    </form>
  )
}
