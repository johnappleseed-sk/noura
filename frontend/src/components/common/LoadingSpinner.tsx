import styles from './LoadingSpinner.module.css';

interface LoadingSpinnerProps {
  label?: string;
}

/**
 * Creates a new LoadingSpinner instance.
 */
export function LoadingSpinner({
  label = 'Loading content...',
}: LoadingSpinnerProps) {
  return (
    <div className={styles.wrapper} role="status" aria-live="polite">
      <span className={styles.spinner} aria-hidden="true" />
      <p>{label}</p>
    </div>
  );
}
