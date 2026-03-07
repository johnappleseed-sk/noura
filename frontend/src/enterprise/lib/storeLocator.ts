import { GeoContext, StoreCoordinates, StoreHoursEntry, StoreLocation } from '@/types'

const toRadians = (value: number): number => (value * Math.PI) / 180

/**
 * Executes calculate distance km.
 *
 * @param fromLatitude The from latitude value.
 * @param fromLongitude The from longitude value.
 * @param toLatitude The to latitude value.
 * @param toLongitude The to longitude value.
 * @returns The result of calculate distance km.
 */
export const calculateDistanceKm = (
  fromLatitude: number,
  fromLongitude: number,
  toLatitude: number,
  toLongitude: number,
): number => {
  const earthRadiusKm = 6371
  const latitudeDelta = toRadians(toLatitude - fromLatitude)
  const longitudeDelta = toRadians(toLongitude - fromLongitude)

  const arc =
    Math.sin(latitudeDelta / 2) * Math.sin(latitudeDelta / 2) +
    Math.cos(toRadians(fromLatitude)) *
      Math.cos(toRadians(toLatitude)) *
      Math.sin(longitudeDelta / 2) *
      Math.sin(longitudeDelta / 2)

  const centralAngle = 2 * Math.atan2(Math.sqrt(arc), Math.sqrt(1 - arc))
  return earthRadiusKm * centralAngle
}

/**
 * Executes geo context to coordinates.
 *
 * @param geo The geo value.
 * @returns The result of geo context to coordinates.
 */
export const geoContextToCoordinates = (geo: GeoContext): StoreCoordinates => {
  switch (geo.region) {
    case 'us':
      return { latitude: 39.8283, longitude: -98.5795 }
    case 'eu':
      return { latitude: 50.1109, longitude: 8.6821 }
    case 'apac':
      return { latitude: 13.7563, longitude: 100.5018 }
    case 'global':
    default:
      return { latitude: 37.0902, longitude: -95.7129 }
  }
}

/**
 * Retrieves get stores sorted by distance.
 *
 * @param stores The stores value.
 * @param coordinates The coordinates value.
 * @param param3 The param3 value.
 * @returns The result of get stores sorted by distance.
 */
export const getStoresSortedByDistance = (
  stores: StoreLocation[],
  coordinates: StoreCoordinates,
): Array<StoreLocation & { distanceKm: number }> =>
  stores
    .map((store) => ({
      ...store,
      distanceKm: calculateDistanceKm(
        coordinates.latitude,
        coordinates.longitude,
        store.latitude,
        store.longitude,
      ),
    }))
    .sort((left, right) => left.distanceKm - right.distanceKm)

/**
 * Retrieves get nearest store.
 *
 * @param stores The stores value.
 * @param coordinates The coordinates value.
 * @returns The result of get nearest store.
 */
export const getNearestStore = (
  stores: StoreLocation[],
  coordinates: StoreCoordinates,
): (StoreLocation & { distanceKm: number }) | null => {
  const sorted = getStoresSortedByDistance(stores, coordinates)
  return sorted[0] ?? null
}

/**
 * Executes matches store query.
 *
 * @param store The store value.
 * @param query The search query text.
 * @returns The result of matches store query.
 */
export const matchesStoreQuery = (store: StoreLocation, query: string): boolean => {
  const normalized = query.trim().toLowerCase()
  if (!normalized) {
    return true
  }

  const haystack = [store.name, store.city, store.state, store.zipCode, store.addressLine1]
    .join(' ')
    .toLowerCase()

  return haystack.includes(normalized)
}

const daySequence = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']

/**
 * Executes as minutes.
 *
 * @param value The value value.
 * @returns The result of as minutes.
 */
const asMinutes = (value: string): number => {
  const [hour, minute] = value.split(':').map(Number)
  return (hour ?? 0) * 60 + (minute ?? 0)
}

/**
 * Retrieves get day hours.
 *
 * @param hours The hours value.
 * @param day The day value.
 * @returns The result of get day hours.
 */
const getDayHours = (hours: StoreHoursEntry[], day: string): StoreHoursEntry | undefined =>
  hours.find((entry) => entry.day === day)

/**
 * Determines whether is store open now.
 *
 * @param store The store value.
 * @param at The at value.
 * @returns True when the condition is satisfied; otherwise false.
 */
export const isStoreOpenNow = (store: StoreLocation, at = new Date()): boolean => {
  const day = daySequence[at.getDay()] ?? 'Mon'
  const hours = getDayHours(store.hours, day)
  if (!hours || hours.closed) {
    return false
  }
  const nowMinutes = at.getHours() * 60 + at.getMinutes()
  return nowMinutes >= asMinutes(hours.open) && nowMinutes <= asMinutes(hours.close)
}

/**
 * Executes format store address.
 *
 * @param store The store value.
 * @returns The result of format store address.
 */
export const formatStoreAddress = (store: StoreLocation): string =>
  `${store.addressLine1}, ${store.city}, ${store.state} ${store.zipCode}`
