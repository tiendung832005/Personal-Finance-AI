'use client'

import { useCallback, useEffect, useMemo, useState } from 'react'
import Link from 'next/link'
import { useParams } from 'next/navigation'
import {
  Bar,
  CartesianGrid,
  ComposedChart,
  Legend,
  Line,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { DashboardLayout } from '@/components/layout/dashboard-layout'
import { Header } from '@/components/layout/header'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { useToast } from '@/hooks/use-toast'
import { ApiError, apiFetch } from '@/lib/api'
import {
  alertLabel,
  alertTone,
  goalProgress,
  money,
  monthLabel,
  statusLabel,
  type GoalDetailResponse,
  type GoalPlanResponse,
  type GoalProgressResponse,
} from '@/lib/goals'
import { cn } from '@/lib/utils'
import { ArrowLeft, Bot, CalendarClock, RefreshCw, Target, Wallet } from 'lucide-react'

type ChartRow = {
  month: string
  label: string
  saved: number
  planned: number
}

export default function GoalDetailPage() {
  const params = useParams<{ id: string }>()
  const id = params.id
  const { toast } = useToast()

  const [detail, setDetail] = useState<GoalDetailResponse | null>(null)
  const [progress, setProgress] = useState<GoalProgressResponse | null>(null)
  const [plan, setPlan] = useState<GoalPlanResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [loadingPlan, setLoadingPlan] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const [detailRes, progressRes, planRes] = await Promise.all([
        apiFetch<GoalDetailResponse>(`/api/goals/${id}`, { method: 'GET' }),
        apiFetch<GoalProgressResponse>(`/api/goals/${id}/progress`, { method: 'GET' }),
        apiFetch<GoalPlanResponse>(`/api/goals/${id}/plan`, { method: 'GET' }),
      ])
      setDetail(detailRes.data || null)
      setProgress(progressRes.data || null)
      setPlan(planRes.data || null)
    } catch (err) {
      toast({
        title: 'Không tải được mục tiêu',
        description: err instanceof ApiError ? err.message : 'Vui lòng thử lại.',
        variant: 'destructive',
      })
    } finally {
      setLoading(false)
    }
  }, [id, toast])

  useEffect(() => {
    load()
  }, [load])

  const goal = progress?.goal || detail?.goal
  const chartRows = useMemo<ChartRow[]>(() => {
    return (progress?.monthlyHistory || []).map(item => ({
      month: item.month,
      label: monthLabel(item.month),
      saved: Number(item.savedAmount || 0),
      planned: Number(item.plannedAmount || 0),
    }))
  }, [progress])

  async function handleRegenerate() {
    setLoadingPlan(true)
    try {
      const res = await apiFetch<GoalPlanResponse>(`/api/goals/${id}/plan/regenerate`, {
        method: 'POST',
      })
      setPlan(res.data || null)
      toast({ title: 'Đã tạo lại kế hoạch AI' })
    } catch (err) {
      toast({
        title: 'Không tạo lại được kế hoạch',
        description: err instanceof ApiError ? err.message : 'Vui lòng thử lại.',
        variant: 'destructive',
      })
    } finally {
      setLoadingPlan(false)
    }
  }

  if (loading) {
    return (
      <DashboardLayout>
        <Header title="Mục tiêu tài chính" subtitle="Đang tải chi tiết" />
        <div className="p-6 text-sm text-muted-foreground">Đang tải...</div>
      </DashboardLayout>
    )
  }

  if (!goal) {
    return (
      <DashboardLayout>
        <Header title="Mục tiêu tài chính" subtitle="Không tìm thấy mục tiêu" />
        <div className="p-6">
          <Button asChild variant="outline">
            <Link href="/goals">
              <ArrowLeft className="h-4 w-4" />
              Quay lại
            </Link>
          </Button>
        </div>
      </DashboardLayout>
    )
  }

  const pct = goalProgress(goal)

  return (
    <DashboardLayout>
      <Header title={goal.name} subtitle="Kế hoạch tiết kiệm và tiến độ mục tiêu" />

      <div className="p-6">
        <div className="mb-6">
          <Button asChild variant="outline" size="sm">
            <Link href="/goals">
              <ArrowLeft className="h-4 w-4" />
              Danh sách mục tiêu
            </Link>
          </Button>
        </div>

        <div className="grid gap-6 lg:grid-cols-[1.1fr_0.9fr]">
          <section className="rounded-xl border border-border bg-card p-6 shadow-sm">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <div className="flex flex-wrap items-center gap-2">
                  <Badge variant="outline">{statusLabel(goal.status)}</Badge>
                  <Badge className={cn('border', alertTone(goal.alertStatus))}>
                    {alertLabel(goal.alertStatus)}
                  </Badge>
                </div>
                {goal.alertMessage && (
                  <p className="mt-3 text-sm text-muted-foreground">{goal.alertMessage}</p>
                )}
              </div>
              <div className="text-right">
                <p className="text-sm text-muted-foreground">Tiến độ</p>
                <p className="text-3xl font-bold tabular-nums">{pct.toFixed(1)}%</p>
              </div>
            </div>

            <div className="mt-6">
              <div className="mb-2 flex items-center justify-between text-sm">
                <span className="font-medium">{money(goal.currentAmount)} đã tiết kiệm</span>
                <span className="text-muted-foreground">Mục tiêu {money(goal.targetAmount)}</span>
              </div>
              <div className="h-3 w-full overflow-hidden rounded-full bg-secondary">
                <div className="h-full rounded-full bg-primary" style={{ width: `${pct}%` }} />
              </div>
            </div>

            <div className="mt-6 grid gap-4 sm:grid-cols-3">
              <div className="rounded-lg border border-border p-4">
                <p className="text-sm text-muted-foreground">Còn thiếu</p>
                <p className="mt-1 font-semibold tabular-nums">{money(goal.remainingAmount)}</p>
              </div>
              <div className="rounded-lg border border-border p-4">
                <p className="text-sm text-muted-foreground">Cần mỗi tháng</p>
                <p className="mt-1 font-semibold tabular-nums">{money(goal.monthlyNeeded)}</p>
              </div>
              <div className="rounded-lg border border-border p-4">
                <p className="text-sm text-muted-foreground">Thời gian</p>
                <p className="mt-1 font-semibold">{goal.monthsRemaining} tháng</p>
              </div>
            </div>

            <div className="mt-5 flex flex-wrap gap-4 text-sm text-muted-foreground">
              <span className="inline-flex items-center gap-1">
                <CalendarClock className="h-4 w-4" />
                Deadline {new Intl.DateTimeFormat('vi-VN').format(new Date(goal.deadline))}
              </span>
              {goal.linkedAccountName && (
                <span className="inline-flex items-center gap-1">
                  <Wallet className="h-4 w-4" />
                  {goal.linkedAccountName}
                </span>
              )}
            </div>
          </section>

          <section className="rounded-xl border border-border bg-card p-6 shadow-sm">
            <div className="flex items-center justify-between gap-3">
              <div className="flex items-center gap-2">
                <Bot className="h-5 w-5 text-primary" />
                <h2 className="font-semibold">Kế hoạch AI</h2>
              </div>
              <Button size="sm" variant="outline" onClick={handleRegenerate} disabled={loadingPlan}>
                <RefreshCw className={cn('h-4 w-4', loadingPlan && 'animate-spin')} />
                Tạo lại
              </Button>
            </div>
            {plan?.isFromCache && (
              <Badge variant="outline" className="mt-3">
                Đang dùng cache
              </Badge>
            )}
            <p className="mt-4 whitespace-pre-line text-sm leading-6 text-foreground">
              {loadingPlan ? 'AI đang phân tích...' : plan?.plan || detail?.aiPlan || 'Chưa có kế hoạch AI.'}
            </p>
          </section>
        </div>

        <section className="mt-6 rounded-xl border border-border bg-card p-6 shadow-sm">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 className="font-semibold">Tiến độ theo tháng</h2>
              <p className="text-sm text-muted-foreground">
                {progress?.summary
                  ? `${progress.summary.onTrackMonths} tháng đúng tiến độ, ${progress.summary.behindMonths} tháng chậm`
                  : 'Lịch sử snapshot hàng tháng'}
              </p>
            </div>
            <Target className="h-5 w-5 text-muted-foreground" />
          </div>

          <div className="mt-5 h-72">
            {chartRows.length === 0 ? (
              <div className="flex h-full items-center justify-center text-sm text-muted-foreground">
                Chưa có snapshot tháng nào.
              </div>
            ) : (
              <ResponsiveContainer width="100%" height="100%">
                <ComposedChart data={chartRows}>
                  <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
                  <XAxis dataKey="label" tick={{ fill: 'var(--muted-foreground)', fontSize: 12 }} tickLine={false} axisLine={false} />
                  <YAxis
                    tick={{ fill: 'var(--muted-foreground)', fontSize: 12 }}
                    tickLine={false}
                    axisLine={false}
                    tickFormatter={value => `${Math.round(Number(value) / 1000000)}M`}
                  />
                  <Tooltip
                    content={({ active, payload, label }) => {
                      if (!active || !payload?.length) return null
                      return (
                        <div className="rounded-lg border border-border bg-card px-3 py-2 shadow-lg">
                          <p className="font-medium">{label}</p>
                          {payload.map(item => (
                            <p key={item.dataKey} className="text-sm" style={{ color: item.color }}>
                              {item.dataKey === 'saved' ? 'Thực tế' : 'Kế hoạch'}: {money(item.value as number)}
                            </p>
                          ))}
                        </div>
                      )
                    }}
                  />
                  <Legend formatter={value => (value === 'saved' ? 'Thực tế' : 'Kế hoạch  ')} />
                  <Bar dataKey="saved" fill="var(--primary)" radius={[4, 4, 0, 0]} />
                  <Line type="monotone" dataKey="planned" stroke="var(--destructive)" strokeWidth={2} dot={{ r: 3 }} />
                </ComposedChart>
              </ResponsiveContainer>
            )}
          </div>
        </section>
      </div>
    </DashboardLayout>
  )
}
