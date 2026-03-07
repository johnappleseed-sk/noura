export type PersonalizationSegment = 'new' | 'explorer' | 'buyer' | 'b2b'

export interface PersonalizationState {
  sessionId: string
  segment: PersonalizationSegment
  headline: string
  subheadline: string
}
