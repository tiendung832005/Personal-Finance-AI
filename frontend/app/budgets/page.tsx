'use client'

import { useState } from 'react'
import { DashboardLayout } from '@/components/layout/dashboard-layout'
import { Header } from '@/components/layout/header'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import { Progress } from '@/components/ui/progress'
import { cn } from '@/lib/utils'
import { budgets, categories, getCategoryById, formatCurrency } from '@/lib/mock-data'
import {
  Plus,
  ChevronLeft,
  ChevronRight,
  Utensils,
  Car,
  ShoppingBag,
  FileText,
  Gamepad2,
  HeartPulse,
  MoreHorizontal,
  Edit,
} from 'lucide-react'

const iconMap: Record<string, React.ElementType> = {
  utensils: Utensils,
  car: Car,
  'shopping-bag': ShoppingBag,
  'file-text': FileText,
  'gamepad-2': Gamepad2,
  'heart-pulse': HeartPulse,
  'more-horizontal': MoreHorizontal,
}

export default function BudgetsPage() {
  const [isAddOpen, setIsAddOpen] = useState(false)
  const [currentMonth, setCurrentMonth] = useState(new Date())

  const monthLabel = currentMonth.toLocaleDateString('vi-VN', {
    month: 'long',
    year: 'numeric',
  })

  const goToPrevMonth = () => {
    setCurrentMonth(
      new Date(currentMonth.getFullYear(), currentMonth.getMonth() - 1)
    )
  }

  const goToNextMonth = () => {
    setCurrentMonth(
      new Date(currentMonth.getFullYear(), currentMonth.getMonth() + 1)
    )
  }

  const totalBudget = budgets.reduce((sum, b) => sum + b.amount, 0)
  const totalSpent = budgets.reduce((sum, b) => sum + b.spent, 0)
  const totalRemaining = totalBudget - totalSpent
  const overallPercentage = Math.round((totalSpent / totalBudget) * 100)

  const getProgressColor = (percentage: number) => {
    if (percentage >= 90) return 'bg-destructive'
    if (percentage >= 70) return 'bg-warning'
    return 'bg-success'
  }

  const getStatusText = (percentage: number) => {
    if (percentage >= 100) return 'Vượt ngân sách'
    if (percentage >= 90) return 'Gần hết'
    if (percentage >= 70) return 'Cẩn thận'
    return 'Ổn định'
  }

  const getStatusColor = (percentage: number) => {
    if (percentage >= 90) return 'text-destructive'
    if (percentage >= 70) return 'text-warning'
    return 'text-success'
  }

  return (
    <DashboardLayout>
      <Header title="Ngân sách" subtitle="Quản lý ngân sách theo danh mục" />

      <div className="p-6">
        {/* Month navigation */}
        <div className="mb-6 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="icon"
              onClick={goToPrevMonth}
            >
              <ChevronLeft className="h-4 w-4" />
            </Button>
            <span className="min-w-[140px] text-center font-semibold text-foreground capitalize">
              {monthLabel}
            </span>
            <Button
              variant="outline"
              size="icon"
              onClick={goToNextMonth}
            >
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>

          <Dialog open={isAddOpen} onOpenChange={setIsAddOpen}>
            <DialogTrigger asChild>
              <Button className="gap-2">
                <Plus className="h-4 w-4" />
                Thêm ngân sách
              </Button>
            </DialogTrigger>
            <DialogContent className="sm:max-w-md">
              <DialogHeader>
                <DialogTitle>Thêm ngân sách mới</DialogTitle>
              </DialogHeader>
              <form className="space-y-4 mt-4">
                <div className="space-y-2">
                  <Label>Danh mục</Label>
                  <Select>
                    <SelectTrigger>
                      <SelectValue placeholder="Chọn danh mục" />
                    </SelectTrigger>
                    <SelectContent>
                      {categories
                        .filter(c => c.type === 'expense')
                        .map(cat => (
                          <SelectItem key={cat.id} value={cat.id}>
                            {cat.name}
                          </SelectItem>
                        ))}
                    </SelectContent>
                  </Select>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="amount">Ngân sách hàng tháng</Label>
                  <Input
                    id="amount"
                    type="number"
                    placeholder="0"
                    className="font-semibold"
                  />
                </div>

                <div className="flex gap-3 pt-2">
                  <Button
                    type="button"
                    variant="outline"
                    className="flex-1"
                    onClick={() => setIsAddOpen(false)}
                  >
                    Hủy
                  </Button>
                  <Button type="submit" className="flex-1">
                    Lưu ngân sách
                  </Button>
                </div>
              </form>
            </DialogContent>
          </Dialog>
        </div>

        {/* Overall summary */}
        <div className="mb-6 rounded-xl border border-border bg-card p-5">
          <div className="flex flex-wrap items-center justify-between gap-4">
            <div>
              <p className="text-sm text-muted-foreground">Tổng ngân sách</p>
              <p className="text-2xl font-bold tabular-nums text-foreground">
                {formatCurrency(totalBudget)}
              </p>
            </div>
            <div className="h-10 w-px bg-border hidden sm:block" />
            <div>
              <p className="text-sm text-muted-foreground">Đã chi</p>
              <p className="text-2xl font-bold tabular-nums text-destructive">
                {formatCurrency(totalSpent)}
              </p>
            </div>
            <div className="h-10 w-px bg-border hidden sm:block" />
            <div>
              <p className="text-sm text-muted-foreground">Còn lại</p>
              <p
                className={cn(
                  'text-2xl font-bold tabular-nums',
                  totalRemaining >= 0 ? 'text-success' : 'text-destructive'
                )}
              >
                {formatCurrency(totalRemaining)}
              </p>
            </div>
            <div className="w-full sm:w-auto sm:flex-1 sm:max-w-[200px]">
              <div className="flex items-center justify-between mb-2">
                <span className="text-sm text-muted-foreground">
                  Tiến độ tổng
                </span>
                <span
                  className={cn('text-sm font-medium', getStatusColor(overallPercentage))}
                >
                  {overallPercentage}%
                </span>
              </div>
              <div className="h-2 w-full overflow-hidden rounded-full bg-secondary">
                <div
                  className={cn('h-full transition-all', getProgressColor(overallPercentage))}
                  style={{ width: `${Math.min(overallPercentage, 100)}%` }}
                />
              </div>
            </div>
          </div>
        </div>

        {/* Budget cards */}
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {budgets.map(budget => {
            const category = getCategoryById(budget.categoryId)
            const Icon = category
              ? iconMap[category.icon] || MoreHorizontal
              : MoreHorizontal
            const percentage = Math.round((budget.spent / budget.amount) * 100)
            const remaining = budget.amount - budget.spent

            return (
              <div
                key={budget.id}
                className="rounded-xl border border-border bg-card p-5 shadow-sm"
              >
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-3">
                    <div
                      className="flex h-10 w-10 items-center justify-center rounded-lg"
                      style={{ backgroundColor: `${category?.color}15` }}
                    >
                      <Icon
                        className="h-5 w-5"
                        style={{ color: category?.color }}
                      />
                    </div>
                    <div>
                      <h3 className="font-semibold text-foreground">
                        {category?.name}
                      </h3>
                      <p
                        className={cn(
                          'text-sm font-medium',
                          getStatusColor(percentage)
                        )}
                      >
                        {getStatusText(percentage)}
                      </p>
                    </div>
                  </div>
                  <Button variant="ghost" size="icon" className="h-8 w-8">
                    <Edit className="h-4 w-4" />
                  </Button>
                </div>

                <div className="mt-4">
                  <div className="flex items-baseline justify-between">
                    <span className="text-lg font-bold tabular-nums text-foreground">
                      {formatCurrency(budget.spent)}
                    </span>
                    <span className="text-sm text-muted-foreground">
                      / {formatCurrency(budget.amount)}
                    </span>
                  </div>

                  <div className="mt-3 h-2 w-full overflow-hidden rounded-full bg-secondary">
                    <div
                      className={cn(
                        'h-full transition-all',
                        getProgressColor(percentage)
                      )}
                      style={{ width: `${Math.min(percentage, 100)}%` }}
                    />
                  </div>

                  <div className="mt-2 flex items-center justify-between text-sm">
                    <span className="text-muted-foreground">{percentage}%</span>
                    <span
                      className={cn(
                        'tabular-nums',
                        remaining >= 0
                          ? 'text-muted-foreground'
                          : 'text-destructive font-medium'
                      )}
                    >
                      Còn {formatCurrency(remaining)}
                    </span>
                  </div>
                </div>
              </div>
            )
          })}
        </div>
      </div>
    </DashboardLayout>
  )
}
