export function PaginationControls({
  page = 0,
  totalPages = 1,
  totalElements = 0,
  first = true,
  last = true,
  pageSize = null,
  pageSizeOptions = [10, 20, 50, 100],
  onPageSizeChange = null,
  onPrev = null,
  onNext = null,
  noun = 'items'
}) {
  const safeTotalPages = Math.max(1, Number(totalPages) || 1)
  const currentPage = Math.min(safeTotalPages, Math.max(1, Number(page) + 1))

  return (
    <div className="pager">
      <p className="subtle-meta">
        {Number(totalElements) || 0} {noun} • Page {currentPage} of {safeTotalPages}
      </p>

      <div className="pager-controls">
        {onPageSizeChange ? (
          <label className="pager-size">
            <span>Page size</span>
            <select
              value={String(pageSize ?? '')}
              onChange={(event) => onPageSizeChange(Number(event.target.value))}
            >
              {pageSizeOptions.map((value) => (
                <option key={value} value={String(value)}>
                  {value}
                </option>
              ))}
            </select>
          </label>
        ) : null}

        <button className="btn btn-outline btn-sm" type="button" onClick={onPrev} disabled={first}>
          Prev
        </button>
        <button className="btn btn-outline btn-sm" type="button" onClick={onNext} disabled={last}>
          Next
        </button>
      </div>
    </div>
  )
}
