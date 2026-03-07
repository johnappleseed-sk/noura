import { useAppDispatch, useAppSelector } from '@/app/hooks'
import { selectTheme, toggleTheme } from '@/features/ui/uiSlice'

/**
 * Renders the ThemeToggle component.
 *
 * @returns The rendered component tree.
 */
export const ThemeToggle = (): JSX.Element => {
  const dispatch = useAppDispatch()
  const theme = useAppSelector(selectTheme)

  return (
    <button
      aria-label="Toggle dark mode"
      className="m3-btn m3-btn-outlined !px-4 !py-2 !text-xs uppercase tracking-wide"
      onClick={() => dispatch(toggleTheme())}
      type="button"
    >
      {theme === 'dark' ? 'Light mode' : 'Dark mode'}
    </button>
  )
}
