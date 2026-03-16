'use client'

import { useEffect, useMemo, useState } from 'react'
import Link from 'next/link'
import {
  addCustomerAddress,
  deleteCustomerAddress,
  forwardGeocode,
  getCustomerAddresses,
  getNearbyStores,
  resolveCustomerToken,
  resolveLocation,
  setDefaultCustomerAddress,
  validateServiceArea
} from '@/lib/api'
import Badge from '@/components/ui/Badge'
import { Breadcrumbs } from '@/components/navigation'
import LocationPickerMap from '@/components/location/LocationPickerMap'

const EMPTY_FORM = {
  label: '',
  recipientName: '',
  phone: '',
  line1: '',
  line2: '',
  district: '',
  city: '',
  stateProvince: '',
  postalCode: '',
  countryCode: 'United States',
  latitude: '',
  longitude: '',
  accuracyMeters: '',
  placeId: '',
  formattedAddress: '',
  deliveryInstructions: '',
  defaultShipping: true,
  defaultBilling: false
}

function derivePrimaryAddressLine(formattedAddress) {
  return String(formattedAddress || '')
    .split(',')
    .map((segment) => segment.trim())
    .find(Boolean) || ''
}

function formatDistance(distanceMeters) {
  if (distanceMeters == null) {
    return null
  }
  if (distanceMeters < 1000) {
    return `${distanceMeters} m`
  }
  return `${(distanceMeters / 1000).toFixed(1)} km`
}

function validationPresentation(status) {
  switch (status) {
    case 'VALID':
      return { label: 'Deliverable', variant: 'success' }
    case 'OUT_OF_SERVICE_AREA':
      return { label: 'Outside service area', variant: 'danger' }
    case 'OUT_OF_STORE_RADIUS':
      return { label: 'Outside store radius', variant: 'warning' }
    case 'STORE_CLOSED':
      return { label: 'Store currently closed', variant: 'warning' }
    case 'STORE_UNAVAILABLE':
      return { label: 'Store unavailable', variant: 'danger' }
    default:
      return { label: 'Not verified', variant: 'neutral' }
  }
}

function eligibilityPresentation(eligibility) {
  const reason = eligibility?.eligibilityReason || null
  if (!reason) {
    return null
  }

  switch (reason) {
    case 'AVAILABLE':
    case 'AVAILABLE_NO_SERVICE_AREAS':
      return {
        title: 'Delivery available',
        detail: eligibility?.distanceMeters != null
          ? `Eligible store is ${formatDistance(eligibility.distanceMeters)} away.`
          : 'This coordinate is currently within delivery coverage.',
        tone: 'success'
      }
    case 'SERVICE_AREA_MISS':
      return {
        title: 'Outside active service areas',
        detail: 'Move the pin or search for a closer address to continue with delivery.',
        tone: 'danger'
      }
    case 'OUT_OF_RANGE':
    case 'OUT_OF_STORE_RADIUS':
      return {
        title: 'Outside store delivery radius',
        detail: 'The selected point is beyond the delivery radius of nearby active stores.',
        tone: 'warning'
      }
    case 'STORE_CLOSED':
      return {
        title: 'Nearest store is closed',
        detail: 'The address is recognized, but the matched store is outside service hours.',
        tone: 'warning'
      }
    case 'NO_STORE_AVAILABLE':
      return {
        title: 'No eligible store available',
        detail: 'There is no active delivery-capable store serving this coordinate right now.',
        tone: 'danger'
      }
    case 'PICKUP_ONLY_AREA':
      return {
        title: 'Pickup-only coverage',
        detail: 'This area is configured for pickup, not delivery.',
        tone: 'warning'
      }
    case 'DELIVERY_ONLY_AREA':
      return {
        title: 'Delivery-only area',
        detail: 'This coordinate is only available for delivery rules, not pickup.',
        tone: 'warning'
      }
    default:
      return {
        title: reason.replace(/_/g, ' '),
        detail: 'Location validation completed with a provider or rules-based fallback.',
        tone: 'neutral'
      }
  }
}

function notificationStyles(tone) {
  switch (tone) {
    case 'success':
      return { background: '#f0fdf4', borderLeft: '3px solid var(--success)', color: 'var(--success)' }
    case 'warning':
      return { background: '#fff7ed', borderLeft: '3px solid #c1672c', color: '#9a3412' }
    case 'danger':
      return { background: '#fef2f2', borderLeft: '3px solid var(--danger)', color: 'var(--danger)' }
    default:
      return { background: '#f8fafc', borderLeft: '3px solid var(--line)', color: 'var(--muted)' }
  }
}

export default function AccountAddressesPage() {
  const [token, setToken] = useState(null)
  const [addresses, setAddresses] = useState([])
  const [loading, setLoading] = useState(false)
  const [locationLoading, setLocationLoading] = useState(false)
  const [searchLoading, setSearchLoading] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState(EMPTY_FORM)
  const [searchQuery, setSearchQuery] = useState('')
  const [searchResults, setSearchResults] = useState([])
  const [eligibility, setEligibility] = useState(null)
  const [nearbyStores, setNearbyStores] = useState([])

  const coordinates = useMemo(() => {
    const latitude = Number(form.latitude)
    const longitude = Number(form.longitude)
    if (Number.isNaN(latitude) || Number.isNaN(longitude)) {
      return { latitude: null, longitude: null }
    }
    return { latitude, longitude }
  }, [form.latitude, form.longitude])

  const eligibilityMeta = eligibilityPresentation(eligibility)

  const resetForm = () => {
    setForm(EMPTY_FORM)
    setEligibility(null)
    setNearbyStores([])
    setSearchQuery('')
    setSearchResults([])
  }

  const loadAddresses = async (activeToken) => {
    try {
      const list = await getCustomerAddresses(activeToken)
      setAddresses(Array.isArray(list) ? list : [])
    } catch (err) {
      setError(err.message || 'Unable to load addresses.')
    }
  }

  useEffect(() => {
    const activeToken = resolveCustomerToken()
    if (!activeToken) {
      setError('Please sign in to manage addresses.')
      return
    }
    setToken(activeToken)
    loadAddresses(activeToken)
  }, [])

  const onField = (field, value) => {
    setForm((current) => ({ ...current, [field]: value }))
  }

  const applyLocationDetails = ({ geocode, latitude, longitude, accuracyMeters }) => {
    setForm((current) => ({
      ...current,
      latitude: latitude == null ? '' : String(latitude),
      longitude: longitude == null ? '' : String(longitude),
      accuracyMeters: accuracyMeters == null ? '' : String(Math.round(accuracyMeters)),
      line1: current.line1 || derivePrimaryAddressLine(geocode?.formattedAddress),
      city: geocode?.city || current.city,
      district: geocode?.district || current.district,
      stateProvince: geocode?.region || current.stateProvince,
      postalCode: geocode?.postalCode || current.postalCode,
      countryCode: geocode?.country || current.countryCode,
      placeId: geocode?.placeId || current.placeId,
      formattedAddress: geocode?.formattedAddress || current.formattedAddress
    }))
  }

  const loadCoverageSignals = async (latitude, longitude) => {
    if (latitude == null || longitude == null) {
      setEligibility(null)
      setNearbyStores([])
      return
    }

    const [serviceEligibility, stores] = await Promise.all([
      validateServiceArea({ latitude, longitude, serviceType: 'DELIVERY' }),
      getNearbyStores({ latitude, longitude, serviceType: 'DELIVERY', limit: 3 })
    ])

    setEligibility(serviceEligibility)
    setNearbyStores(stores)
  }

  const resolveCoordinates = async ({ latitude, longitude, accuracyMeters, source, consentGiven }) => {
    if (!token) {
      return
    }

    setLocationLoading(true)
    setError('')
    try {
      const resolved = await resolveLocation(token, {
        latitude,
        longitude,
        accuracyMeters,
        source,
        consentGiven,
        persist: false,
        purpose: 'storefront_address_capture',
        serviceType: 'DELIVERY'
      })

      applyLocationDetails({
        geocode: resolved?.geocode,
        latitude,
        longitude,
        accuracyMeters
      })
      setEligibility(resolved?.eligibility || null)
      const stores = await getNearbyStores({ latitude, longitude, serviceType: 'DELIVERY', limit: 3 })
      setNearbyStores(stores)
      setMessage(consentGiven ? 'Current location captured. Review the address details before saving.' : 'Pin updated. Review the address details before saving.')
    } catch (err) {
      setError(err.message || 'Unable to resolve this location.')
    } finally {
      setLocationLoading(false)
    }
  }

  const handleUseCurrentLocation = async () => {
    if (typeof navigator === 'undefined' || !navigator.geolocation) {
      setError('Browser geolocation is not available on this device.')
      return
    }

    setMessage('')
    setError('')
    setLocationLoading(true)

    navigator.geolocation.getCurrentPosition(
      async (position) => {
        await resolveCoordinates({
          latitude: Number(position.coords.latitude.toFixed(7)),
          longitude: Number(position.coords.longitude.toFixed(7)),
          accuracyMeters: position.coords.accuracy,
          source: 'BROWSER',
          consentGiven: true
        })
      },
      (geoError) => {
        setLocationLoading(false)
        if (geoError?.code === 1) {
          setError('Location permission was denied. Search for the address or place the pin manually.')
          return
        }
        setError('Unable to capture your current location. Search for the address or place the pin manually.')
      },
      {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 0
      }
    )
  }

  const handleLocationSearch = async (event) => {
    event.preventDefault()
    if (!searchQuery.trim()) {
      return
    }

    setSearchLoading(true)
    setError('')
    try {
      const results = await forwardGeocode({
        query: searchQuery.trim(),
        limit: 5,
        countryCodes: 'us',
        locale: 'en'
      })
      setSearchResults(results)
      if (!results.length) {
        setMessage('No place matches were found. Try a street, district, or landmark.')
      }
    } catch (err) {
      setError(err.message || 'Unable to search for places.')
    } finally {
      setSearchLoading(false)
    }
  }

  const handleSearchSelection = async (result) => {
    if (!result) {
      return
    }

    setSearchResults([])
    setSearchQuery(result.formattedAddress || '')
    applyLocationDetails({
      geocode: result,
      latitude: result.latitude,
      longitude: result.longitude,
      accuracyMeters: null
    })

    setLocationLoading(true)
    setError('')
    try {
      await loadCoverageSignals(result.latitude, result.longitude)
      setMessage('Place selected. Review the pin and structured address details before saving.')
    } catch (err) {
      setError(err.message || 'Unable to validate the selected place.')
    } finally {
      setLocationLoading(false)
    }
  }

  const handleManualCoordinateCheck = async () => {
    if (coordinates.latitude == null || coordinates.longitude == null) {
      setError('Enter both latitude and longitude to validate a manual pin.')
      return
    }

    await resolveCoordinates({
      latitude: coordinates.latitude,
      longitude: coordinates.longitude,
      accuracyMeters: form.accuracyMeters ? Number(form.accuracyMeters) : null,
      source: 'MANUAL_PIN',
      consentGiven: false
    })
  }

  const submit = async (event) => {
    event.preventDefault()
    setLoading(true)
    setError('')
    setMessage('')

    try {
      await addCustomerAddress(token, {
        label: form.label.trim() || null,
        recipientName: form.recipientName.trim(),
        phone: form.phone.trim() || null,
        line1: form.line1.trim(),
        line2: form.line2.trim() || null,
        district: form.district.trim() || null,
        city: form.city.trim(),
        stateProvince: form.stateProvince.trim(),
        postalCode: form.postalCode.trim(),
        countryCode: form.countryCode.trim(),
        latitude: coordinates.latitude,
        longitude: coordinates.longitude,
        accuracyMeters: form.accuracyMeters ? Number(form.accuracyMeters) : null,
        placeId: form.placeId.trim() || null,
        formattedAddress: form.formattedAddress.trim() || null,
        deliveryInstructions: form.deliveryInstructions.trim() || null,
        defaultShipping: form.defaultShipping,
        defaultBilling: form.defaultBilling
      })
      setMessage('Address saved.')
      resetForm()
      setShowForm(false)
      await loadAddresses(token)
    } catch (err) {
      setError(err.message || 'Unable to save address.')
    } finally {
      setLoading(false)
    }
  }

  const removeAddress = async (addressId) => {
    setLoading(true)
    setError('')
    setMessage('')
    try {
      await deleteCustomerAddress(token, addressId)
      await loadAddresses(token)
      setMessage('Address removed.')
    } catch (err) {
      setError(err.message || 'Unable to remove address.')
    } finally {
      setLoading(false)
    }
  }

  const markAsDefault = async (addressId) => {
    setLoading(true)
    setError('')
    setMessage('')
    try {
      await setDefaultCustomerAddress(token, addressId)
      await loadAddresses(token)
      setMessage('Default delivery address updated.')
    } catch (err) {
      setError(err.message || 'Unable to update the default address.')
    } finally {
      setLoading(false)
    }
  }

  if (!token && error) {
    return (
      <>
        <section className="hero-compact" style={{ paddingBlock: 20 }}>
          <div className="container">
            <Breadcrumbs items={[{ label: 'Home', href: '/' }, { label: 'Account', href: '/auth' }, { label: 'Addresses' }]} />
          </div>
        </section>
        <section className="featured-section">
          <div className="container" style={{ maxWidth: 'var(--max-w-narrow)' }}>
            <div className="panel" style={{ padding: 40, textAlign: 'center' }}>
              <p style={{ color: 'var(--danger)' }}>{error}</p>
              <Link href="/auth/login" className="button primary">Sign In</Link>
            </div>
          </div>
        </section>
      </>
    )
  }

  return (
    <>
      <section className="hero-compact" style={{ paddingBlock: 20 }}>
        <div className="container">
          <Breadcrumbs items={[{ label: 'Home', href: '/' }, { label: 'Account', href: '/auth' }, { label: 'Addresses' }]} />
        </div>
      </section>

      <section className="featured-section" style={{ paddingTop: 32, paddingBottom: 48 }}>
        <div className="container" style={{ maxWidth: 'min(1180px, 100%)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 16, marginBottom: 24, flexWrap: 'wrap' }}>
            <div>
              <h1 style={{ margin: 0 }}>Delivery Addresses</h1>
              <p style={{ margin: '6px 0 0', color: 'var(--muted)' }}>
                Save coordinates with each address so delivery eligibility stays backend-verified through checkout.
              </p>
            </div>
            <button type="button" className="button primary sm" onClick={() => setShowForm((current) => !current)}>
              {showForm ? 'Close Form' : '+ Add Address'}
            </button>
          </div>

          {error && (
            <div style={{ padding: 12, marginBottom: 16, borderRadius: 'var(--radius-sm)', ...notificationStyles('danger') }}>
              <p style={{ margin: 0, fontSize: '0.9rem' }}>{error}</p>
            </div>
          )}
          {message && (
            <div style={{ padding: 12, marginBottom: 16, borderRadius: 'var(--radius-sm)', ...notificationStyles('success') }}>
              <p style={{ margin: 0, fontSize: '0.9rem' }}>{message}</p>
            </div>
          )}

          {showForm && (
            <div className="panel" style={{ padding: 24, marginBottom: 24 }}>
              <div style={{ display: 'grid', gap: 24 }}>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: 16 }}>
                  <div style={{ display: 'grid', gap: 12 }}>
                    <h3 style={{ margin: 0 }}>Location capture</h3>
                    <p style={{ margin: 0, color: 'var(--muted)', fontSize: '0.92rem' }}>
                      Use current location, search an address, or drag the pin. The backend resolves the address and delivery coverage.
                    </p>
                    <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
                      <button type="button" className="button primary sm" onClick={handleUseCurrentLocation} disabled={locationLoading}>
                        {locationLoading ? 'Locating...' : 'Use My Current Location'}
                      </button>
                      <button type="button" className="button ghost sm" onClick={handleManualCoordinateCheck} disabled={locationLoading}>
                        Validate Pin
                      </button>
                    </div>
                    <form onSubmit={handleLocationSearch} style={{ display: 'flex', gap: 8 }}>
                      <input
                        className="form-input"
                        placeholder="Search street, district, or landmark"
                        value={searchQuery}
                        onChange={(event) => setSearchQuery(event.target.value)}
                        style={{ flex: 1 }}
                      />
                      <button type="submit" className="button ghost sm" disabled={searchLoading}>
                        {searchLoading ? 'Searching...' : 'Search'}
                      </button>
                    </form>
                    {searchResults.length > 0 && (
                      <div style={{ display: 'grid', gap: 8 }}>
                        {searchResults.map((result) => (
                          <button
                            key={`${result.placeId || result.formattedAddress}-${result.latitude}-${result.longitude}`}
                            type="button"
                            className="button ghost"
                            style={{ justifyContent: 'flex-start', textAlign: 'left' }}
                            onClick={() => handleSearchSelection(result)}
                          >
                            {result.formattedAddress}
                          </button>
                        ))}
                      </div>
                    )}
                  </div>

                  <div>
                    <LocationPickerMap
                      latitude={coordinates.latitude}
                      longitude={coordinates.longitude}
                      nearbyStores={nearbyStores}
                      onCoordinateChange={({ latitude, longitude }) => {
                        setEligibility(null)
                        setNearbyStores([])
                        setForm((current) => ({
                          ...current,
                          latitude: String(latitude),
                          longitude: String(longitude)
                        }))
                      }}
                    />
                    <small style={{ display: 'block', marginTop: 8, color: 'var(--muted)' }}>
                      Click the map or drag the pin to refine the delivery point. Orange circles are nearby delivery-capable stores.
                    </small>
                  </div>
                </div>

                {eligibilityMeta && (
                  <div style={{ padding: 14, borderRadius: 'var(--radius-sm)', ...notificationStyles(eligibilityMeta.tone) }}>
                    <strong style={{ display: 'block', marginBottom: 4 }}>{eligibilityMeta.title}</strong>
                    <span style={{ fontSize: '0.9rem' }}>{eligibilityMeta.detail}</span>
                  </div>
                )}

                {nearbyStores.length > 0 && (
                  <div style={{ display: 'grid', gap: 10 }}>
                    <h4 style={{ margin: 0 }}>Nearby service points</h4>
                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: 12 }}>
                      {nearbyStores.map((store) => (
                        <div key={store.id} className="panel" style={{ padding: 16 }}>
                          <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8, alignItems: 'flex-start' }}>
                            <strong>{store.name}</strong>
                            <Badge variant={store.openNow ? 'success' : 'warning'}>{store.openNow ? 'Open' : 'Closed'}</Badge>
                          </div>
                          <p style={{ margin: '8px 0 4px', color: 'var(--muted)', fontSize: '0.9rem' }}>
                            {[store.addressLine1, store.city, store.state].filter(Boolean).join(', ')}
                          </p>
                          <small style={{ color: 'var(--muted)' }}>
                            {formatDistance(store.distanceMeters)}
                            {store.serviceRadiusMeters ? ` • radius ${formatDistance(store.serviceRadiusMeters)}` : ''}
                          </small>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                <form onSubmit={submit} style={{ display: 'grid', gap: 12 }}>
                  <h3 style={{ margin: 0 }}>Address details</h3>
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: 12 }}>
                    <input className="form-input" placeholder="Label (e.g. Home)" value={form.label} onChange={(event) => onField('label', event.target.value)} />
                    <input className="form-input" placeholder="Recipient Name *" required value={form.recipientName} onChange={(event) => onField('recipientName', event.target.value)} />
                    <input className="form-input" placeholder="Phone" value={form.phone} onChange={(event) => onField('phone', event.target.value)} />
                  </div>
                  <input className="form-input" placeholder="Address Line 1 *" required value={form.line1} onChange={(event) => onField('line1', event.target.value)} />
                  <input className="form-input" placeholder="Address Line 2" value={form.line2} onChange={(event) => onField('line2', event.target.value)} />
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: 12 }}>
                    <input className="form-input" placeholder="District" value={form.district} onChange={(event) => onField('district', event.target.value)} />
                    <input className="form-input" placeholder="City *" required value={form.city} onChange={(event) => onField('city', event.target.value)} />
                    <input className="form-input" placeholder="State/Province *" required value={form.stateProvince} onChange={(event) => onField('stateProvince', event.target.value)} />
                    <input className="form-input" placeholder="Postal Code *" required value={form.postalCode} onChange={(event) => onField('postalCode', event.target.value)} />
                    <input className="form-input" placeholder="Country *" required value={form.countryCode} onChange={(event) => onField('countryCode', event.target.value)} />
                  </div>
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 12 }}>
                    <input className="form-input" type="number" step="0.0000001" placeholder="Latitude" value={form.latitude} onChange={(event) => onField('latitude', event.target.value)} />
                    <input className="form-input" type="number" step="0.0000001" placeholder="Longitude" value={form.longitude} onChange={(event) => onField('longitude', event.target.value)} />
                    <input className="form-input" type="number" min="0" placeholder="Accuracy (m)" value={form.accuracyMeters} onChange={(event) => onField('accuracyMeters', event.target.value)} />
                  </div>
                  <input className="form-input" placeholder="Formatted address from geocoder" value={form.formattedAddress} onChange={(event) => onField('formattedAddress', event.target.value)} />
                  <input className="form-input" placeholder="Place ID" value={form.placeId} onChange={(event) => onField('placeId', event.target.value)} />
                  <textarea
                    className="form-input"
                    placeholder="Delivery instructions"
                    rows={3}
                    value={form.deliveryInstructions}
                    onChange={(event) => onField('deliveryInstructions', event.target.value)}
                  />
                  <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap' }}>
                    <label style={{ display: 'flex', alignItems: 'center', gap: 6, cursor: 'pointer' }}>
                      <input type="checkbox" checked={form.defaultShipping} onChange={(event) => onField('defaultShipping', event.target.checked)} /> Default Shipping
                    </label>
                    <label style={{ display: 'flex', alignItems: 'center', gap: 6, cursor: 'pointer' }}>
                      <input type="checkbox" checked={form.defaultBilling} onChange={(event) => onField('defaultBilling', event.target.checked)} /> Default Billing
                    </label>
                  </div>
                  <button type="submit" className="button primary" disabled={loading}>
                    {loading ? 'Saving...' : 'Save Address'}
                  </button>
                </form>
              </div>
            </div>
          )}

          {addresses.length === 0 ? (
            <div className="panel" style={{ padding: 40, textAlign: 'center' }}>
              <p style={{ color: 'var(--muted)', marginBottom: 12 }}>No delivery addresses saved yet.</p>
              <p style={{ color: 'var(--muted)', margin: 0 }}>Capture a location before checkout so service availability can be validated server-side.</p>
            </div>
          ) : (
            <div style={{ display: 'grid', gap: 12 }}>
              {addresses.map((address) => {
                const validationMeta = validationPresentation(address.validationStatus)
                return (
                  <div key={address.id} className="panel" style={{ padding: 20 }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', gap: 16, alignItems: 'flex-start', flexWrap: 'wrap' }}>
                      <div style={{ flex: 1 }}>
                        <div style={{ display: 'flex', gap: 8, alignItems: 'center', marginBottom: 8, flexWrap: 'wrap' }}>
                          <strong>{address.label || 'Address'}</strong>
                          {address.defaultShipping && <Badge variant="success">Default</Badge>}
                          <Badge variant={validationMeta.variant}>{validationMeta.label}</Badge>
                        </div>
                        <p style={{ margin: 0, color: 'var(--muted)' }}>
                          {address.formattedAddress || [
                            address.recipientName,
                            address.line1,
                            address.line2,
                            address.district,
                            address.city,
                            address.stateProvince,
                            address.postalCode,
                            address.countryCode
                          ].filter(Boolean).join(', ')}
                        </p>
                        {address.phone && <p style={{ margin: '6px 0 0', fontSize: '0.85rem', color: 'var(--muted)' }}>Phone: {address.phone}</p>}
                        {address.deliveryInstructions && (
                          <p style={{ margin: '6px 0 0', fontSize: '0.85rem', color: 'var(--muted)' }}>
                            Instructions: {address.deliveryInstructions}
                          </p>
                        )}
                        {(address.latitude != null && address.longitude != null) && (
                          <p style={{ margin: '6px 0 0', fontSize: '0.8rem', color: 'var(--muted)' }}>
                            Coordinates: {Number(address.latitude).toFixed(5)}, {Number(address.longitude).toFixed(5)}
                          </p>
                        )}
                      </div>
                      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                        {!address.defaultShipping && (
                          <button type="button" className="button ghost sm" onClick={() => markAsDefault(address.id)} disabled={loading}>
                            Set Default
                          </button>
                        )}
                        <button type="button" className="button ghost sm" onClick={() => removeAddress(address.id)} disabled={loading}>
                          Delete
                        </button>
                      </div>
                    </div>
                  </div>
                )
              })}
            </div>
          )}

          <div style={{ marginTop: 24 }}>
            <Link href="/auth" className="button ghost">← Back to Account</Link>
          </div>
        </div>
      </section>
    </>
  )
}
