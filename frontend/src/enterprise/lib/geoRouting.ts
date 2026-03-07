import { CurrencyCode, GeoContext, GeoRegion, LanguageCode } from '@/types'

/**
 * Executes region from country.
 *
 * @param countryCode The country code value.
 * @returns The result of region from country.
 */
const regionFromCountry = (countryCode: string): GeoRegion => {
  if (['US', 'CA', 'MX'].includes(countryCode)) {
    return 'us'
  }
  if (
    ['DE', 'FR', 'ES', 'IT', 'NL', 'SE', 'BE', 'CH', 'AT', 'PL', 'IE', 'PT', 'DK', 'FI', 'NO', 'GB'].includes(
      countryCode,
    )
  ) {
    return 'eu'
  }
  if (
    ['TH', 'SG', 'JP', 'KR', 'VN', 'MY', 'ID', 'PH', 'AU', 'NZ', 'IN', 'HK', 'TW', 'CN'].includes(countryCode)
  ) {
    return 'apac'
  }
  return 'global'
}

/**
 * Executes country from locale.
 *
 * @param locale The locale value.
 * @returns The result of country from locale.
 */
const countryFromLocale = (locale: string): string => {
  const pieces = locale.split('-')
  return pieces[1]?.toUpperCase() ?? 'US'
}

/**
 * Executes currency from country.
 *
 * @param countryCode The country code value.
 * @returns The result of currency from country.
 */
const currencyFromCountry = (countryCode: string): CurrencyCode => {
  if (['DE', 'FR', 'ES', 'IT', 'NL', 'AT', 'BE', 'IE', 'PT', 'FI'].includes(countryCode)) {
    return 'EUR'
  }
  if (countryCode === 'GB') {
    return 'GBP'
  }
  if (countryCode === 'JP') {
    return 'JPY'
  }
  if (countryCode === 'TH') {
    return 'THB'
  }
  return 'USD'
}

/**
 * Executes language from locale.
 *
 * @param locale The locale value.
 * @returns The result of language from locale.
 */
const languageFromLocale = (locale: string): LanguageCode => {
  const normalized = locale as LanguageCode
  const supported: LanguageCode[] = ['en-US', 'en-GB', 'de-DE', 'fr-FR', 'th-TH', 'ja-JP']
  return supported.includes(normalized) ? normalized : 'en-US'
}

/**
 * Executes detect geo context.
 *
 * @returns The result of detect geo context.
 */
export const detectGeoContext = (): GeoContext => {
  const locale = navigator.language || 'en-US'
  const countryCode = countryFromLocale(locale)
  const region = regionFromCountry(countryCode)
  const currency = currencyFromCountry(countryCode)
  const language = languageFromLocale(locale)

  return {
    region,
    countryCode,
    locale,
    currency,
    language,
    source: 'client-fallback',
  }
}
