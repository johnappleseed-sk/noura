'use client'

import { useEffect, useRef } from 'react'

const DEFAULT_CENTER = [39.8283, -98.5795]

function buildPinMarkup(color) {
  return `
    <div style="position:relative;width:22px;height:22px;">
      <span style="position:absolute;inset:0;border-radius:999px;background:${color};border:2px solid #ffffff;box-shadow:0 10px 18px rgba(0,0,0,0.22);"></span>
      <span style="position:absolute;left:7px;top:7px;width:8px;height:8px;border-radius:999px;background:#ffffff;"></span>
    </div>
  `
}

export default function LocationPickerMap({
  latitude,
  longitude,
  nearbyStores = [],
  interactive = true,
  onCoordinateChange
}) {
  const containerRef = useRef(null)
  const mapRef = useRef(null)
  const leafletRef = useRef(null)
  const markerRef = useRef(null)
  const nearbyLayerRef = useRef([])

  useEffect(() => {
    let cancelled = false

    async function initialize() {
      if (cancelled || mapRef.current || !containerRef.current) {
        return
      }

      const leafletModule = await import('leaflet')
      const L = leafletModule.default || leafletModule
      leafletRef.current = L

      const map = L.map(containerRef.current, {
        zoomControl: true,
        scrollWheelZoom: interactive
      })
      mapRef.current = map

      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; OpenStreetMap contributors'
      }).addTo(map)

      if (interactive) {
        map.on('click', (event) => {
          onCoordinateChange?.({
            latitude: Number(event.latlng.lat.toFixed(7)),
            longitude: Number(event.latlng.lng.toFixed(7))
          })
        })
      }

      map.setView(
        latitude != null && longitude != null ? [latitude, longitude] : DEFAULT_CENTER,
        latitude != null && longitude != null ? 15 : 12
      )

      window.setTimeout(() => {
        map.invalidateSize()
      }, 0)
    }

    initialize()

    return () => {
      cancelled = true
      if (mapRef.current) {
        mapRef.current.remove()
        mapRef.current = null
      }
      markerRef.current = null
      nearbyLayerRef.current = []
      leafletRef.current = null
    }
  }, [interactive, latitude, longitude, onCoordinateChange])

  useEffect(() => {
    const map = mapRef.current
    const L = leafletRef.current
    if (!map || !L) {
      return
    }

    if (latitude == null || longitude == null) {
      if (markerRef.current) {
        markerRef.current.remove()
        markerRef.current = null
      }
      map.setView(DEFAULT_CENTER, 12)
      return
    }

    const coords = [latitude, longitude]
    if (!markerRef.current) {
      markerRef.current = L.marker(coords, {
        draggable: interactive,
        icon: L.divIcon({
          className: 'location-picker-pin',
          html: buildPinMarkup('#0b6d5f'),
          iconSize: [22, 22],
          iconAnchor: [11, 11]
        })
      }).addTo(map)

      if (interactive) {
        markerRef.current.on('dragend', (event) => {
          const point = event.target.getLatLng()
          onCoordinateChange?.({
            latitude: Number(point.lat.toFixed(7)),
            longitude: Number(point.lng.toFixed(7))
          })
        })
      }
    } else {
      markerRef.current.setLatLng(coords)
    }

    map.setView(coords, Math.max(map.getZoom(), 15))
  }, [interactive, latitude, longitude, onCoordinateChange])

  useEffect(() => {
    const map = mapRef.current
    const L = leafletRef.current
    if (!map || !L) {
      return
    }

    nearbyLayerRef.current.forEach((layer) => layer.remove())
    nearbyLayerRef.current = []

    for (const store of Array.isArray(nearbyStores) ? nearbyStores : []) {
      if (store?.latitude == null || store?.longitude == null) {
        continue
      }

      const marker = L.circleMarker([store.latitude, store.longitude], {
        radius: 7,
        color: '#c1672c',
        weight: 2,
        fillColor: '#f4a261',
        fillOpacity: 0.85
      })
        .bindPopup(
          `<strong>${store.name || 'Store'}</strong><br/>${[store.addressLine1, store.city].filter(Boolean).join(', ')}`
        )
        .addTo(map)

      nearbyLayerRef.current.push(marker)
    }
  }, [nearbyStores])

  return (
    <div
      ref={containerRef}
      style={{
        width: '100%',
        height: 320,
        borderRadius: 'var(--radius-md)',
        border: '1px solid var(--line)',
        overflow: 'hidden',
        background: 'linear-gradient(180deg, rgba(240,246,245,0.9), rgba(246,238,230,0.9))'
      }}
    />
  )
}
