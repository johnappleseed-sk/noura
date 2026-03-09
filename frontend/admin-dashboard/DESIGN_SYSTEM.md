# Noura Admin Dashboard — UI/UX Design System

This dashboard targets internal administrators/managers. The UI is optimized for speed, clarity, and dense operational workflows while maintaining strong accessibility.

## 1) Design Tokens (CSS Custom Properties)

Tokens live in `src/styles/theme.css` and are applied via CSS variables. Dark mode is controlled by setting `data-theme="dark"` on `<html>`.

### Core palette (Light)

- Primary: `#2563eb` (`--primary`)
- Success: `#10b981` (`--success`)
- Warning: `#f59e0b` (`--warning`)
- Error: `#ef4444` (`--error`)
- Info: `#3b82f6` (`--info`)
- Background: `#f9fafb` (`--bg`)
- Surface: `#ffffff` (`--panel`)
- Border: `#e5e7eb` (`--line`)
- Text: `#111827` (`--text`)
- Text secondary: `#6b7280` (`--muted`)
- Disabled text: `#9ca3af` (`--disabled`)

### Dark mode (Automatic + Toggle)

Dark mode tokens are defined under `:root[data-theme="dark"]`:

- Background: `#1f2937`
- Surface: `#374151`
- Borders: translucent whites
- Text: `#f9fafb`
- Text secondary: `#9ca3af`

### Legacy compatibility

The system also maps legacy variables (`--brand`, `--good`, etc.) to the new palette so existing pages continue working without functional changes.

## 2) Typography

Font: `Inter` (loaded in `src/styles/theme.css`) with system fallbacks.

Recommended scale:

- Page title (H1): 24px/32px, bold (`.page-head h2`)
- Section header (H2): 20px/28px, semibold (card/section titles)
- Card title (H3): 18px/24px, semibold
- Body: 14px/20px, regular
- Small: 12px/16px, regular (labels, metadata)
- Caption: 11px/14px, medium (badges, timestamps)

## 3) Spacing + Layout Grid

Use a 4/8px rhythm:

- `--space-1` (4), `--space-2` (8), `--space-3` (12), `--space-4` (16), `--space-5` (20), `--space-6` (24)

Layout primitives:

- App shell: sidebar + content (`.admin-shell`)
- Content padding: `.page-content`
- Page vertical rhythm: `.page` uses a consistent gap

## 4) App Shell (Header + Sidebar)

### Header

`src/app/layouts/AdminLayout.jsx` implements:

- Left: mobile menu button + user greeting
- Center: global search trigger (⌘K / Ctrl+K) -> command palette
- Right: theme toggle, notifications (unread badge), help, profile dropdown

### Sidebar

- Collapsible: full width vs icon-only (`.admin-shell.sidebar-collapsed`)
- Group labels (hidden when collapsed)
- Active state highlighting and hover states
- Mobile: slide-in drawer with backdrop (≤ 960px)

## 5) Component Library

### Cards

Use `.panel` for containers and `.metric-card` for KPI tiles:

- Radius: 12px (`--radius-lg`)
- Shadow: subtle (`--shadow-soft`)
- Border: `--line`

Reusable wrapper component:

- `src/shared/ui/Panel.jsx`

### Buttons

Classes:

- `.btn .btn-primary` (primary action)
- `.btn .btn-outline` (secondary)
- `.btn .btn-danger` (destructive)
- `.btn-sm` (compact)
- `.icon-btn` (icon-only)

States:

- Hover: subtle lift
- Focus: visible blue ring
- Disabled: reduced opacity + not-allowed cursor

### Forms

Inputs/selects/textarea:

- Border: `--line`
- Focus ring: `rgba(37,99,235,0.25)`
- Disabled: muted + `--bg-secondary`

### Badges

- `.badge` with variants: `.badge-success`, `.badge-warning`, `.badge-danger`, `.badge-info`, `.badge-muted`
- For notification dots: combine with `.badge-dot`

### Tables

`.table-wrap` provides:

- Sticky headers
- Hover row highlighting
- Horizontal scroll for narrow screens

### Modals

Base modal primitives:

- `.overlay`, `.overlay-backdrop`, `.modal`, `.modal-head`, `.modal-body`

Currently used by:

- Command palette (`src/shared/ui/CommandPalette.jsx`)

### Toasts (recommended pattern)

Not required by current flows, but recommended:

- Provider at app root + `useToast()`
- Variants aligned to success/error/warning/info
- Auto-dismiss 3–5s + manual close + progress bar

## 6) Interactions & Animations

Guidelines:

- Use 200–300ms transitions (hover/focus)
- Prefer subtle transforms (`translateY(-1px)`) to communicate affordance
- Keep motion reduced-friendly:
  - Respect `prefers-reduced-motion` for future animations

## 7) Accessibility Checklist (WCAG 2.1 AA)

- Keyboard navigation: all interactive elements reachable + usable
- Visible focus ring (`:focus-visible`) on buttons/links/inputs
- ARIA labels on icon-only buttons (`.icon-btn`)
- Semantic headings: one H1-equivalent per page
- Sufficient contrast (especially dark mode)
- Avoid placeholder-only labels for inputs

## 8) Sample Page Layouts (Reference)

### Analytics Dashboard

- Date range selector (presets + custom)
- KPI row (4 cards)
- Revenue chart + comparison toggle
- Acquisition chart
- Top products bar chart
- Distribution donut
- Export report action

### Orders Management

- Advanced filters panel (status/date/amount/customer)
- Bulk actions toolbar
- Sortable/sticky table header
- Inline status updates
- Order details slide-over panel
- Export CSV/JSON

### Product Catalog

- Grid/list view toggle
- Category filter sidebar
- Search input with debounce
- “Add product” FAB (44px)
- Bulk edit actions + quick edit modal

### Customer Details

- Profile header with avatar + contact actions
- Tabs: Overview / Orders / Activity / Support
- KPI tiles: LTV, AOV, total orders
- Timeline feed
- Add note modal

## 9) Implementation Notes

### Theme toggle

- Implemented via `ThemeProvider` (`src/shared/ui/ThemeProvider.jsx`)
- Stores preference in `localStorage` under `noura.admin.theme`
- Applies the resolved theme by setting `document.documentElement.dataset.theme`

### Global search / command palette

- Component: `src/shared/ui/CommandPalette.jsx`
- Trigger:
  - Click search bar
  - Press Ctrl+K / ⌘K
- Searches navigation + key actions and remembers recent queries

### Tailwind (optional)

This project currently uses CSS variables (no Tailwind dependency). If you choose to add Tailwind later, map Tailwind config colors to the same design tokens and keep the tokens as the source of truth.

Example Tailwind-ish equivalents:

```jsx
<button className="px-4 h-9 rounded-md bg-[#2563eb] text-white hover:bg-[#1d4ed8] focus:outline-none focus:ring-4 focus:ring-blue-200">
  Primary
</button>
```

## 10) Performance Guidelines

- Lazy-load heavy pages (charts/reports) via route-level splitting
- Debounce search inputs
- Prefer pagination/virtualization for long tables
- Keep charts responsive; avoid re-render loops by memoizing datasets/options

