export type GeoRegion = 'us' | 'eu' | 'apac' | 'global'
export type CurrencyCode = 'USD' | 'EUR' | 'GBP' | 'THB' | 'JPY'
export type LanguageCode = 'en-US' | 'en-GB' | 'de-DE' | 'fr-FR' | 'th-TH' | 'ja-JP'

export interface GeoContext {
  region: GeoRegion
  countryCode: string
  locale: string
  currency: CurrencyCode
  language: LanguageCode
  source: 'client-fallback' | 'edge-header'
}
