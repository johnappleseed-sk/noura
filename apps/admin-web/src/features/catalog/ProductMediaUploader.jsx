import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  addCommerceMedia,
  deleteCommerceMedia,
  updateCommerceMedia
} from '../../shared/api/endpoints/commerceProductsApi'
import {
  importMediaAssetFromUrl,
  uploadMediaAsset
} from '../../shared/api/endpoints/mediaAssetsApi'
import { useConfirmDialog } from '../../shared/ui/ConfirmDialogProvider'

const MAX_FILE_BYTES = 8 * 1024 * 1024
const MAX_PARALLEL_UPLOADS = 3
const ACCEPTED_FORMATS = ['jpg', 'jpeg', 'png', 'webp', 'gif', 'svg']
const ENABLE_SVG = false
const IMAGE_URL_PATTERN = /^https?:\/\/[^\s]+$/i
const INTERNAL_HASH_PATTERN = /([a-f0-9]{64})\.(jpg|jpeg|png|webp|gif|svg)(?:\?.*)?$/i
const MIME_TO_EXTENSION = {
  'image/jpeg': 'jpg',
  'image/jpg': 'jpg',
  'image/png': 'png',
  'image/webp': 'webp',
  'image/gif': 'gif',
  'image/svg+xml': 'svg'
}

function createJobId() {
  return `job-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

function extOf(name = '') {
  const idx = name.lastIndexOf('.')
  return idx === -1 ? '' : name.slice(idx + 1).toLowerCase()
}

function inferFormatFromBytes(bytes) {
  if (bytes.length >= 3 && bytes[0] === 0xff && bytes[1] === 0xd8 && bytes[2] === 0xff) return 'jpg'
  if (
    bytes.length >= 8 &&
    bytes[0] === 0x89 &&
    bytes[1] === 0x50 &&
    bytes[2] === 0x4e &&
    bytes[3] === 0x47 &&
    bytes[4] === 0x0d &&
    bytes[5] === 0x0a &&
    bytes[6] === 0x1a &&
    bytes[7] === 0x0a
  ) return 'png'
  if (
    bytes.length >= 12 &&
    bytes[0] === 0x52 &&
    bytes[1] === 0x49 &&
    bytes[2] === 0x46 &&
    bytes[3] === 0x46 &&
    bytes[8] === 0x57 &&
    bytes[9] === 0x45 &&
    bytes[10] === 0x42 &&
    bytes[11] === 0x50
  ) return 'webp'
  if (
    bytes.length >= 6 &&
    bytes[0] === 0x47 &&
    bytes[1] === 0x49 &&
    bytes[2] === 0x46 &&
    bytes[3] === 0x38 &&
    (bytes[4] === 0x37 || bytes[4] === 0x39) &&
    bytes[5] === 0x61
  ) return 'gif'
  return null
}

function sanitizeSvg(svgText) {
  const lower = String(svgText || '').toLowerCase()
  if (!lower.includes('<svg')) {
    throw new Error('SVG payload is invalid.')
  }
  if (
    lower.includes('<script') ||
    lower.includes('javascript:') ||
    lower.includes('<iframe') ||
    lower.includes('<object') ||
    lower.includes('<embed') ||
    lower.includes('<foreignobject') ||
    lower.includes('onload=') ||
    lower.includes('onerror=') ||
    lower.includes('onclick=')
  ) {
    throw new Error('SVG contains unsafe content.')
  }
}

async function validateFile(rawFile) {
  if (!rawFile) {
    throw new Error('File is required.')
  }
  if (rawFile.size <= 0) {
    throw new Error('File is empty.')
  }
  if (rawFile.size > MAX_FILE_BYTES) {
    throw new Error(`"${rawFile.name}" exceeds ${Math.round(MAX_FILE_BYTES / (1024 * 1024))}MB.`)
  }
  const extension = extOf(rawFile.name)
  if (!ACCEPTED_FORMATS.includes(extension)) {
    throw new Error(`"${rawFile.name}" has an unsupported extension.`)
  }
  if (extension === 'svg') {
    if (!ENABLE_SVG) {
      throw new Error('SVG uploads are currently disabled.')
    }
    const svg = await rawFile.text()
    sanitizeSvg(svg)
    return
  }
  const header = new Uint8Array(await rawFile.slice(0, 16).arrayBuffer())
  const detected = inferFormatFromBytes(header)
  if (!detected) {
    throw new Error(`"${rawFile.name}" is not a valid image file.`)
  }
  if ((detected === 'jpg' && !['jpg', 'jpeg'].includes(extension)) || (detected !== 'jpg' && detected !== extension)) {
    throw new Error(`"${rawFile.name}" extension does not match file contents.`)
  }
}

async function optimizeImageFile(rawFile) {
  const fileExt = extOf(rawFile.name)
  if (['gif', 'svg', 'webp'].includes(fileExt)) {
    return rawFile
  }
  if (!rawFile.type.startsWith('image/')) {
    return rawFile
  }
  if (typeof window === 'undefined' || typeof document === 'undefined') {
    return rawFile
  }
  const bitmap = await createImageBitmap(rawFile)
  const maxDimension = 2000
  const scale = Math.min(1, maxDimension / Math.max(bitmap.width, bitmap.height))
  const width = Math.max(1, Math.round(bitmap.width * scale))
  const height = Math.max(1, Math.round(bitmap.height * scale))
  const canvas = document.createElement('canvas')
  canvas.width = width
  canvas.height = height
  const context = canvas.getContext('2d')
  if (!context) {
    return rawFile
  }
  context.drawImage(bitmap, 0, 0, width, height)
  const blob = await new Promise((resolve) => {
    canvas.toBlob(resolve, 'image/webp', 0.82)
  })
  bitmap.close?.()
  if (!blob || blob.size === 0) {
    return rawFile
  }
  if (blob.size >= rawFile.size * 0.98) {
    return rawFile
  }
  const base = rawFile.name.replace(/\.[a-z0-9]+$/i, '')
  return new File([blob], `${base}.webp`, { type: 'image/webp', lastModified: Date.now() })
}

async function fileHash(file) {
  const buffer = await file.arrayBuffer()
  if (window.crypto?.subtle) {
    const digest = await window.crypto.subtle.digest('SHA-256', buffer)
    const view = new Uint8Array(digest)
    return Array.from(view).map((v) => v.toString(16).padStart(2, '0')).join('')
  }
  return `${file.name}:${file.size}:${file.lastModified}`
}

function toSortedMedia(mediaItems = []) {
  return [...mediaItems].sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
}

function reorderMedia(items, sourceId, targetId) {
  const sourceIndex = items.findIndex((item) => String(item.id) === String(sourceId))
  const targetIndex = items.findIndex((item) => String(item.id) === String(targetId))
  if (sourceIndex < 0 || targetIndex < 0 || sourceIndex === targetIndex) {
    return items
  }
  const copy = [...items]
  const [moved] = copy.splice(sourceIndex, 1)
  copy.splice(targetIndex, 0, moved)
  return copy
}

function hashFromUrl(url) {
  const match = String(url || '').match(INTERNAL_HASH_PATTERN)
  return match?.[1]?.toLowerCase() || null
}

function normalizeUrl(url) {
  const value = String(url || '').trim()
  if (!IMAGE_URL_PATTERN.test(value)) {
    throw new Error('Please provide a valid http/https image URL.')
  }
  return value
}

function normalizeComparableUrl(url) {
  return String(url || '').trim()
}

function isAbortError(error) {
  if (!error) return false
  const message = String(error?.message || '').toLowerCase()
  return error?.name === 'AbortError' || error?.name === 'CanceledError' || error?.code === 'ERR_CANCELED' || message.includes('aborted') || message.includes('canceled')
}

function isInternalAsset(url) {
  return /\/uploads\//i.test(String(url || ''))
}

function normalizeIncomingFile(file, source = 'file') {
  const name = String(file?.name || '').trim()
  if (extOf(name)) {
    return file
  }
  const mime = String(file?.type || '').toLowerCase()
  const mappedExtension = MIME_TO_EXTENSION[mime]
  if (!mappedExtension || !ACCEPTED_FORMATS.includes(mappedExtension)) {
    return file
  }
  const base = source === 'clipboard' ? 'clipboard-image' : 'image'
  return new File([file], `${base}-${Date.now()}.${mappedExtension}`, {
    type: file.type || `image/${mappedExtension}`,
    lastModified: file.lastModified || Date.now()
  })
}

function getErrorMessage(error, fallbackMessage) {
  return error?.response?.data?.message || error?.message || fallbackMessage
}

export function ProductMediaUploader({
  productId,
  mediaItems,
  disabled,
  onMediaChanged,
  onFlash,
  onError
}) {
  const confirm = useConfirmDialog()
  const fileInputRef = useRef(null)
  const replaceInputRef = useRef(null)
  const activeUploadsRef = useRef(0)
  const queuedUploadsRef = useRef([])
  const pendingHashesRef = useRef(new Set())
  const mediaCountRef = useRef(0)
  const hasPrimaryRef = useRef(false)
  const sortCounterRef = useRef(0)
  const jobsRef = useRef([])
  const replaceTargetRef = useRef(null)
  const dragMediaIdRef = useRef(null)
  const uploadControllersRef = useRef(new Map())
  const sortedMediaRef = useRef([])

  const [dragActive, setDragActive] = useState(false)
  const [draggingMediaId, setDraggingMediaId] = useState(null)
  const [jobs, setJobs] = useState([])
  const [urlInput, setUrlInput] = useState('')
  const [urlError, setUrlError] = useState('')
  const [actionBusy, setActionBusy] = useState(false)
  const [liveMessage, setLiveMessage] = useState('')

  const sortedMedia = useMemo(() => toSortedMedia(mediaItems), [mediaItems])
  const knownHashes = useMemo(() => {
    const hashes = new Set()
    sortedMedia.forEach((item) => {
      const hash = hashFromUrl(item.url)
      if (hash) hashes.add(hash)
    })
    return hashes
  }, [sortedMedia])
  const knownUrls = useMemo(() => {
    const urls = new Set()
    sortedMedia.forEach((item) => {
      const normalized = normalizeComparableUrl(item.url)
      if (normalized) urls.add(normalized)
    })
    return urls
  }, [sortedMedia])

  useEffect(() => {
    mediaCountRef.current = sortedMedia.length
    hasPrimaryRef.current = sortedMedia.some((item) => Boolean(item.primary))
    const maxSort = sortedMedia.reduce((max, item) => Math.max(max, Number(item.sortOrder) || 0), -1)
    sortCounterRef.current = Math.max(sortCounterRef.current, maxSort + 1)
    sortedMediaRef.current = sortedMedia
  }, [sortedMedia])

  useEffect(() => {
    jobsRef.current = jobs
  }, [jobs])

  useEffect(() => {
    return () => {
      uploadControllersRef.current.forEach((controller) => controller.abort())
      uploadControllersRef.current.clear()
      queuedUploadsRef.current = []
      pendingHashesRef.current.clear()
      jobsRef.current.forEach((job) => {
        if (job.previewUrl) URL.revokeObjectURL(job.previewUrl)
      })
    }
  }, [])

  const updateJob = useCallback((jobId, patch) => {
    setJobs((current) => current.map((job) => (job.id === jobId ? { ...job, ...patch } : job)))
  }, [])

  const removeJob = useCallback((jobId) => {
    setJobs((current) => {
      const target = current.find((job) => job.id === jobId)
      if (target?.previewUrl) {
        URL.revokeObjectURL(target.previewUrl)
      }
      return current.filter((job) => job.id !== jobId)
    })
  }, [])

  const attachMediaUrl = useCallback(async (url, { primaryOverride = null } = {}) => {
    const normalizedUrl = normalizeComparableUrl(url)
    if (!normalizedUrl) {
      throw new Error('Media URL is required.')
    }
    const duplicateInProduct = sortedMediaRef.current.some(
      (item) => normalizeComparableUrl(item.url) === normalizedUrl
    )
    if (duplicateInProduct) {
      return { attached: false, duplicateInProduct: true }
    }
    const nextSort = sortCounterRef.current
    sortCounterRef.current += 1
    const makePrimary = primaryOverride ?? (!hasPrimaryRef.current && mediaCountRef.current === 0)
    await addCommerceMedia(productId, {
      mediaType: 'IMAGE',
      url: normalizedUrl,
      sortOrder: nextSort,
      isPrimary: Boolean(makePrimary)
    })
    mediaCountRef.current += 1
    if (makePrimary) {
      hasPrimaryRef.current = true
    }
    return { attached: true, duplicateInProduct: false }
  }, [productId])

  const processUploadQueue = useCallback(() => {
    while (activeUploadsRef.current < MAX_PARALLEL_UPLOADS && queuedUploadsRef.current.length) {
      const task = queuedUploadsRef.current.shift()
      activeUploadsRef.current += 1
      updateJob(task.id, { status: 'uploading', progress: 5 })
      const controller = new AbortController()
      uploadControllersRef.current.set(task.id, controller)
      uploadMediaAsset(task.file, {
        onUploadProgress: (event) => {
          const total = Number(event.total) || task.file.size || 1
          const progress = Math.min(98, Math.round((Number(event.loaded || 0) / total) * 100))
          updateJob(task.id, { progress })
        },
        signal: controller.signal
      })
        .then(async (asset) => {
          const attachResult = await attachMediaUrl(asset.url)
          updateJob(task.id, {
            status: 'completed',
            progress: 100,
            message: attachResult.attached
              ? asset.duplicate
                ? 'Duplicate detected and reused from storage.'
                : 'Uploaded successfully.'
              : 'Upload finished, but image already exists on this product.'
          })
          if (attachResult.attached) {
            setLiveMessage(`${task.file.name} uploaded successfully.`)
            onFlash?.(asset.duplicate ? `Duplicate image reused: ${task.file.name}` : `Uploaded ${task.file.name}`)
            await onMediaChanged?.()
          } else {
            setLiveMessage(`Skipped duplicate image: ${task.file.name}.`)
            onFlash?.(`Skipped duplicate image: ${task.file.name}`)
          }
          window.setTimeout(() => removeJob(task.id), 3500)
        })
        .catch((err) => {
          if (isAbortError(err)) {
            updateJob(task.id, {
              status: 'canceled',
              progress: 0,
              message: 'Upload canceled.'
            })
            setLiveMessage(`Upload canceled for ${task.file.name}.`)
            return
          }
          updateJob(task.id, {
            status: 'error',
            progress: 0,
            message: getErrorMessage(err, 'Upload failed.')
          })
          onError?.(getErrorMessage(err, `Failed to upload ${task.file.name}.`))
          setLiveMessage(`Upload failed for ${task.file.name}.`)
        })
        .finally(() => {
          uploadControllersRef.current.delete(task.id)
          activeUploadsRef.current -= 1
          pendingHashesRef.current.delete(task.hash)
          processUploadQueue()
        })
    }
  }, [attachMediaUrl, onError, onFlash, onMediaChanged, removeJob, updateJob])

  const queueFiles = useCallback(async (files, source = 'file') => {
    const incoming = Array.from(files || [])
    if (!incoming.length) return

    for (const sourceFile of incoming) {
      const id = createJobId()
      const rawFile = normalizeIncomingFile(sourceFile, source)
      try {
        await validateFile(rawFile)
        const optimized = await optimizeImageFile(rawFile)
        const hash = await fileHash(optimized)
        if (knownHashes.has(hash) || pendingHashesRef.current.has(hash)) {
          setJobs((current) => [
            {
              id,
              fileName: rawFile.name,
              source,
              status: 'error',
              progress: 0,
              message: 'Duplicate image detected. This file already exists.'
            },
            ...current
          ])
          continue
        }
        pendingHashesRef.current.add(hash)
        const previewUrl = URL.createObjectURL(optimized)
        setJobs((current) => [
          {
            id,
            fileName: optimized.name,
            source,
            status: 'queued',
            progress: 0,
            previewUrl,
            message: 'Queued for upload.',
            file: optimized,
            hash
          },
          ...current
        ])
        queuedUploadsRef.current.push({ id, file: optimized, hash })
      } catch (err) {
        setJobs((current) => [
          {
            id,
            fileName: rawFile.name,
            source,
            status: 'error',
            progress: 0,
            message: getErrorMessage(err, 'File validation failed.')
          },
          ...current
        ])
      }
    }
    processUploadQueue()
  }, [knownHashes, processUploadQueue])

  const retryJob = useCallback((jobId) => {
    const job = jobsRef.current.find((entry) => entry.id === jobId)
    if (!job?.file || !job?.hash) {
      return
    }
    if (knownHashes.has(job.hash) || pendingHashesRef.current.has(job.hash)) {
      updateJob(jobId, {
        status: 'error',
        progress: 0,
        message: 'Duplicate image detected. This file already exists.'
      })
      return
    }
    pendingHashesRef.current.add(job.hash)
    queuedUploadsRef.current.push({ id: job.id, file: job.file, hash: job.hash })
    updateJob(jobId, {
      status: 'queued',
      progress: 0,
      message: 'Queued for retry.'
    })
    processUploadQueue()
  }, [knownHashes, processUploadQueue, updateJob])

  const cancelUploadJob = useCallback((jobId) => {
    const queuedIndex = queuedUploadsRef.current.findIndex((task) => task.id === jobId)
    if (queuedIndex >= 0) {
      const [queuedTask] = queuedUploadsRef.current.splice(queuedIndex, 1)
      pendingHashesRef.current.delete(queuedTask.hash)
      updateJob(jobId, {
        status: 'canceled',
        progress: 0,
        message: 'Upload canceled before starting.'
      })
      setLiveMessage('Queued upload canceled.')
      return
    }
    const controller = uploadControllersRef.current.get(jobId)
    if (controller) {
      controller.abort()
      return
    }
    removeJob(jobId)
  }, [removeJob, updateJob])

  const cancelAllUploads = useCallback(() => {
    if (!queuedUploadsRef.current.length && !uploadControllersRef.current.size) {
      return
    }
    queuedUploadsRef.current.forEach((task) => pendingHashesRef.current.delete(task.hash))
    queuedUploadsRef.current = []
    uploadControllersRef.current.forEach((controller) => controller.abort())
    setJobs((current) =>
      current.map((job) =>
        ['queued', 'uploading'].includes(job.status)
          ? { ...job, status: 'canceled', progress: 0, message: 'Canceled by user.' }
          : job
      )
    )
    setLiveMessage('All active uploads were canceled.')
  }, [])

  const clearFinishedJobs = useCallback(() => {
    setJobs((current) => {
      current.forEach((job) => {
        if (!['queued', 'uploading'].includes(job.status) && job.previewUrl) {
          URL.revokeObjectURL(job.previewUrl)
        }
      })
      return current.filter((job) => ['queued', 'uploading'].includes(job.status))
    })
  }, [])

  const handleDrop = useCallback((event) => {
    event.preventDefault()
    event.stopPropagation()
    setDragActive(false)
    if (disabled) return
    const dropped = Array.from(event.dataTransfer?.files || [])
    queueFiles(dropped, 'drop')
  }, [disabled, queueFiles])

  const handlePasteFiles = useCallback((clipboardData) => {
    if (!clipboardData) return false
    const files = []
    if (clipboardData.items) {
      for (const item of clipboardData.items) {
        if (item.kind === 'file' && item.type.startsWith('image/')) {
          const file = item.getAsFile()
          if (file) files.push(file)
        }
      }
    }
    if (!files.length && clipboardData.files) {
      for (const file of clipboardData.files) {
        if (file?.type?.startsWith?.('image/')) {
          files.push(file)
        }
      }
    }
    if (!files.length) return false
    queueFiles(files, 'clipboard')
    return true
  }, [queueFiles])

  useEffect(() => {
    const handler = (event) => {
      if (disabled) return
      const consumed = handlePasteFiles(event.clipboardData)
      if (consumed) {
        event.preventDefault()
        setLiveMessage('Pasted image added to upload queue.')
      }
    }
    window.addEventListener('paste', handler)
    return () => window.removeEventListener('paste', handler)
  }, [disabled, handlePasteFiles])

  const onBrowseClick = useCallback(() => {
    if (disabled) return
    fileInputRef.current?.click()
  }, [disabled])

  const onDropzoneKeyDown = useCallback((event) => {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault()
      onBrowseClick()
    }
  }, [onBrowseClick])

  const handleExternalUrlAttach = useCallback(async () => {
    if (disabled || !productId) return
    setUrlError('')
    let normalized
    try {
      normalized = normalizeUrl(urlInput)
    } catch (err) {
      setUrlError(err.message)
      return
    }
    if (knownUrls.has(normalized)) {
      setUrlError('This URL is already attached to the product.')
      return
    }
    setActionBusy(true)
    try {
      const result = await attachMediaUrl(normalized)
      if (result.attached) {
        setUrlInput('')
        setLiveMessage('External image URL attached.')
        onFlash?.('External image URL attached.')
        await onMediaChanged?.()
      } else {
        setUrlError('This URL is already attached to the product.')
      }
    } catch (err) {
      onError?.(getErrorMessage(err, 'Unable to attach external image URL.'))
    } finally {
      setActionBusy(false)
    }
  }, [attachMediaUrl, disabled, knownUrls, onError, onFlash, onMediaChanged, productId, urlInput])

  const handleImportUrl = useCallback(async () => {
    if (disabled || !productId) return
    setUrlError('')
    let normalized
    try {
      normalized = normalizeUrl(urlInput)
    } catch (err) {
      setUrlError(err.message)
      return
    }
    setActionBusy(true)
    try {
      const asset = await importMediaAssetFromUrl(normalized)
      const result = await attachMediaUrl(asset.url)
      if (result.attached) {
        setUrlInput('')
        setLiveMessage('Image URL imported to internal storage.')
        onFlash?.(asset.duplicate ? 'Duplicate asset reused from storage.' : 'Image imported to internal storage.')
        await onMediaChanged?.()
      } else {
        setUrlError('Imported image is already attached to this product.')
      }
    } catch (err) {
      onError?.(getErrorMessage(err, 'Unable to import image URL.'))
    } finally {
      setActionBusy(false)
    }
  }, [attachMediaUrl, disabled, onError, onFlash, onMediaChanged, productId, urlInput])

  const persistMediaOrder = useCallback(async (nextOrder) => {
    if (!productId || !nextOrder.length) return
    setActionBusy(true)
    try {
      await Promise.all(
        nextOrder.map((item, index) =>
          updateCommerceMedia(productId, item.id, {
            mediaType: item.mediaType || 'IMAGE',
            url: item.url,
            sortOrder: index,
            isPrimary: Boolean(item.primary)
          })
        )
      )
      onFlash?.('Media order updated.')
      await onMediaChanged?.()
    } catch (err) {
      onError?.(getErrorMessage(err, 'Unable to reorder media.'))
    } finally {
      setActionBusy(false)
    }
  }, [onError, onFlash, onMediaChanged, productId])

  const moveMediaByKeyboard = useCallback((index, direction) => {
    const targetIndex = index + direction
    if (targetIndex < 0 || targetIndex >= sortedMedia.length) return
    const next = [...sortedMedia]
    const [moved] = next.splice(index, 1)
    next.splice(targetIndex, 0, moved)
    persistMediaOrder(next)
  }, [persistMediaOrder, sortedMedia])

  const handleRemove = useCallback(async (item) => {
    if (!productId || disabled) return
    const confirmed = await confirm({
      title: 'Move media to trash?',
      message: 'This image will be removed from the active product gallery.',
      description: item?.url || '',
      confirmLabel: 'Move to trash',
      tone: 'danger'
    })
    if (!confirmed) return
    setActionBusy(true)
    try {
      await deleteCommerceMedia(productId, item.id)
      onFlash?.('Image moved to trash.')
      await onMediaChanged?.()
    } catch (err) {
      onError?.(getErrorMessage(err, 'Unable to move image to trash.'))
    } finally {
      setActionBusy(false)
    }
  }, [confirm, disabled, onError, onFlash, onMediaChanged, productId])

  const handleReplaceTrigger = useCallback((item) => {
    replaceTargetRef.current = item
    replaceInputRef.current?.click()
  }, [])

  const handleReplaceChange = useCallback(async (event) => {
    const file = event.target.files?.[0]
    const target = replaceTargetRef.current
    event.target.value = ''
    if (!file || !target || disabled || !productId) return
    setActionBusy(true)
    try {
      await validateFile(file)
      const optimized = await optimizeImageFile(file)
      const asset = await uploadMediaAsset(optimized, {
        onUploadProgress: ({ loaded, total }) => {
          const safeTotal = Number(total) || optimized.size || 1
          const progress = Math.min(100, Math.round((Number(loaded || 0) / safeTotal) * 100))
          setLiveMessage(`Replacing ${target.url} (${progress}%)`)
        }
      })
      await updateCommerceMedia(productId, target.id, {
        mediaType: target.mediaType || 'IMAGE',
        url: asset.url,
        sortOrder: target.sortOrder ?? 0,
        isPrimary: Boolean(target.primary)
      })
      onFlash?.('Image replaced.')
      await onMediaChanged?.()
    } catch (err) {
      onError?.(getErrorMessage(err, 'Unable to replace image.'))
    } finally {
      setActionBusy(false)
      replaceTargetRef.current = null
    }
  }, [disabled, onError, onFlash, onMediaChanged, productId])

  const handleSetPrimary = useCallback(async (item) => {
    if (!productId || disabled || item.primary) return
    setActionBusy(true)
    try {
      await updateCommerceMedia(productId, item.id, {
        mediaType: item.mediaType || 'IMAGE',
        url: item.url,
        sortOrder: item.sortOrder ?? 0,
        isPrimary: true
      })
      onFlash?.('Primary image updated.')
      await onMediaChanged?.()
    } catch (err) {
      onError?.(getErrorMessage(err, 'Unable to update primary image.'))
    } finally {
      setActionBusy(false)
    }
  }, [disabled, onError, onFlash, onMediaChanged, productId])

  const jobStats = useMemo(() => {
    const counts = { queued: 0, uploading: 0, completed: 0, error: 0, canceled: 0 }
    jobs.forEach((job) => {
      if (counts[job.status] != null) {
        counts[job.status] += 1
      }
    })
    return counts
  }, [jobs])

  const hasActiveJobs = jobStats.queued > 0 || jobStats.uploading > 0
  const hasFinishedJobs = jobs.some((job) => !['queued', 'uploading'].includes(job.status))
  const dropzoneDisabled = disabled || actionBusy

  return (
    <section className="media-uploader">
      <p className="visually-hidden" aria-live="polite">{liveMessage}</p>
      <input
        ref={fileInputRef}
        type="file"
        accept="image/jpeg,image/jpg,image/png,image/webp,image/gif,image/svg+xml"
        multiple
        className="visually-hidden"
        onChange={(event) => {
          queueFiles(event.target.files, 'browse')
          event.target.value = ''
        }}
        disabled={dropzoneDisabled}
      />
      <input
        ref={replaceInputRef}
        type="file"
        accept="image/jpeg,image/jpg,image/png,image/webp,image/gif,image/svg+xml"
        className="visually-hidden"
        onChange={handleReplaceChange}
        disabled={dropzoneDisabled}
      />

      <div
        className={`media-dropzone ${dragActive ? 'is-drag-active' : ''} ${dropzoneDisabled ? 'is-disabled' : ''}`}
        role="button"
        tabIndex={dropzoneDisabled ? -1 : 0}
        aria-disabled={dropzoneDisabled ? 'true' : 'false'}
        aria-label="Upload product images by dropping files, browsing files, or pasting from clipboard"
        onClick={onBrowseClick}
        onKeyDown={onDropzoneKeyDown}
        onDragEnter={(event) => {
          event.preventDefault()
          event.stopPropagation()
          if (!dropzoneDisabled) setDragActive(true)
        }}
        onDragOver={(event) => {
          event.preventDefault()
          event.stopPropagation()
          if (!dropzoneDisabled) setDragActive(true)
        }}
        onDragLeave={(event) => {
          event.preventDefault()
          event.stopPropagation()
          if (event.currentTarget.contains(event.relatedTarget)) return
          setDragActive(false)
        }}
        onDrop={handleDrop}
        onPaste={(event) => {
          if (dropzoneDisabled) return
          const consumed = handlePasteFiles(event.clipboardData)
          if (consumed) {
            event.preventDefault()
            setLiveMessage('Pasted image added to upload queue.')
          }
        }}
      >
        <h4>Product image uploader</h4>
        <p>Drag & drop, click to browse, or paste screenshots (Ctrl/Cmd + V).</p>
        <small>Accepted: {ACCEPTED_FORMATS.join(', ')} • Max {Math.round(MAX_FILE_BYTES / (1024 * 1024))}MB • Auto-optimize to WebP when possible</small>
      </div>

      <div className="media-url-import">
        <label htmlFor="media-url-input">Image URL</label>
        <input
          id="media-url-input"
          value={urlInput}
          onChange={(event) => setUrlInput(event.target.value)}
          placeholder="https://cdn.example.com/image.webp"
          disabled={dropzoneDisabled}
        />
        <div className="media-url-actions">
          <button
            type="button"
            className="btn btn-outline btn-sm"
            onClick={handleExternalUrlAttach}
            disabled={dropzoneDisabled || !urlInput.trim()}
          >
            Attach external URL
          </button>
          <button
            type="button"
            className="btn btn-outline btn-sm"
            onClick={handleImportUrl}
            disabled={dropzoneDisabled || !urlInput.trim()}
          >
            Import to internal storage
          </button>
        </div>
        {urlError ? <p className="media-inline-error" role="alert">{urlError}</p> : null}
        {urlInput && IMAGE_URL_PATTERN.test(urlInput.trim()) ? (
          <div className="media-url-preview">
            <img src={urlInput.trim()} alt="URL preview" loading="lazy" />
          </div>
        ) : null}
      </div>

      {jobs.length ? (
        <div className="media-upload-jobs" aria-live="polite">
          <div className="media-job-toolbar">
            <p>
              Queue: {jobStats.queued} • Uploading: {jobStats.uploading} • Completed: {jobStats.completed} • Errors: {jobStats.error} • Canceled: {jobStats.canceled}
            </p>
            <div className="media-job-toolbar-actions">
              <button
                type="button"
                className="btn btn-outline btn-sm"
                onClick={cancelAllUploads}
                disabled={!hasActiveJobs}
              >
                Cancel all
              </button>
              <button
                type="button"
                className="btn btn-outline btn-sm"
                onClick={clearFinishedJobs}
                disabled={!hasFinishedJobs}
              >
                Clear finished
              </button>
            </div>
          </div>
          {jobs.map((job) => (
            <article key={job.id} className={`media-upload-job status-${job.status}`}>
              {job.previewUrl ? <img src={job.previewUrl} alt="" loading="lazy" /> : <div className="media-upload-job-placeholder">img</div>}
              <div className="media-upload-job-content">
                <strong>{job.fileName}</strong>
                <span className="media-upload-job-source">{job.source}</span>
                <div className="media-progress" role="progressbar" aria-valuemin="0" aria-valuemax="100" aria-valuenow={job.progress}>
                  <div style={{ width: `${job.progress}%` }} />
                </div>
                <small className={job.status === 'error' ? 'text-error' : ''}>{job.message}</small>
              </div>
              <div className="media-upload-job-actions">
                {['queued', 'uploading'].includes(job.status) ? (
                  <button type="button" className="btn btn-outline btn-sm" onClick={() => cancelUploadJob(job.id)}>
                    Cancel
                  </button>
                ) : null}
                {['error', 'canceled'].includes(job.status) ? (
                  <button
                    type="button"
                    className="btn btn-outline btn-sm"
                    onClick={() => retryJob(job.id)}
                    disabled={!job.file || !job.hash}
                  >
                    Retry
                  </button>
                ) : null}
                <button type="button" className="btn btn-outline btn-sm" onClick={() => removeJob(job.id)}>
                  Dismiss
                </button>
              </div>
            </article>
          ))}
        </div>
      ) : null}

      {sortedMedia.length ? (
        <div className="media-grid">
          {sortedMedia.map((item, index) => (
            <article
              key={item.id}
              className={`media-card ${draggingMediaId && String(draggingMediaId) === String(item.id) ? 'is-dragged' : ''}`}
              draggable={!dropzoneDisabled}
              onDragStart={() => {
                dragMediaIdRef.current = item.id
                setDraggingMediaId(item.id)
              }}
              onDragEnd={() => {
                dragMediaIdRef.current = null
                setDraggingMediaId(null)
              }}
              onDragOver={(event) => {
                event.preventDefault()
              }}
              onDrop={(event) => {
                event.preventDefault()
                const sourceId = dragMediaIdRef.current
                if (!sourceId || String(sourceId) === String(item.id)) return
                const next = reorderMedia(sortedMedia, sourceId, item.id)
                setDraggingMediaId(null)
                dragMediaIdRef.current = null
                persistMediaOrder(next)
              }}
            >
              <img src={item.url} alt={`Product media ${index + 1}`} loading="lazy" />
              <div className="media-card-meta">
                <div className="media-card-badges">
                  {item.primary ? <span className="badge badge-success">Primary</span> : null}
                  <span className={`badge ${isInternalAsset(item.url) ? 'badge-info' : 'badge-muted'}`}>
                    {isInternalAsset(item.url) ? 'Internal' : 'External'}
                  </span>
                </div>
                <p className="mono" title={item.url}>{item.url}</p>
              </div>
              <div className="media-card-actions">
                <button
                  type="button"
                  className="btn btn-outline btn-sm"
                  disabled={dropzoneDisabled || index === 0}
                  onClick={() => moveMediaByKeyboard(index, -1)}
                >
                  Move up
                </button>
                <button
                  type="button"
                  className="btn btn-outline btn-sm"
                  disabled={dropzoneDisabled || index === sortedMedia.length - 1}
                  onClick={() => moveMediaByKeyboard(index, 1)}
                >
                  Move down
                </button>
                <button
                  type="button"
                  className="btn btn-outline btn-sm"
                  disabled={dropzoneDisabled || item.primary}
                  onClick={() => handleSetPrimary(item)}
                >
                  Set primary
                </button>
                <button type="button" className="btn btn-outline btn-sm" disabled={dropzoneDisabled} onClick={() => handleReplaceTrigger(item)}>
                  Replace
                </button>
                <button type="button" className="btn btn-outline btn-sm btn-danger" disabled={dropzoneDisabled} onClick={() => handleRemove(item)}>
                  Move to trash
                </button>
              </div>
            </article>
          ))}
        </div>
      ) : (
        <p className="empty-copy">No media attached.</p>
      )}
    </section>
  )
}
