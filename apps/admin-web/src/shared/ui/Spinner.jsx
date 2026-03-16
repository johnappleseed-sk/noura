export function Spinner({ label = 'Loading...' }) {
  return (
    <div className="spinner-shell" role="status" aria-live="polite">
      <div className="spinner" />
      <p>{label}</p>
    </div>
  )
}
