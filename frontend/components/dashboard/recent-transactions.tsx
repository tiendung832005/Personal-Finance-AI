'use client'

import { useEffect, useMemo, useState } from 'react'
import { cn } from '@/lib/utils'
import { formatCurrency, formatDate } from '@/lib/mock-data'
import { apiFetch, ApiError } from '@/lib/api'
import { useToast } from '@/hooks/use-toast'
import {
  Utensils,
  Car,
  ShoppingBag,
  FileText,
  Gamepad2,
  HeartPulse,
  GraduationCap,
  MoreHorizontal,
  Briefcase,
  Gift,
  TrendingUp,
  PlusCircle,
  Sparkles,
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import Link from 'next/link'

const iconMap: Record<string, React.ElementType> = {
  utensils: Utensils,
  car: Car,
  'shopping-bag': ShoppingBag,
  'file-text': FileText,
  'gamepad-2': Gamepad2,
  'heart-pulse': HeartPulse,
  'graduation-cap': GraduationCap,
  'more-horizontal': MoreHorizontal,
  briefcase: Briefcase,
  gift: Gift,
  'trending-up': TrendingUp,
  'plus-circle': PlusCircle,
}

type Category = { id: number; name: string; icon: string | null; color: string | null }
type Account = { id: number; name: string }
type Transaction = {
  id: number
  accountId: number
  amount: number
  type: 'INCOME' | 'EXPENSE'
  description: string | null
  categoryId: number | null
  transactionDate: string
  isAutoCategorized: boolean
}

export function RecentTransactions() {
  const { toast } = useToast()
  const [transactions, setTransactions] = useState<Transaction[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [accounts, setAccounts] = useState<Account[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const run = async () => {
      setLoading(true)
      try {
        const [txRes, catRes, accRes] = await Promise.all([
          apiFetch<Transaction[]>('/api/transactions', { method: 'GET' }),
          apiFetch<Category[]>('/api/categories', { method: 'GET' }),
          apiFetch<Account[]>('/api/accounts', { method: 'GET' }),
        ])
        const tx = (txRes.data || []).map(t => ({ ...t, amount: Number(t.amount) }))
        setTransactions(tx.slice(0, 5))
        setCategories(catRes.data || [])
        setAccounts(accRes.data || [])
      } catch (err: any) {
        toast({
          title: 'Không tải được giao dịch gần đây',
          description: err instanceof ApiError ? err.message : 'Vui lòng đăng nhập lại.',
          variant: 'destructive',
        })
      } finally {
        setLoading(false)
      }
    }
    run()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const getCategoryById = useMemo(
    () => (id: number | null) => (id == null ? undefined : categories.find(c => c.id === id)),
    [categories]
  )
  const getAccountById = useMemo(
    () => (id: number) => accounts.find(a => a.id === id),
    [accounts]
  )

  return (
    <div className="rounded-xl border border-border bg-card shadow-sm">
      <div className="flex items-center justify-between border-b border-border px-5 py-4">
        <h3 className="font-semibold text-foreground">Giao dịch gần đây</h3>
        <Link href="/transactions">
          <Button variant="ghost" size="sm" className="text-muted-foreground">
            Xem tất cả
          </Button>
        </Link>
      </div>
      <div className="divide-y divide-border">
        {loading ? (
          <div className="px-5 py-4 text-sm text-muted-foreground">Đang tải...</div>
        ) : transactions.length === 0 ? (
          <div className="px-5 py-4 text-sm text-muted-foreground">Chưa có giao dịch.</div>
        ) : transactions.map(transaction => {
          const category = getCategoryById(transaction.categoryId)
          const account = getAccountById(transaction.accountId)
          const Icon = category ? iconMap[String(category.icon || '')] || MoreHorizontal : MoreHorizontal

          return (
            <div
              key={transaction.id}
              className="flex items-center gap-4 px-5 py-3 transition-colors hover:bg-secondary/50"
            >
              <div
                className="flex h-10 w-10 items-center justify-center rounded-lg"
                style={{ backgroundColor: `${category?.color || '#9E9890'}15` }}
              >
                <Icon
                  className="h-5 w-5"
                  style={{ color: category?.color || '#9E9890' }}
                />
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2">
                  <p className="truncate font-medium text-foreground">
                    {transaction.description || '(Không có mô tả)'}
                  </p>
                  {transaction.isAutoCategorized && (
                    <span className="flex items-center gap-1 rounded-full bg-primary/10 px-2 py-0.5 text-xs text-primary">
                      <Sparkles className="h-3 w-3" />
                      AI
                    </span>
                  )}
                </div>
                <p className="text-sm text-muted-foreground">
                  {category?.name} • {account?.name}
                </p>
              </div>
              <div className="text-right">
                <p
                  className={cn(
                    'font-semibold tabular-nums',
                    transaction.type === 'INCOME'
                      ? 'text-success'
                      : 'text-destructive'
                  )}
                >
                  {transaction.type === 'INCOME' ? '+' : '-'}
                  {formatCurrency(transaction.amount)}
                </p>
                <p className="text-sm text-muted-foreground">
                  {formatDate(transaction.transactionDate)}
                </p>
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}
