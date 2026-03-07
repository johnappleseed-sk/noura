import { createSlice, PayloadAction } from '@reduxjs/toolkit'
import { RootState } from '@/app/store'
import { PersonalizationSegment, PersonalizationState } from '@/types'

/**
 * Creates build copy.
 *
 * @param segment The segment value.
 * @returns The result of build copy.
 */
const buildCopy = (segment: PersonalizationSegment): Pick<PersonalizationState, 'headline' | 'subheadline'> => {
  switch (segment) {
    case 'buyer':
      return {
        headline: 'High-intent picks selected for your current purchase cycle',
        subheadline: 'AI is prioritizing fast-ship and top-rated products based on your recent behavior.',
      }
    case 'b2b':
      return {
        headline: 'Enterprise buying workspace tailored for your account',
        subheadline: 'Quick reorder, order tracking, and account-level recommendations are enabled.',
      }
    case 'explorer':
      return {
        headline: 'Discover trending products matched to your session',
        subheadline: 'AI recommendations adapt to your browsing and category exploration in real-time.',
      }
    case 'new':
    default:
      return {
        headline: 'Welcome to your AI-personalized storefront',
        subheadline: 'Start browsing and we will tailor products, offers, and support to your session.',
      }
  }
}

/**
 * Retrieves get session id.
 *
 * @returns The result of get session id.
 */
const getSessionId = (): string => {
  const existing = sessionStorage.getItem('enterprise_session_id')
  if (existing) {
    return existing
  }
  const value = `sess-${crypto.randomUUID()}`
  sessionStorage.setItem('enterprise_session_id', value)
  return value
}

const initialSegment: PersonalizationSegment = 'new'

const initialState: PersonalizationState = {
  sessionId: getSessionId(),
  segment: initialSegment,
  ...buildCopy(initialSegment),
}

const personalizationSlice = createSlice({
  name: 'personalization',
  initialState,
  reducers: {
    updatePersonalizationSegment: (state, { payload }: PayloadAction<PersonalizationSegment>) => {
      state.segment = payload
      const copy = buildCopy(payload)
      state.headline = copy.headline
      state.subheadline = copy.subheadline
    },
  },
})

export const { updatePersonalizationSegment } = personalizationSlice.actions
export const selectPersonalization = (state: RootState): PersonalizationState => state.personalization
export const personalizationReducer = personalizationSlice.reducer
