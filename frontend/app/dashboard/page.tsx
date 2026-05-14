'use client'

import { useEffect, useMemo, useState } from 'react'
import { DashboardLayout } from '@/components/layout/dashboard-layout'
import { Header } from '@/components/layout/header'
import { SummaryCard } from '@/components/dashboard/summary-card'
import { HealthScoreCard } from '@/components/dashboard/health-score-card'
import { RecentTransactions } from '@/components/dashboard/recent-transactions'
import { InsightCard } from '@/components/dashboard/insight-card'
import { ExpenseChart } from '@/components/dashboard/expense-chart'
import { TrendChart } from '@/components/dashboard/trend-chart'
import { formatCurrency } from '@/lib/mock-data'
import { apiFetch, ApiError, decodeJwtPayload, getToken } from '@/lib/api'
import {
  num,
  summaryToExpenseChartData,
  trendApiToChartData,
  trendFromTransactions,
  type SummaryResponse,
  type TrendMonthPoint,
  type TrendResponse,
} from '@/lib/summary'
import { useToast } from '@/hooks/use-toast'
import {
  TrendingUp,
  TrendingDown,
  Wallet,
  AlertTriangle,
} from 'lucide-react'

type TxRow = {
  amount: number
  type: 'INCOME' | 'EXPENSE'
  transactionDate: string
  isAnomaly?: boolean
}

export default function DashboardPage() {
  const { toast } = useToast()
  const currentMonth = new Date().toLocaleDateString('vi-VN', {
    month: 'long',
    year: 'numeric',
  })

  const monthKey = new Date().toISOString().slice(0, 7)

  const [accountsBalance, setAccountsBalance] = useState(0)
  const [transactions, setTransactions] = useState<TxRow[]>([])
  const [summary, setSummary] = useState<SummaryResponse | null | undefined>(undefined)
  const [trendFromApi, setTrendFromApi] = useState<TrendMonthPoint[] | null>(null)
  const [loading, setLoading] = useState(true)

  const userName = useMemo(() => {
    const token = getToken()
    if (!token) return 'bạn'
    const payload = decodeJwtPayload(token)
    const email = payload?.sub || payload?.email
    if (!email) return 'bạn'
    return String(email).split('@')[0]
  }, [])

  useEffect(() => {
    const run = async () => {
      setLoading(true)
      setSummary(undefined)
      setTrendFromApi(null)
      const summaryPath = `/api/summary?month=${encodeURIComponent(monthKey)}`
      const trendPath = `/api/summary/trend?months=6`

      const settled = await Promise.allSettled([
        apiFetch<{ balance: number }[]>('/api/accounts', { method: 'GET' }),
        apiFetch<any[]>('/api/transactions', { method: 'GET' }),
        apiFetch<SummaryResponse>(summaryPath, { method: 'GET' }),
        apiFetch<TrendResponse>(trendPath, { method: 'GET' }),
      ])

      const errs: string[] = []

      const accRes = settled[0]
      if (accRes.status === 'fulfilled' && accRes.value.data) {
        const balance = accRes.value.data.reduce(
          (sum: number, a: { balance?: number }) => sum + Number(a.balance || 0),
          0
        )
        setAccountsBalance(balance)
      } else {
        setAccountsBalance(0)
        if (accRes.status === 'rejected') errs.push('Tài khoản')
      }

      const txRes = settled[1]
      if (txRes.status === 'fulfilled' && txRes.value.data) {
        setTransactions(
          txRes.value.data.map((t: any) => ({
            amount: Number(t.amount || 0),
            type: t.type,
            transactionDate: String(t.transactionDate || ''),
            isAnomaly: Boolean(t.isAnomaly),
          }))
        )
      } else {
        setTransactions([])
        if (txRes.status === 'rejected') errs.push('Giao dịch')
      }

      const sumRes = settled[2]
      if (sumRes.status === 'fulfilled' && sumRes.value.data) {
        setSummary(sumRes.value.data)
      } else {
        setSummary(null)
        if (sumRes.status === 'rejected') {
          const e = sumRes.reason
          errs.push(
            e instanceof ApiError ? e.message : 'Tổng quan (summary)'
          )
        } else {
          errs.push('Tổng quan (summary)')
        }
      }

      const trendRes = settled[3]
      if (trendRes.status === 'fulfilled' && trendRes.value.data?.trend?.length) {
        setTrendFromApi(trendApiToChartData(trendRes.value.data.trend))
      } else {
        setTrendFromApi(null)
        if (trendRes.status === 'rejected') {
          const e = trendRes.reason
          errs.push(e instanceof ApiError ? e.message : 'Xu hướng (trend)')
        }
      }

      if (errs.length) {
        toast({
          title: 'Một phần dữ liệu không tải được',
          description: errs.join(' · '),
          variant: 'destructive',
        })
      }

      setLoading(false)
    }
    run()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [monthKey])

  const monthTx = useMemo(() => {
    return transactions.filter(t => String(t.transactionDate || '').startsWith(monthKey))
  }, [transactions, monthKey])

  const fallbackIncome = useMemo(
    () => monthTx.filter(t => t.type === 'INCOME').reduce((sum, t) => sum + t.amount, 0),
    [monthTx]
  )
  const fallbackExpense = useMemo(
    () => monthTx.filter(t => t.type === 'EXPENSE').reduce((sum, t) => sum + t.amount, 0),
    [monthTx]
  )

  const totalIncome = summary ? num(summary.totalIncome) : fallbackIncome
  const totalExpense = summary ? num(summary.totalExpense) : fallbackExpense
  const currentBalance = summary ? num(summary.currentBalance) : accountsBalance

  const expenseChartSlices = summary ? summaryToExpenseChartData(summary) : []
  const trendPoints = useMemo(() => {
    if (trendFromApi && trendFromApi.length > 0) return trendFromApi
    return trendFromTransactions(transactions, 6)
  }, [trendFromApi, transactions])

  const hasAnomaly = monthTx.some(t => Boolean(t.isAnomaly))

  return (
    <DashboardLayout>
      <Header
        title={`Xin chào, ${userName}!`}
        subtitle={`Tổng quan tài chính ${currentMonth}`}
      />

      <div className="p-6">
        {hasAnomaly && (
          <div className="mb-6 flex items-center gap-3 rounded-lg border border-warning/30 bg-warning/10 px-4 py-3">
            <AlertTriangle className="h-5 w-5 text-warning" />
            <p className="text-sm text-foreground">
              Phát hiện <strong>giao dịch bất thường</strong> trong tháng này.{' '}
              <a href="/insights" className="font-medium underline">
                Xem chi tiết
              </a>
            </p>
          </div>
        )}

        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <SummaryCard
            title="Tổng thu tháng này"
            value={loading ? '...' : formatCurrency(totalIncome)}
            icon={TrendingUp}
            variant="success"
          />
          <SummaryCard
            title="Tổng chi tháng này"
            value={loading ? '...' : formatCurrency(totalExpense)}
            icon={TrendingDown}
            variant="danger"
          />
          <SummaryCard
            title="Số dư hiện tại"
            value={loading ? '...' : formatCurrency(currentBalance)}
            icon={Wallet}
            variant="default"
          />
          <HealthScoreCard score={78} size="sm" />
        </div>

        <div className="mt-6 grid gap-6 lg:grid-cols-2">
          <ExpenseChart data={expenseChartSlices} loading={loading} />
          <TrendChart data={trendPoints} loading={loading} />
        </div>

        <div className="mt-6 grid gap-6 lg:grid-cols-3">
          <div className="lg:col-span-2">
            <RecentTransactions />
          </div>
          <div className="space-y-4">
            <h3 className="font-semibold text-foreground">AI Insight</h3>
            <InsightCard
              insight={{
                id: 'placeholder',
                type: 'tip',
                title: 'Gợi ý',
                description:
                  summary && num(summary.netBalance) < 0
                    ? `Tháng này chi nhiều hơn thu ${formatCurrency(Math.abs(num(summary.netBalance)))}. Cân nhắc rà soát các danh mục chi lớn trên biểu đồ.`
                    : 'Backend chưa có endpoint insight/health-score. Bạn có thể nối khi Sprint 7 xong.',
                icon: 'lightbulb',
                date: new Date().toISOString().slice(0, 10),
              }}
              featured
            />
          </div>
        </div>
      </div>
    </DashboardLayout>
  )
}
