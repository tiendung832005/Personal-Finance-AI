'use client'

import { useCallback, useEffect, useMemo, useState } from 'react'
import Link from 'next/link'
import { DashboardLayout } from '@/components/layout/dashboard-layout'
import { Header } from '@/components/layout/header'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Badge } from '@/components/ui/badge'
import { useToast } from '@/hooks/use-toast'
import { ApiError, apiFetch } from '@/lib/api'
import {
  alertLabel,
  alertTone,
  goalProgress,
  money,
  statusLabel,
  type AccountOption,
  type GoalResponse,
} from '@/lib/goals'
import { cn } from '@/lib/utils'
import { ArrowRight, CalendarClock, Plus, Target, Wallet } from 'lucide-react'

const NO_ACCOUNT = '__none__'

export default function GoalsPage() {
  const { toast } = useToast()
  const [goals, setGoals] = useState<GoalResponse[]>([])
  const [accounts, setAccounts] = useState<AccountOption[]>([])
  const [loading, setLoading] = useState(true)
  const [open, setOpen] = useState(false)
  const [creating, setCreating] = useState(false)

  const [name, setName] = useState('')
  const [targetAmount, setTargetAmount] = useState('')
  const [deadline, setDeadline] = useState('')
  const [linkedAccountId, setLinkedAccountId] = useState(NO_ACCOUNT)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const [goalRes, accountRes] = await Promise.all([
        apiFetch<GoalResponse[]>('/api/goals', { method: 'GET' }),
        apiFetch<AccountOption[]>('/api/accounts', { method: 'GET' }),
      ])
      setGoals(goalRes.data || [])
      setAccounts(accountRes.data || [])
    } catch (err) {
      toast({
        title: 'Không tải được mục tiêu',
        description: err instanceof ApiError ? err.message : 'Vui lòng thử lại.',
        variant: 'destructive',
      })
    } finally {
      setLoading(false)
    }
  }, [toast])

  useEffect(() => {
    load()
  }, [load])

  const personalAccounts = useMemo(
    () => accounts.filter(a => (a.scope == null || a.scope === 'PERSONAL') && a.familyId == null),
    [accounts]
  )

  const totalTarget = goals.reduce((sum, goal) => sum + Number(goal.targetAmount || 0), 0)
  const totalSaved = goals.reduce((sum, goal) => sum + Number(goal.currentAmount || 0), 0)
  const behindCount = goals.filter(g => g.alertStatus === 'SIGNIFICANTLY_BEHIND').length

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault()
    const amount = Number(targetAmount)
    if (!name.trim() || !deadline || !Number.isFinite(amount) || amount < 1000) {
      toast({
        title: 'Thiếu thông tin',
        description: 'Vui lòng nhập tên, số tiền mục tiêu và deadline hợp lệ.',
        variant: 'destructive',
      })
      return
    }

    setCreating(true)
    try {
      await apiFetch<GoalResponse>('/api/goals', {
        method: 'POST',
        body: JSON.stringify({
          name: name.trim(),
          targetAmount: amount,
          deadline,
          linkedAccountId: linkedAccountId === NO_ACCOUNT ? null : Number(linkedAccountId),
        }),
      })
      toast({ title: 'Đã tạo mục tiêu' })
      setOpen(false)
      setName('')
      setTargetAmount('')
      setDeadline('')
      setLinkedAccountId(NO_ACCOUNT)
      await load()
    } catch (err) {
      toast({
        title: 'Tạo mục tiêu thất bại',
        description: err instanceof ApiError ? err.message : 'Vui lòng thử lại.',
        variant: 'destructive',
      })
    } finally {
      setCreating(false)
    }
  }

  return (
    <DashboardLayout>
      <Header title="Mục tiêu tài chính" subtitle="Lập kế hoạch và theo dõi tiến độ tiết kiệm" />

      <div className="p-6">
        <div className="mb-6 grid gap-4 sm:grid-cols-3">
          <div className="rounded-xl border border-border bg-card p-5">
            <p className="text-sm text-muted-foreground">Tổng mục tiêu</p>
            <p className="mt-1 text-2xl font-bold tabular-nums">{money(totalTarget)}</p>
          </div>
          <div className="rounded-xl border border-border bg-card p-5">
            <p className="text-sm text-muted-foreground">Đã tích lũy</p>
            <p className="mt-1 text-2xl font-bold tabular-nums text-success">{money(totalSaved)}</p>
          </div>
          <div className="rounded-xl border border-border bg-card p-5">
            <p className="text-sm text-muted-foreground">Cần chú ý</p>
            <p className={cn('mt-1 text-2xl font-bold tabular-nums', behindCount ? 'text-destructive' : 'text-foreground')}>
              {behindCount}
            </p>
          </div>
        </div>

        <div className="mb-6 flex justify-end">
          <Dialog open={open} onOpenChange={setOpen}>
            <DialogTrigger asChild>
              <Button className="gap-2">
                <Plus className="h-4 w-4" />
                Tạo mục tiêu
              </Button>
            </DialogTrigger>
            <DialogContent className="sm:max-w-lg">
              <DialogHeader>
                <DialogTitle>Tạo mục tiêu mới</DialogTitle>
              </DialogHeader>
              <form className="mt-4 space-y-4" onSubmit={handleCreate}>
                <div className="space-y-2">
                  <Label htmlFor="goal-name">Tên mục tiêu</Label>
                  <Input
                    id="goal-name"
                    value={name}
                    onChange={e => setName(e.target.value)}
                    placeholder="Mua xe may Honda Vision"
                  />
                </div>
                <div className="grid gap-4 sm:grid-cols-2">
                  <div className="space-y-2">
                    <Label htmlFor="target">Số tiền cần</Label>
                    <Input
                      id="target"
                      type="number"
                      min={1000}
                      value={targetAmount}
                      onChange={e => setTargetAmount(e.target.value)}
                      placeholder="30000000"
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="deadline">Deadline</Label>
                    <Input
                      id="deadline"
                      type="date"
                      value={deadline}
                      onChange={e => setDeadline(e.target.value)}
                    />
                  </div>
                </div>
                <div className="space-y-2">
                  <Label> Tài khoản tiết kiệm liên kết</Label>
                  <Select value={linkedAccountId} onValueChange={setLinkedAccountId}>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value={NO_ACCOUNT}>Chưa liên kết</SelectItem>
                      {personalAccounts.map(account => (
                        <SelectItem key={account.id} value={String(account.id)}>
                          {account.name} - {money(account.balance)}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div className="flex gap-3 pt-2">
                  <Button type="button" variant="outline" className="flex-1" onClick={() => setOpen(false)}>
                    Huy
                  </Button>
                  <Button type="submit" className="flex-1" disabled={creating}>
                    {creating ? 'Đang lưu...' : 'Lưu mục tiêu'}
                  </Button>
                </div>
              </form>
            </DialogContent>
          </Dialog>
        </div>

        {loading ? (
          <p className="text-sm text-muted-foreground">Đang tải...</p>
        ) : goals.length === 0 ? (
          <div className="rounded-xl border border-dashed border-border bg-card p-8 text-center">
            <Target className="mx-auto h-10 w-10 text-muted-foreground" />
            <p className="mt-3 font-medium">Chưa có mục tiêu nào</p>
            <p className="mt-1 text-sm text-muted-foreground">Tạo mục tiêu đầu tiên để AI lập kế hoạch tiết kiệm.</p>
          </div>
        ) : (
          <div className="grid gap-4 lg:grid-cols-2">
            {goals.map(goal => {
              const progress = goalProgress(goal)
              return (
                <Link
                  key={goal.id}
                  href={`/goals/${goal.id}`}
                  className="rounded-xl border border-border bg-card p-5 shadow-sm transition hover:shadow-md"
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <h3 className="truncate font-semibold text-foreground">{goal.name}</h3>
                      <p className="mt-1 flex items-center gap-1 text-sm text-muted-foreground">
                        <CalendarClock className="h-4 w-4" />
                        {goal.monthsRemaining} tháng còn lại
                      </p>
                    </div>
                    <Badge className={cn('border', alertTone(goal.alertStatus))}>
                      {alertLabel(goal.alertStatus)}
                    </Badge>
                  </div>

                  <div className="mt-5">
                    <div className="mb-2 flex items-center justify-between text-sm">
                      <span className="font-medium">{money(goal.currentAmount)} / {money(goal.targetAmount)}</span>
                      <span className="tabular-nums">{progress.toFixed(1)}%</span>
                    </div>
                    <div className="h-2 w-full overflow-hidden rounded-full bg-secondary">
                      <div className="h-full rounded-full bg-primary" style={{ width: `${progress}%` }} />
                    </div>
                  </div>

                  <div className="mt-4 flex flex-wrap items-center justify-between gap-3 text-sm">
                    <span className="text-muted-foreground">
                      Cần {money(goal.monthlyNeeded)}/tháng
                    </span>
                    <span className="inline-flex items-center gap-1 font-medium text-primary">
                      Chi tiết  <ArrowRight className="h-4 w-4" />
                    </span>
                  </div>
                  {goal.linkedAccountName && (
                    <p className="mt-3 flex items-center gap-1 text-xs text-muted-foreground">
                      <Wallet className="h-3.5 w-3.5" />
                      {goal.linkedAccountName}
                    </p>
                  )}
                </Link>
              )
            })}
          </div>
        )}
      </div>
    </DashboardLayout>
  )
}
