import { num } from '@/lib/summary'

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
