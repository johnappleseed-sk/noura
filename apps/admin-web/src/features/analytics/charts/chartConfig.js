import { formatCurrency } from "../../../shared/ui/formatters"

/**
 * Base chart options for cartesian charts.
 */
export const baseCartesianOptions = {
  responsive: true,
  maintainAspectRatio: false,
  scales: {
    x: {
      grid: { display: false }
    },
    y: {
      grid: { color: "#f1f5f9" }
    }
  }
}

/**
 * Revenue line chart options.
 */
export const revenueLineOptions = {
  ...baseCartesianOptions,

  plugins: {
    legend: { display: false },

    tooltip: {
      callbacks: {
        label: ctx => `Revenue: ${formatCurrency(ctx.parsed.y)}`
      }
    }
  }
}

/**
 * Generic bar chart options.
 */
export const barChartOptions = {
  ...baseCartesianOptions,
  plugins: {
    legend: { display: false }
  }
}

/**
 * Generic pie chart options.
 */
export const pieChartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      position: "bottom"
    }
  }
}
