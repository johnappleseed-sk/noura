import { GeoRegion } from '@/types/geo'

export type StoreStatus = 'active' | 'inactive'
export type StoreService = 'pickup' | 'delivery' | 'curbside' | 'b2b-desk'

export interface StoreHoursEntry {
  day: string
  open: string
  close: string
  closed?: boolean
}

export interface StoreLocation {
  id: string
  name: string
  addressLine1: string
  city: string
  state: string
  zipCode: string
  country: string
  phone: string
  latitude: number
  longitude: number
  region: GeoRegion
  hoursSummary: string
  hours: StoreHoursEntry[]
  services: StoreService[]
  freeShippingThreshold: number
  shippingFee: number
  status: StoreStatus
}

export interface StoreCoordinates {
  latitude: number
  longitude: number
}
