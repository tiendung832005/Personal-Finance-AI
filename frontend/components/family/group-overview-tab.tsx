'use client'

import { useCallback, useEffect, useMemo, useState } from 'react'
import { SummaryCard } from '@/components/dashboard/summary-card'
import { ExpenseChart } from '@/components/dashboard/expense-chart'
import { TrendChart } from '@/components/dashboard/trend-chart'
import { Button } from '@/components/ui/button'
import { formatCurrency } from '@/lib/mock-data'
import { ApiError } from '@/lib/api'
import {
  getFamilySummary,
  getFamilyTrend,
  monthKeyFromDate,
} from '@/lib/group-finance'
import {
  num,
  summaryToExpenseChartData,
  trendApiToChartData,
  type SummaryResponse,
} from '@/lib/summary'
import { useToast } from '@/hooks/use-toast'
import { ChevronLeft, ChevronRight, Loader2, TrendingDown, TrendingUp, Wallet } from 'lucide-react'

type Props = {
  groupId: number
}

export function GroupOverviewTab({ groupId }: Props) {
  const { toast } = useToast()
  const [currentMonth, setCurrentMonth] = useState(new Date())
  const monthKey = useMemo(() => monthKeyFromDate(currentMonth), [currentMonth])

  const [loading, setLoading] = useState(true)
  const [summary, setSummary] = useState<Awaited<ReturnType<typeof getFamilySummary>> | null>(null)
  const [trendPoints, setTrendPoints] = useState<ReturnType<typeof trendApiToChartData>>([])

  const monthLabel = currentMonth.toLocaleDateString('vi-VN', {
    month: 'long',
    year: 'numeric',
  })

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const [sum, trend] = await Promise.all([
        getFamilySummary(groupId, monthKey),
        getFamilyTrend(groupId, 6),
      ])
      setSummary(sum)
      setTrendPoints(trendApiToChartData(trend.trend))
    } catch (err) {
      const message = err instanceof ApiError ? err.message : 'Không tải được tổng quan'
      toast({ title: 'Lỗi', description: message, variant: 'destructive' })
      setSummary(null)
      setTrendPoints([])
    } finally {
      setLoading(false)
    }
  }, [groupId, monthKey, toast])

  useEffect(() => {
    load()
  }, [load])

  const expenseChartData = useMemo(() => {
    if (!summary) return []
    const adapted: SummaryResponse = {
      month: summary.month,
      totalIncome: summary.totalIncome,
      totalExpense: summary.totalExpense,
      netBalance: summary.netBalance,
      currentBalance: summary.groupAccountBalance,
      categoryBreakdown: summary.categoryBreakdown,
    }
    return summaryToExpenseChartData(adapted)
  }, [summary])

  if (loading) {
    return (
      <div className="flex justify-center py-16">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center gap-2">
        <Button
          variant="outline"
          size="icon"
          onClick={() =>
            setCurrentMonth(d => new Date(d.getFullYear(), d.getMonth() - 1, 1))
          }
        >
          <ChevronLeft className="h-4 w-4" />
        </Button>
        <span className="min-w-[140px] text-center text-sm font-medium capitalize">
          {monthLabel}
        </span>
        <Button
          variant="outline"
          size="icon"
          onClick={() =>
            setCurrentMonth(d => new Date(d.getFullYear(), d.getMonth() + 1, 1))
          }
        >
          <ChevronRight className="h-4 w-4" />
        </Button>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <SummaryCard
          title="Quỹ chung"
          value={formatCurrency(num(summary?.groupAccountBalance))}
          icon={Wallet}
          variant="default"
        />
        <SummaryCard
          title="Thu tháng này"
          value={formatCurrency(num(summary?.totalIncome))}
          icon={TrendingUp}
          variant="success"
        />
        <SummaryCard
          title="Chi tháng này"
          value={formatCurrency(num(summary?.totalExpense))}
          icon={TrendingDown}
          variant="danger"
        />
        <SummaryCard
          title="Tiết kiệm tháng"
          value={formatCurrency(num(summary?.netBalance))}
          icon={Wallet}
          variant="default"
        />
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <ExpenseChart data={expenseChartData} />
        <TrendChart data={trendPoints} />
      </div>
    </div>
  )
}
