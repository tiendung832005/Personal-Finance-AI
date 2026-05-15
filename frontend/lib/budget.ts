import { num } from '@/lib/summary'

export type BudgetLineStatus = 'OK' | 'WARNING' | 'EXCEEDED'

export type BudgetStatusItem = {
  categoryId: number
  categoryName: string
  budgetAmount: number | string
  actualAmount: number | string
  remainingAmount: number | string
  usedPercentage: number
  status: BudgetLineStatus
}

export type BudgetStatusResponse = {
  month: string
  totalBudget: number | string
  totalActual: number | string
  totalRemaining: number | string
  items: BudgetStatusItem[]
}

export type BudgetRow = {
  id: number
  categoryId: number
  categoryName: string
  amount: number | string
  month: string
}

/** Gom chi EXPENSE theo categoryId trong tháng yyyy-MM (transactionDate ISO). */
export function expenseTotalsByCategory(
  transactions: {
    amount: number | string
    type: string
    categoryId?: number | null
    transactionDate?: string
  }[],
  monthKey: string
): Map<number, number> {
  const map = new Map<number, number>()
  for (const t of transactions) {
    if (t.type !== 'EXPENSE' || t.categoryId == null) continue
    const d = String(t.transactionDate || '')
    if (!d.startsWith(monthKey)) continue
    const id = Number(t.categoryId)
    const amt = num(t.amount)
    map.set(id, (map.get(id) || 0) + amt)
  }
  return map
}

export function budgetAmount(b: BudgetRow): number {
  return num(b.amount)
}

export function inferBudgetLineStatusFromPct(pct: number): BudgetLineStatus {
  if (pct >= 100) return 'EXCEEDED'
  if (pct >= 80) return 'WARNING'
  return 'OK'
}

export function statusLabelVi(status: BudgetLineStatus): string {
  switch (status) {
    case 'EXCEEDED':
      return 'Vượt ngân sách'
    case 'WARNING':
      return 'Cảnh báo'
    default:
      return 'Ổn định'
  }
}

export function statusTextClass(status: BudgetLineStatus): string {
  switch (status) {
    case 'EXCEEDED':
      return 'text-destructive'
    case 'WARNING':
      return 'text-warning'
    default:
      return 'text-success'
  }
}

export function statusBarClass(status: BudgetLineStatus): string {
  switch (status) {
    case 'EXCEEDED':
      return 'bg-destructive'
    case 'WARNING':
      return 'bg-warning'
    default:
      return 'bg-success'
  }
}
