export function SortableHeader({ label, field, sortBy, direction, onSort }) {
  const active = sortBy === field
  const arrow = active ? (direction === 'asc' ? '\u25B2' : '\u25BC') : '\u25BC'

  function handleClick() {
    if (active) {
      onSort(field, direction === 'asc' ? 'desc' : 'asc')
    } else {
      onSort(field, 'asc')
    }
  }

  return (
    <th className={`th-sort ${active ? 'active' : ''}`} onClick={handleClick}>
      {label}
      <span className="sort-indicator">{arrow}</span>
    </th>
  )
}
