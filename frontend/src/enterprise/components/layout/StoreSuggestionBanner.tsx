import { useMemo } from 'react'
import { useAppDispatch, useAppSelector } from '@/app/hooks'
import { pushNotification } from '@/features/notifications/notificationsSlice'
import { refreshRecommendations } from '@/features/recommendations/recommendationsSlice'
import { dismissStoreSuggestion, selectStoreById } from '@/features/stores/storesSlice'

/**
 * Renders the StoreSuggestionBanner component.
 *
 * @returns The rendered component tree.
 */
export const StoreSuggestionBanner = (): JSX.Element | null => {
  const dispatch = useAppDispatch()
  const stores = useAppSelector((state) => state.stores)

  const suggestedStore = useMemo(
    () => stores.availableStores.find((store) => store.id === stores.suggestedStoreId) ?? null,
    [stores.availableStores, stores.suggestedStoreId],
  )

  if (!suggestedStore || stores.selectedStoreId || stores.suggestionDismissed) {
    return null
  }

  /**
   * Executes apply suggested store.
   *
   * @returns No value.
   */
  const applySuggestedStore = (): void => {
    dispatch(selectStoreById(suggestedStore.id))
    void dispatch(refreshRecommendations())
    dispatch(
      pushNotification({
        id: `store-suggestion-apply-${Date.now()}`,
        title: 'Store selected',
        description: `${suggestedStore.name} is now your active store.`,
        category: 'store',
        createdAt: new Date().toISOString(),
        read: false,
      }),
    )
  }

  return (
    <div className="border-b px-4 py-2 text-xs sm:px-6 lg:px-8" style={{ borderColor: 'var(--m3-outline-variant)', background: 'var(--m3-primary-container)', color: 'var(--m3-on-primary-container)' }}>
      <div className="mx-auto flex max-w-none flex-wrap items-center justify-between gap-2">
        <p>
          Nearest store suggestion: <strong>{suggestedStore.name}</strong> ({suggestedStore.city})
        </p>
        <div className="flex items-center gap-2">
          <button className="m3-btn m3-btn-filled !h-8 !rounded-xl !px-3 !py-1 text-xs" onClick={applySuggestedStore} type="button">
            Set as my store
          </button>
          <button className="m3-btn m3-btn-outlined !h-8 !rounded-xl !px-3 !py-1 text-xs" onClick={() => dispatch(dismissStoreSuggestion())} type="button">
            Dismiss
          </button>
        </div>
      </div>
    </div>
  )
}
