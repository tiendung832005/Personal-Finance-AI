'use client'

import { useCallback, useEffect, useMemo, useState } from 'react'
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
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { cn } from '@/lib/utils'
import { formatCurrency } from '@/lib/mock-data'
import { apiFetch, ApiError } from '@/lib/api'
import { budgetAmount, expenseTotalsByCategory, type BudgetRow } from '@/lib/budget'
import { useToast } from '@/hooks/use-toast'
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
  Trash2,
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

type CategoryOpt = {
  id: number
  name: string
  icon: string | null
  color: string | null
  type: string
}

function monthKeyFromDate(d: Date): string {
  const y = d.getFullYear()
  const m = d.getMonth() + 1
  return `${y}-${String(m).padStart(2, '0')}`
}

export default function BudgetsPage() {
  const { toast } = useToast()
  const [currentMonth, setCurrentMonth] = useState(new Date())
  const monthKey = useMemo(() => monthKeyFromDate(currentMonth), [currentMonth])

  const [budgets, setBudgets] = useState<BudgetRow[]>([])
  const [categories, setCategories] = useState<CategoryOpt[]>([])
  const [txMonth, setTxMonth] = useState<
    { amount: number; type: string; categoryId?: number | null; transactionDate?: string }[]
  >([])
  const [loading, setLoading] = useState(true)

  const [isAddOpen, setIsAddOpen] = useState(false)
  const [addCategoryId, setAddCategoryId] = useState('')
  const [addAmount, setAddAmount] = useState('')

  const [editOpen, setEditOpen] = useState(false)
  const [editing, setEditing] = useState<BudgetRow | null>(null)
  const [editAmount, setEditAmount] = useState('')

  const [deleteTarget, setDeleteTarget] = useState<BudgetRow | null>(null)

  const monthLabel = currentMonth.toLocaleDateString('vi-VN', {
    month: 'long',
    year: 'numeric',
  })

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const [budRes, catRes, txRes] = await Promise.all([
        apiFetch<BudgetRow[]>(`/api/budgets?month=${encodeURIComponent(monthKey)}`, {
          method: 'GET',
        }),
        apiFetch<CategoryOpt[]>('/api/categories', { method: 'GET' }),
        apiFetch<
          { amount: number | string; type: string; categoryId?: number | null; transactionDate?: string }[]
        >(`/api/transactions?month=${encodeURIComponent(monthKey)}`, { method: 'GET' }),
      ])
      setBudgets((budRes.data || []).map(b => ({ ...b, amount: Number(b.amount) })))
      setCategories(catRes.data || [])
      setTxMonth(
        (txRes.data || []).map(t => ({
          ...t,
          amount: Number(t.amount),
        }))
      )
    } catch (e) {
      toast({
        title: 'Không tải được ngân sách',
        description: e instanceof ApiError ? e.message : 'Thử đăng nhập lại.',
        variant: 'destructive',
      })
      setBudgets([])
      setCategories([])
      setTxMonth([])
    } finally {
      setLoading(false)
    }
  }, [monthKey, toast])

  useEffect(() => {
    load()
  }, [load])

  const spentByCat = useMemo(() => expenseTotalsByCategory(txMonth, monthKey), [txMonth, monthKey])

  const expenseCategories = useMemo(
    () => categories.filter(c => c.type === 'EXPENSE' || c.type === 'BOTH'),
    [categories]
  )

  const existingCategoryIds = useMemo(() => new Set(budgets.map(b => b.categoryId)), [budgets])
  const addableCategories = useMemo(
    () => expenseCategories.filter(c => !existingCategoryIds.has(c.id)),
    [expenseCategories, existingCategoryIds]
  )

  const rowsWithSpent = useMemo(
    () =>
      budgets.map(b => ({
        budget: b,
        spent: spentByCat.get(b.categoryId) || 0,
        amount: budgetAmount(b),
      })),
    [budgets, spentByCat]
  )

  const totalBudget = rowsWithSpent.reduce((s, r) => s + r.amount, 0)
  const totalSpent = rowsWithSpent.reduce((s, r) => s + r.spent, 0)
  const totalRemaining = totalBudget - totalSpent
  const overallPercentage = totalBudget > 0 ? Math.round((totalSpent / totalBudget) * 100) : 0

  const goToPrevMonth = () => {
    setCurrentMonth(new Date(currentMonth.getFullYear(), currentMonth.getMonth() - 1))
  }

  const goToNextMonth = () => {
    setCurrentMonth(new Date(currentMonth.getFullYear(), currentMonth.getMonth() + 1))
  }

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

  const openEdit = (b: BudgetRow) => {
    setEditing(b)
    setEditAmount(String(budgetAmount(b)))
    setEditOpen(true)
  }

  async function handleAddSubmit(e: React.FormEvent) {
    e.preventDefault()
    const catId = Number(addCategoryId)
    const amt = Number(String(addAmount).replace(/\s/g, '').replace(',', ''))
    if (!catId || !Number.isFinite(amt) || amt <= 0) {
      toast({
        title: 'Thiếu thông tin',
        description: 'Chọn danh mục và nhập số tiền hợp lệ.',
        variant: 'destructive',
      })
      return
    }
    try {
      await apiFetch<BudgetRow>('/api/budgets', {
        method: 'POST',
        body: JSON.stringify({ categoryId: catId, amount: amt, month: monthKey }),
      })
      toast({ title: 'Đã thêm ngân sách' })
      setIsAddOpen(false)
      setAddCategoryId('')
      setAddAmount('')
      await load()
    } catch (err) {
      toast({
        title: 'Không lưu được',
        description: err instanceof ApiError ? err.message : 'Lỗi không xác định',
        variant: 'destructive',
      })
    }
  }

  async function handleEditSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!editing) return
    const amt = Number(String(editAmount).replace(/\s/g, '').replace(',', ''))
    if (!Number.isFinite(amt) || amt <= 0) {
      toast({ title: 'Số tiền không hợp lệ', variant: 'destructive' })
      return
    }
    try {
      await apiFetch<BudgetRow>(`/api/budgets/${editing.id}`, {
        method: 'PUT',
        body: JSON.stringify({ amount: amt }),
      })
      toast({ title: 'Đã cập nhật ngân sách' })
      setEditOpen(false)
      setEditing(null)
      await load()
    } catch (err) {
      toast({
        title: 'Không lưu được',
        description: err instanceof ApiError ? err.message : 'Lỗi không xác định',
        variant: 'destructive',
      })
    }
  }

  async function confirmDelete() {
    if (!deleteTarget) return
    try {
      await apiFetch(`/api/budgets/${deleteTarget.id}`, { method: 'DELETE' })
      toast({ title: 'Đã xóa ngân sách' })
      setDeleteTarget(null)
      await load()
    } catch (err) {
      toast({
        title: 'Không xóa được',
        description: err instanceof ApiError ? err.message : 'Lỗi không xác định',
        variant: 'destructive',
      })
    }
  }

  return (
    <DashboardLayout>
      <Header title="Ngân sách" subtitle="Quản lý ngân sách theo danh mục" />

      <div className="p-6">
        <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <Button variant="outline" size="icon" onClick={goToPrevMonth}>
              <ChevronLeft className="h-4 w-4" />
            </Button>
            <span className="min-w-[140px] text-center font-semibold text-foreground capitalize">
              {monthLabel}
            </span>
            <Button variant="outline" size="icon" onClick={goToNextMonth}>
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>

          <Dialog open={isAddOpen} onOpenChange={setIsAddOpen}>
            <DialogTrigger asChild>
              <Button className="gap-2" disabled={addableCategories.length === 0 && !loading}>
                <Plus className="h-4 w-4" />
                Thêm ngân sách
              </Button>
            </DialogTrigger>
            <DialogContent className="sm:max-w-md">
              <DialogHeader>
                <DialogTitle>Thêm ngân sách mới</DialogTitle>
              </DialogHeader>
              <form className="mt-4 space-y-4" onSubmit={handleAddSubmit}>
                <div className="space-y-2">
                  <Label>Danh mục</Label>
                  <Select value={addCategoryId} onValueChange={setAddCategoryId}>
                    <SelectTrigger>
                      <SelectValue placeholder="Chọn danh mục" />
                    </SelectTrigger>
                    <SelectContent>
                      {addableCategories.map(cat => (
                        <SelectItem key={cat.id} value={String(cat.id)}>
                          {cat.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  {addableCategories.length === 0 && (
                    <p className="text-xs text-muted-foreground">
                      Mọi danh mục chi đã có ngân sách cho tháng này, hoặc chưa có danh mục.
                    </p>
                  )}
                </div>

                <div className="space-y-2">
                  <Label htmlFor="amount">Ngân sách (VNĐ)</Label>
                  <Input
                    id="amount"
                    type="number"
                    min={1}
                    placeholder="5000000"
                    className="font-semibold"
                    value={addAmount}
                    onChange={e => setAddAmount(e.target.value)}
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

        <div className="mb-6 rounded-xl border border-border bg-card p-5">
          <div className="flex flex-wrap items-center justify-between gap-4">
            <div>
              <p className="text-sm text-muted-foreground">Tổng ngân sách</p>
              <p className="text-2xl font-bold tabular-nums text-foreground">
                {loading ? '…' : formatCurrency(totalBudget)}
              </p>
            </div>
            <div className="hidden h-10 w-px bg-border sm:block" />
            <div>
              <p className="text-sm text-muted-foreground">Đã chi (theo giao dịch tháng)</p>
              <p className="text-2xl font-bold tabular-nums text-destructive">
                {loading ? '…' : formatCurrency(totalSpent)}
              </p>
            </div>
            <div className="hidden h-10 w-px bg-border sm:block" />
            <div>
              <p className="text-sm text-muted-foreground">Còn lại</p>
              <p
                className={cn(
                  'text-2xl font-bold tabular-nums',
                  totalRemaining >= 0 ? 'text-success' : 'text-destructive'
                )}
              >
                {loading ? '…' : formatCurrency(totalRemaining)}
              </p>
            </div>
            <div className="w-full sm:w-auto sm:max-w-[200px] sm:flex-1">
              <div className="mb-2 flex items-center justify-between">
                <span className="text-sm text-muted-foreground">Tiến độ tổng</span>
                <span className={cn('text-sm font-medium', getStatusColor(overallPercentage))}>
                  {loading ? '…' : `${overallPercentage}%`}
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

        {loading ? (
          <p className="text-sm text-muted-foreground">Đang tải…</p>
        ) : rowsWithSpent.length === 0 ? (
          <p className="text-sm text-muted-foreground">Chưa có ngân sách cho tháng này.</p>
        ) : (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {rowsWithSpent.map(({ budget, spent, amount }) => {
              const category = categories.find(c => c.id === budget.categoryId)
              const iconKey = String(category?.icon || '')
              const Icon = iconMap[iconKey] || MoreHorizontal
              const percentage = amount > 0 ? Math.round((spent / amount) * 100) : 0
              const remaining = amount - spent
              const color = category?.color || '#9E9890'

              return (
                <div
                  key={budget.id}
                  className="rounded-xl border border-border bg-card p-5 shadow-sm"
                >
                  <div className="flex items-start justify-between gap-2">
                    <div className="flex min-w-0 items-center gap-3">
                      <div
                        className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg"
                        style={{ backgroundColor: `${color}15` }}
                      >
                        <Icon className="h-5 w-5" style={{ color }} />
                      </div>
                      <div className="min-w-0">
                        <h3 className="font-semibold text-foreground">
                          {category?.name || budget.categoryName}
                        </h3>
                        <p className={cn('text-sm font-medium', getStatusColor(percentage))}>
                          {getStatusText(percentage)}
                        </p>
                      </div>
                    </div>
                    <div className="flex shrink-0 gap-1">
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-8 w-8"
                        type="button"
                        onClick={() => openEdit(budget)}
                      >
                        <Edit className="h-4 w-4" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-8 w-8 text-muted-foreground hover:text-destructive"
                        type="button"
                        onClick={() => setDeleteTarget(budget)}
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </div>
                  </div>

                  <div className="mt-4">
                    <div className="flex items-baseline justify-between">
                      <span className="text-lg font-bold tabular-nums text-foreground">
                        {formatCurrency(spent)}
                      </span>
                      <span className="text-sm text-muted-foreground">/ {formatCurrency(amount)}</span>
                    </div>

                    <div className="mt-3 h-2 w-full overflow-hidden rounded-full bg-secondary">
                      <div
                        className={cn('h-full transition-all', getProgressColor(percentage))}
                        style={{ width: `${Math.min(percentage, 100)}%` }}
                      />
                    </div>

                    <div className="mt-2 flex items-center justify-between text-sm">
                      <span className="text-muted-foreground">{percentage}%</span>
                      <span
                        className={cn(
                          'tabular-nums',
                          remaining >= 0 ? 'text-muted-foreground' : 'text-destructive font-medium'
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
        )}

        <Dialog
          open={editOpen}
          onOpenChange={o => {
            setEditOpen(o)
            if (!o) setEditing(null)
          }}
        >
          <DialogContent className="sm:max-w-md">
            <DialogHeader>
              <DialogTitle>Sửa ngân sách</DialogTitle>
            </DialogHeader>
            {editing && (
              <form className="mt-4 space-y-4" onSubmit={handleEditSubmit}>
                <p className="text-sm text-muted-foreground">{editing.categoryName}</p>
                <div className="space-y-2">
                  <Label htmlFor="edit-amount">Số tiền ngân sách (VNĐ)</Label>
                  <Input
                    id="edit-amount"
                    type="number"
                    min={1}
                    value={editAmount}
                    onChange={e => setEditAmount(e.target.value)}
                    className="font-semibold"
                  />
                </div>
                <div className="flex gap-3 pt-2">
                  <Button type="button" variant="outline" className="flex-1" onClick={() => setEditOpen(false)}>
                    Hủy
                  </Button>
                  <Button type="submit" className="flex-1">
                    Lưu
                  </Button>
                </div>
              </form>
            )}
          </DialogContent>
        </Dialog>

        <AlertDialog open={deleteTarget != null} onOpenChange={open => !open && setDeleteTarget(null)}>
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogTitle>Xóa ngân sách?</AlertDialogTitle>
              <AlertDialogDescription>
                {deleteTarget
                  ? `Xóa ngân sách "${deleteTarget.categoryName}" tháng ${deleteTarget.month}.`
                  : ''}
              </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel>Hủy</AlertDialogCancel>
              <Button
                variant="destructive"
                onClick={() => void confirmDelete()}
              >
                Xóa
              </Button>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>
      </div>
    </DashboardLayout>
  )
}
