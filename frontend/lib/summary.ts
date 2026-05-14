/** Mirrors BE `SummaryResponse` / `CategoryBreakdownItem` (amounts may arrive as strings). */

export type CategoryBreakdownItem = {
  categoryId: number
  categoryName: string
  type: 'INCOME' | 'EXPENSE'
  total: number | string
  percentage: number | null
  transactionCount: number
}

export type SummaryResponse = {
  month: string
  totalIncome: number | string
  totalExpense: number | string
  netBalance: number | string
  currentBalance: number | string
  categoryBreakdown: CategoryBreakdownItem[]
}

export type TrendMonthPoint = {
  /** Nhãn trục X (vd. T5) */
  month: string
  /** yyyy-MM — dùng cho tooltip khi có */
  monthKey?: string
  income: number
  expense: number
}

export type TrendItem = {
  month: string
  income: number | string
  expense: number | string
  net: number | string
}

export type TrendResponse = {
  trend: TrendItem[]
}

const EXPENSE_CHART_COLORS = [
  '#A0522D',
  '#6B7FA3',
  '#8B6F8B',
  '#C49A3C',
  '#7A9E9F',
  '#9E9890',
  '#4A7C59',
  '#8B4513',
]

export function num(v: number | string | null | undefined): number {
  if (v == null) return 0
  const n = typeof v === 'number' ? v : Number(v)
  return Number.isFinite(n) ? n : 0
}

/** Pie slices: EXPENSE categories only, largest first (BE already orders by total DESC). */
export function summaryToExpenseChartData(summary: SummaryResponse | null): {
  name: string
  value: number
  color: string
}[] {
  if (!summary?.categoryBreakdown?.length) return []
  return summary.categoryBreakdown
    .filter(c => c.type === 'EXPENSE' && num(c.total) > 0)
    .map((c, i) => ({
      name: c.categoryName,
      value: num(c.total),
      color: EXPENSE_CHART_COLORS[i % EXPENSE_CHART_COLORS.length],
    }))
}

/**
 * Last `monthCount` months including current, oldest → newest (for line chart).
 * Uses the same month boundaries as BE summary (`YYYY-MM` on transactionDate).
 */
export function trendFromTransactions(
  transactions: { amount: number; type: string; transactionDate: string }[],
  monthCount = 6
): TrendMonthPoint[] {
  const now = new Date()
  const points: TrendMonthPoint[] = []
  for (let offset = monthCount - 1; offset >= 0; offset--) {
    const d = new Date(now.getFullYear(), now.getMonth() - offset, 1)
    const y = d.getFullYear()
    const m = d.getMonth() + 1
    const key = `${y}-${String(m).padStart(2, '0')}`
    const label = `T${m}`
    let income = 0
    let expense = 0
    for (const t of transactions) {
      const dateStr = String(t.transactionDate || '')
      if (!dateStr.startsWith(key)) continue
      const amt = Number(t.amount) || 0
      if (t.type === 'INCOME') income += amt
      else if (t.type === 'EXPENSE') expense += amt
    }
    points.push({
      month: label,
      monthKey: key,
      income,
      expense,
    })
  }
  return points
}

/** Map BE `/api/summary/trend` → dữ liệu Recharts (thứ tự giữ nguyên). */
export function trendApiToChartData(trend: TrendItem[] | undefined | null): TrendMonthPoint[] {
  if (!trend?.length) return []
  return trend.map(t => {
    const parts = t.month.split('-')
    const m = parts.length >= 2 ? parseInt(parts[1], 10) : NaN
    const short = Number.isFinite(m) ? `T${m}` : t.month
    return {
      month: short,
      monthKey: t.month,
      income: num(t.income),
      expense: num(t.expense),
    }
  })
}
