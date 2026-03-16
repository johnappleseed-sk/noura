export function Panel({ title, description, actions, children, className = '' }) {
  return (
    <section className={`panel${className ? ` ${className}` : ''}`}>
      {title || description || actions ? (
        <div className="section-head">
          <div>
            {title ? <h3>{title}</h3> : null}
            {description ? <p>{description}</p> : null}
          </div>
          {actions ? <div className="section-actions">{actions}</div> : null}
        </div>
      ) : null}
      {children}
    </section>
  )
}

