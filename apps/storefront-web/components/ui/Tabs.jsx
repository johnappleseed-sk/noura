'use client'

import { useState } from 'react'

/**
 * Tabs — Tab navigation with panels.
 *
 * tabs: [{ id, label, badge?, disabled?, content }]
 */
export default function Tabs({ tabs = [], defaultTab, onChange, className = '' }) {
  const [active, setActive] = useState(defaultTab || tabs[0]?.id)

  const handleChange = (id) => {
    setActive(id)
    onChange?.(id)
  }

  const activeTab = tabs.find((t) => t.id === active)

  return (
    <div className={`tabs-container ${className}`}>
      <div className="tabs" role="tablist">
        {tabs.map((tab) => (
          <button
            key={tab.id}
            type="button"
            role="tab"
            className={`tab ${active === tab.id ? 'active' : ''}`}
            aria-selected={active === tab.id}
            aria-controls={`panel-${tab.id}`}
            disabled={tab.disabled}
            onClick={() => handleChange(tab.id)}
          >
            {tab.label}
            {tab.badge != null && <span className="tab-badge">{tab.badge}</span>}
          </button>
        ))}
      </div>
      {activeTab?.content && (
        <div id={`panel-${activeTab.id}`} role="tabpanel" className="tab-panel">
          {activeTab.content}
        </div>
      )}
    </div>
  )
}

/**
 * SegmentedControl — Button group toggling between options.
 */
export function SegmentedControl({ options = [], value, onChange, className = '' }) {
  return (
    <div className={`segmented-control ${className}`} role="radiogroup">
      {options.map((opt) => (
        <button
          key={opt.value}
          type="button"
          role="radio"
          aria-checked={value === opt.value}
          className={`segment ${value === opt.value ? 'active' : ''}`}
          onClick={() => onChange?.(opt.value)}
        >
          {opt.label}
        </button>
      ))}
    </div>
  )
}
