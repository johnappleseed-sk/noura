import { useEffect, useRef } from 'react'
import { useToast } from './ToastProvider'

export function useToastFeedback({
  successMessage = '',
  errorMessage = '',
  warningMessage = '',
  infoMessage = ''
}) {
  const toast = useToast()
  const lastSuccessRef = useRef('')
  const lastErrorRef = useRef('')
  const lastWarningRef = useRef('')
  const lastInfoRef = useRef('')

  useEffect(() => {
    if (!successMessage) {
      lastSuccessRef.current = ''
      return
    }
    if (lastSuccessRef.current === successMessage) {
      return
    }
    lastSuccessRef.current = successMessage
    toast.success(successMessage)
  }, [successMessage, toast])

  useEffect(() => {
    if (!errorMessage) {
      lastErrorRef.current = ''
      return
    }
    if (lastErrorRef.current === errorMessage) {
      return
    }
    lastErrorRef.current = errorMessage
    toast.error(errorMessage)
  }, [errorMessage, toast])

  useEffect(() => {
    if (!warningMessage) {
      lastWarningRef.current = ''
      return
    }
    if (lastWarningRef.current === warningMessage) {
      return
    }
    lastWarningRef.current = warningMessage
    toast.warning(warningMessage)
  }, [warningMessage, toast])

  useEffect(() => {
    if (!infoMessage) {
      lastInfoRef.current = ''
      return
    }
    if (lastInfoRef.current === infoMessage) {
      return
    }
    lastInfoRef.current = infoMessage
    toast.info(infoMessage)
  }, [infoMessage, toast])
}
