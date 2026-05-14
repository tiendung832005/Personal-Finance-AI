'use client'

import { useState } from 'react'
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
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { Progress } from '@/components/ui/progress'
import { Badge } from '@/components/ui/badge'
import { cn } from '@/lib/utils'
import { family, formatCurrency, budgets, getCategoryById, transactions, insights } from '@/lib/mock-data'
import {
  Users,
  Plus,
  Copy,
  Check,
  TrendingUp,
  TrendingDown,
  Sparkles,
  Crown,
  AlertTriangle,
  Lightbulb,
  Utensils,
  Car,
  ShoppingBag,
  FileText,
  Gamepad2,
  MoreHorizontal,
} from 'lucide-react'
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  Cell,
} from 'recharts'

const iconMap: Record<string, React.ElementType> = {
  utensils: Utensils,
  car: Car,
  'shopping-bag': ShoppingBag,
  'file-text': FileText,
  'gamepad-2': Gamepad2,
  'more-horizontal': MoreHorizontal,
}

// Simulating whether user has family or not
const hasFamily = true

export default function FamilyPage() {
  const [isCreateOpen, setIsCreateOpen] = useState(false)
  const [isJoinOpen, setIsJoinOpen] = useState(false)
  const [copied, setCopied] = useState(false)

  const handleCopyCode = () => {
    navigator.clipboard.writeText(family.inviteCode)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  const totalFamilyExpense = family.members.reduce(
    (sum, m) => sum + m.totalSpent,
    0
  )

  const memberChartData = family.members.map(m => ({
    name: m.name.split(' ').slice(-1)[0],
    amount: m.totalSpent,
    fullName: m.name,
  }))

  const chartColors = ['#4A7C59', '#A0522D', '#C49A3C', '#6B7FA3', '#8B6F8B']

  // Onboarding state (no family)
  if (!hasFamily) {
    return (
      <DashboardLayout>
        <Header
          title="Tài chính gia đình"
          subtitle="Quản lý chi tiêu chung với gia đình"
        />

        <div className="flex min-h-[calc(100vh-64px)] items-center justify-center p-6">
          <div className="max-w-md text-center">
            <div className="mx-auto mb-6 flex h-20 w-20 items-center justify-center rounded-full bg-secondary">
              <Users className="h-10 w-10 text-muted-foreground" />
            </div>
            <h2 className="text-2xl font-bold text-foreground">
              Chưa có nhóm gia đình
            </h2>
            <p className="mt-2 text-muted-foreground">
              Tạo nhóm gia đình mới hoặc tham gia nhóm đã có để cùng quản lý tài
              chính chung.
            </p>

            <div className="mt-8 flex flex-col gap-3 sm:flex-row sm:justify-center">
              <Dialog open={isCreateOpen} onOpenChange={setIsCreateOpen}>
                <DialogTrigger asChild>
                  <Button className="gap-2">
                    <Plus className="h-4 w-4" />
                    Tạo nhóm mới
                  </Button>
                </DialogTrigger>
                <DialogContent className="sm:max-w-md">
                  <DialogHeader>
                    <DialogTitle>Tạo nhóm gia đình</DialogTitle>
                  </DialogHeader>
                  <form className="space-y-4 mt-4">
                    <div className="space-y-2">
                      <Label htmlFor="familyName">Tên nhóm gia đình</Label>
                      <Input
                        id="familyName"
                        placeholder="VD: Gia đình Nguyễn"
                      />
                    </div>
                    <div className="flex gap-3 pt-2">
                      <Button
                        type="button"
                        variant="outline"
                        className="flex-1"
                        onClick={() => setIsCreateOpen(false)}
                      >
                        Hủy
                      </Button>
                      <Button type="submit" className="flex-1">
                        Tạo nhóm
                      </Button>
                    </div>
                  </form>
                </DialogContent>
              </Dialog>

              <Dialog open={isJoinOpen} onOpenChange={setIsJoinOpen}>
                <DialogTrigger asChild>
                  <Button variant="outline" className="gap-2">
                    <Users className="h-4 w-4" />
                    Nhập mã mời
                  </Button>
                </DialogTrigger>
                <DialogContent className="sm:max-w-md">
                  <DialogHeader>
                    <DialogTitle>Tham gia nhóm gia đình</DialogTitle>
                  </DialogHeader>
                  <form className="space-y-4 mt-4">
                    <div className="space-y-2">
                      <Label htmlFor="inviteCode">Mã mời</Label>
                      <Input
                        id="inviteCode"
                        placeholder="Nhập mã mời từ chủ nhóm"
                        className="text-center text-lg tracking-widest"
                      />
                    </div>
                    <div className="flex gap-3 pt-2">
                      <Button
                        type="button"
                        variant="outline"
                        className="flex-1"
                        onClick={() => setIsJoinOpen(false)}
                      >
                        Hủy
                      </Button>
                      <Button type="submit" className="flex-1">
                        Tham gia
                      </Button>
                    </div>
                  </form>
                </DialogContent>
              </Dialog>
            </div>
          </div>
        </div>
      </DashboardLayout>
    )
  }

  // Family dashboard
  return (
    <DashboardLayout>
      <Header
        title="Tài chính gia đình"
        subtitle={family.name}
      />

      <div className="p-6">
        {/* Family header */}
        <div className="mb-6 flex flex-wrap items-center justify-between gap-4 rounded-xl border border-border bg-card p-5">
          <div className="flex items-center gap-4">
            <div className="flex -space-x-2">
              {family.members.map((member, index) => (
                <Avatar
                  key={member.id}
                  className="h-10 w-10 border-2 border-card"
                  style={{ zIndex: family.members.length - index }}
                >
                  <AvatarFallback className="bg-secondary text-sm">
                    {member.name
                      .split(' ')
                      .map(n => n[0])
                      .join('')
                      .slice(-2)}
                  </AvatarFallback>
                </Avatar>
              ))}
            </div>
            <div>
              <h3 className="font-semibold text-foreground">{family.name}</h3>
              <p className="text-sm text-muted-foreground">
                {family.members.length} thành viên
              </p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <div className="flex items-center gap-2 rounded-lg border border-border bg-secondary/50 px-3 py-2">
              <span className="text-sm text-muted-foreground">Mã mời:</span>
              <code className="font-mono font-semibold text-foreground">
                {family.inviteCode}
              </code>
              <Button
                variant="ghost"
                size="icon"
                className="h-7 w-7"
                onClick={handleCopyCode}
              >
                {copied ? (
                  <Check className="h-4 w-4 text-success" />
                ) : (
                  <Copy className="h-4 w-4" />
                )}
              </Button>
            </div>
          </div>
        </div>

        {/* Summary cards */}
        <div className="mb-6 grid gap-4 sm:grid-cols-3">
          <div className="rounded-xl border border-border bg-card p-5">
            <p className="text-sm text-muted-foreground">Tổng thu gia đình</p>
            <p className="text-2xl font-bold tabular-nums text-success">
              +{formatCurrency(45000000)}
            </p>
            <p className="mt-1 text-xs text-muted-foreground">Tháng này</p>
          </div>
          <div className="rounded-xl border border-border bg-card p-5">
            <p className="text-sm text-muted-foreground">Tổng chi gia đình</p>
            <p className="text-2xl font-bold tabular-nums text-destructive">
              -{formatCurrency(totalFamilyExpense)}
            </p>
            <p className="mt-1 text-xs text-muted-foreground">Tháng này</p>
          </div>
          <div className="rounded-xl border border-border bg-card p-5">
            <p className="text-sm text-muted-foreground">Số dư chung</p>
            <p className="text-2xl font-bold tabular-nums text-foreground">
              {formatCurrency(45000000 - totalFamilyExpense)}
            </p>
            <p className="mt-1 text-xs text-muted-foreground">Còn lại</p>
          </div>
        </div>

        <div className="grid gap-6 lg:grid-cols-2">
          {/* Member spending chart */}
          <div className="rounded-xl border border-border bg-card p-5">
            <h3 className="font-semibold text-foreground">
              Chi tiêu theo thành viên
            </h3>
            <p className="text-sm text-muted-foreground">Tháng này</p>

            <div className="mt-4 h-64">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart
                  data={memberChartData}
                  layout="vertical"
                  margin={{ left: 20, right: 20 }}
                >
                  <XAxis
                    type="number"
                    tick={{ fill: 'var(--muted-foreground)', fontSize: 12 }}
                    tickFormatter={value => `${(value / 1000000).toFixed(0)}M`}
                    axisLine={false}
                    tickLine={false}
                  />
                  <YAxis
                    type="category"
                    dataKey="name"
                    tick={{ fill: 'var(--foreground)', fontSize: 12 }}
                    axisLine={false}
                    tickLine={false}
                    width={60}
                  />
                  <Tooltip
                    content={({ active, payload }) => {
                      if (active && payload && payload.length) {
                        const data = payload[0].payload
                        return (
                          <div className="rounded-lg border border-border bg-card px-3 py-2 shadow-lg">
                            <p className="font-medium text-foreground">
                              {data.fullName}
                            </p>
                            <p className="text-sm text-muted-foreground">
                              {formatCurrency(data.amount)}
                            </p>
                          </div>
                        )
                      }
                      return null
                    }}
                  />
                  <Bar dataKey="amount" radius={[0, 4, 4, 0]}>
                    {memberChartData.map((entry, index) => (
                      <Cell
                        key={`cell-${index}`}
                        fill={chartColors[index % chartColors.length]}
                      />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </div>

            {/* Member list */}
            <div className="mt-4 space-y-2">
              {family.members.map((member, index) => (
                <div
                  key={member.id}
                  className="flex items-center justify-between rounded-lg bg-secondary/50 px-3 py-2"
                >
                  <div className="flex items-center gap-3">
                    <Avatar className="h-8 w-8">
                      <AvatarFallback
                        className="text-xs"
                        style={{
                          backgroundColor: `${chartColors[index]}20`,
                          color: chartColors[index],
                        }}
                      >
                        {member.name
                          .split(' ')
                          .map(n => n[0])
                          .join('')
                          .slice(-2)}
                      </AvatarFallback>
                    </Avatar>
                    <span className="text-sm font-medium text-foreground">
                      {member.name}
                    </span>
                    {member.role === 'owner' && (
                      <Crown className="h-4 w-4 text-warning" />
                    )}
                  </div>
                  <span className="text-sm tabular-nums text-muted-foreground">
                    {formatCurrency(member.totalSpent)}
                  </span>
                </div>
              ))}
            </div>
          </div>

          {/* Family budgets */}
          <div className="rounded-xl border border-border bg-card p-5">
            <h3 className="font-semibold text-foreground">Ngân sách chung</h3>
            <p className="text-sm text-muted-foreground">Theo danh mục</p>

            <div className="mt-4 space-y-4">
              {budgets.slice(0, 5).map(budget => {
                const category = getCategoryById(budget.categoryId)
                const Icon = category
                  ? iconMap[category.icon] || MoreHorizontal
                  : MoreHorizontal
                const percentage = Math.round(
                  (budget.spent / budget.amount) * 100
                )

                return (
                  <div key={budget.id}>
                    <div className="flex items-center justify-between mb-2">
                      <div className="flex items-center gap-2">
                        <div
                          className="flex h-8 w-8 items-center justify-center rounded-lg"
                          style={{ backgroundColor: `${category?.color}15` }}
                        >
                          <Icon
                            className="h-4 w-4"
                            style={{ color: category?.color }}
                          />
                        </div>
                        <span className="text-sm font-medium text-foreground">
                          {category?.name}
                        </span>
                      </div>
                      <span className="text-sm tabular-nums text-muted-foreground">
                        {formatCurrency(budget.spent)} /{' '}
                        {formatCurrency(budget.amount)}
                      </span>
                    </div>
                    <div className="h-2 w-full overflow-hidden rounded-full bg-secondary">
                      <div
                        className={cn(
                          'h-full transition-all',
                          percentage >= 90
                            ? 'bg-destructive'
                            : percentage >= 70
                            ? 'bg-warning'
                            : 'bg-success'
                        )}
                        style={{ width: `${Math.min(percentage, 100)}%` }}
                      />
                    </div>
                  </div>
                )
              })}
            </div>
          </div>

          {/* Recent family transactions */}
          <div className="rounded-xl border border-border bg-card p-5 lg:col-span-2">
            <h3 className="mb-4 font-semibold text-foreground">
              Giao dịch chung gần đây
            </h3>
            <div className="divide-y divide-border">
              {transactions.slice(0, 5).map(tx => {
                const category = getCategoryById(tx.categoryId)
                const Icon = category
                  ? iconMap[category.icon] || MoreHorizontal
                  : MoreHorizontal
                const member =
                  family.members[Math.floor(Math.random() * family.members.length)]

                return (
                  <div
                    key={tx.id}
                    className="flex items-center gap-4 py-3 first:pt-0 last:pb-0"
                  >
                    <div
                      className="flex h-10 w-10 items-center justify-center rounded-lg"
                      style={{ backgroundColor: `${category?.color}15` }}
                    >
                      <Icon
                        className="h-5 w-5"
                        style={{ color: category?.color }}
                      />
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="truncate font-medium text-foreground">
                        {tx.description}
                      </p>
                      <p className="text-sm text-muted-foreground">
                        {member.name.split(' ').slice(-2).join(' ')} •{' '}
                        {category?.name}
                      </p>
                    </div>
                    <p
                      className={cn(
                        'font-semibold tabular-nums',
                        tx.type === 'income'
                          ? 'text-success'
                          : 'text-destructive'
                      )}
                    >
                      {tx.type === 'income' ? '+' : '-'}
                      {formatCurrency(tx.amount)}
                    </p>
                  </div>
                )
              })}
            </div>
          </div>

          {/* Family AI Insight */}
          <div className="rounded-xl border border-border bg-card p-5 lg:col-span-2">
            <div className="flex items-center gap-2 mb-4">
              <Sparkles className="h-5 w-5 text-primary" />
              <h3 className="font-semibold text-foreground">
                AI Insight cho gia đình
              </h3>
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="flex items-start gap-3 rounded-lg border border-warning/30 bg-warning/10 p-4">
                <AlertTriangle className="h-5 w-5 text-warning shrink-0 mt-0.5" />
                <div>
                  <h4 className="font-medium text-foreground">
                    Chi tiêu gia đình tăng 15%
                  </h4>
                  <p className="mt-1 text-sm text-muted-foreground">
                    So với tháng trước, chủ yếu từ danh mục Ăn uống và Mua sắm.
                    Xem xét điều chỉnh ngân sách.
                  </p>
                </div>
              </div>
              <div className="flex items-start gap-3 rounded-lg border border-primary/20 bg-primary/5 p-4">
                <Lightbulb className="h-5 w-5 text-primary shrink-0 mt-0.5" />
                <div>
                  <h4 className="font-medium text-foreground">
                    Gợi ý tiết kiệm chung
                  </h4>
                  <p className="mt-1 text-sm text-muted-foreground">
                    Đặt mục tiêu tiết kiệm 3.000.000₫/tháng cho quỹ gia đình.
                    Hiện tại đang đạt 72%.
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </DashboardLayout>
  )
}
