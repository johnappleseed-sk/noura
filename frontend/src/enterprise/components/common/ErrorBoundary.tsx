import { Component, ErrorInfo, ReactNode } from 'react'

interface ErrorBoundaryProps {
  children: ReactNode
}

interface ErrorBoundaryState {
  hasError: boolean
}

export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  public state: ErrorBoundaryState = {
    hasError: false,
  }

  /**
   * Retrieves get derived state from error.
   *
   * @returns The result of get derived state from error.
   */
  public static getDerivedStateFromError(): ErrorBoundaryState {
    return { hasError: true }
  }

  /**
   * Executes component did catch.
   *
   * @param error The error value.
   * @param errorInfo The error info value.
   * @returns No value.
   */
  public componentDidCatch(error: Error, errorInfo: ErrorInfo): void {
    console.error('Unhandled UI error', error, errorInfo)
  }

  /**
   * Executes render.
   *
   * @returns The result of render.
   */
  public render(): ReactNode {
    if (this.state.hasError) {
      return (
        <div className="app-shell flex min-h-screen items-center justify-center p-6">
          <div className="panel max-w-md p-8 text-center">
            <h1 className="text-xl font-semibold">Something went wrong</h1>
            <p className="m3-subtitle mt-3 text-sm">
              The application encountered an unexpected issue. Refresh the page to continue.
            </p>
            <button
              className="m3-btn m3-btn-filled mt-6"
              onClick={() => window.location.reload()}
              type="button"
            >
              Reload
            </button>
          </div>
        </div>
      )
    }

    return this.props.children
  }
}
