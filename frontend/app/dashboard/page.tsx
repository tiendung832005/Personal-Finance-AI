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
import {
  formatCurrency,
} from '@/lib/mock-data'
import { apiFetch, ApiError, decodeJwtPayload, getToken } from '@/lib/api'
import { useToast } from '@/hooks/use-toast'
import {
  TrendingUp,
  TrendingDown,
  Wallet,
  AlertTriangle,
} from 'lucide-react'

export default function DashboardPage() {
  const { toast } = useToast()
  const currentMonth = new Date().toLocaleDateString('vi-VN', {
    month: 'long',
    year: 'numeric',
  })

  const [accountsBalance, setAccountsBalance] = useState(0)
  const [transactions, setTransactions] = useState<
    { amount: number; type: 'INCOME' | 'EXPENSE'; transactionDate: string; isAnomaly?: boolean }[]
  >([])
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
      try {
        const [accRes, txRes] = await Promise.all([
          apiFetch<{ balance: number }[]>('/api/accounts', { method: 'GET' }),
          apiFetch<any[]>('/api/transactions', { method: 'GET' }),
        ])
        const balance = (accRes.data || []).reduce((sum: number, a: any) => sum + Number(a.balance || 0), 0)
        setAccountsBalance(balance)
        setTransactions((txRes.data || []).map((t: any) => ({
          amount: Number(t.amount || 0),
          type: t.type,
          transactionDate: t.transactionDate,
          isAnomaly: t.isAnomaly,
        })))
      } catch (err: any) {
        toast({
          title: 'Không tải được dashboard',
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

  const monthKey = new Date().toISOString().slice(0, 7) // YYYY-MM
  const monthTx = useMemo(() => {
    return transactions.filter(t => String(t.transactionDate || '').startsWith(monthKey))
  }, [transactions, monthKey])

  const totalIncome = useMemo(
    () => monthTx.filter(t => t.type === 'INCOME').reduce((sum, t) => sum + t.amount, 0),
    [monthTx]
  )
  const totalExpense = useMemo(
    () => monthTx.filter(t => t.type === 'EXPENSE').reduce((sum, t) => sum + t.amount, 0),
    [monthTx]
  )

  const hasAnomaly = monthTx.some(t => Boolean(t.isAnomaly))

  return (
    <DashboardLayout>
      <Header
        title={`Xin chào, ${userName}!`}
        subtitle={`Tổng quan tài chính ${currentMonth}`}
      />

      <div className="p-6">
        {/* Anomaly Warning Banner */}
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

        {/* Summary Cards */}
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <SummaryCard
            title="Tổng thu tháng này"
            value={loading ? '...' : formatCurrency(totalIncome)}
            icon={TrendingUp}
            variant="success"
            trend={{ value: 12, isPositive: true }}
          />
          <SummaryCard
            title="Tổng chi tháng này"
            value={loading ? '...' : formatCurrency(totalExpense)}
            icon={TrendingDown}
            variant="danger"
            trend={{ value: 8, isPositive: false }}
          />
          <SummaryCard
            title="Số dư hiện tại"
            value={loading ? '...' : formatCurrency(accountsBalance)}
            icon={Wallet}
            variant="default"
          />
          <HealthScoreCard score={78} size="sm" />
        </div>

        {/* Charts Row */}
        <div className="mt-6 grid gap-6 lg:grid-cols-2">
          <ExpenseChart />
          <TrendChart />
        </div>

        {/* Insights and Transactions */}
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
                description: 'Backend chưa có endpoint insight/health-score. Bạn có thể nối khi Sprint 7 xong.',
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
