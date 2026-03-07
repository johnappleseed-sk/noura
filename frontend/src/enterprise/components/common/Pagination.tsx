interface PaginationProps {
  currentPage: number
  totalPages: number
  onPageChange: (page: number) => void
}

/**
 * Renders the Pagination component.
 *
 * @param param1 The param1 value.
 * @returns The rendered component tree.
 */
export const Pagination = ({ currentPage, totalPages, onPageChange }: PaginationProps): JSX.Element | null => {
  if (totalPages <= 1) {
    return null
  }

  const pages = Array.from({ length: totalPages }, (_, index) => index + 1)

  return (
    <nav aria-label="Products pagination" className="mt-8 flex flex-wrap items-center justify-center gap-2">
      {pages.map((pageNumber) => (
        <button
          aria-current={currentPage === pageNumber ? 'page' : undefined}
          className={`m3-btn !px-4 !py-2 text-sm ${
            currentPage === pageNumber
              ? 'm3-btn-filled'
              : 'm3-btn-outlined'
          }`}
          key={pageNumber}
          onClick={() => onPageChange(pageNumber)}
          type="button"
        >
          {pageNumber}
        </button>
      ))}
    </nav>
  )
}
