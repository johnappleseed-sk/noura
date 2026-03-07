import { Component, type ErrorInfo, type ReactNode } from 'react';
import styles from './ErrorBoundary.module.css';

interface ErrorBoundaryProps {
  children: ReactNode;
}

interface ErrorBoundaryState {
  hasError: boolean;
}

export class ErrorBoundary extends Component<
  ErrorBoundaryProps,
  ErrorBoundaryState
> {
  public state: ErrorBoundaryState = {
    hasError: false,
  };

  /**
   * Retrieves get derived state from error.
   *
   * @returns The result of get derived state from error.
   */
  public static getDerivedStateFromError(): ErrorBoundaryState {
    return { hasError: true };
  }

  /**
   * Executes component did catch.
   *
   * @param error The error value.
   * @param errorInfo The error info value.
   * @returns The result of component did catch.
   */
  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('Unhandled rendering error:', error, errorInfo);
  }

  /**
   * Executes render.
   *
   * @returns The result of render.
   */
  public render() {
    if (this.state.hasError) {
      return (
        <section className={styles.fallback} role="alert">
          <h1>Something went wrong</h1>
          <p>
            We could not load this page. Refresh the browser or return to the
            homepage.
          </p>
        </section>
      );
    }

    return this.props.children;
  }
}
