'use client'

import { useCallback, useEffect, useMemo, useState } from 'react'
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
import {
  budgetAmount,
  statusBarClass,
  statusLabelVi,
  statusTextClass,
  type BudgetRow,
  type BudgetStatusItem,
  type BudgetStatusResponse,
} from '@/lib/budget'
import {
  createGroupBudget,
  deleteGroupBudget,
  getGroupBudgetStatus,
  listGroupBudgets,
  monthKeyFromDate,
  updateGroupBudget,
} from '@/lib/group-finance'
import { num } from '@/lib/summary'
import { useToast } from '@/hooks/use-toast'
import { ChevronLeft, ChevronRight, Edit, Loader2, Plus, Trash2 } from 'lucide-react'

type CategoryOpt = { id: number; name: string; type: string }

type Props = {
  groupId: number
  isAdmin: boolean
}

export function GroupBudgetsTab({ groupId, isAdmin }: Props) {
  const { toast } = useToast()
  const [currentMonth, setCurrentMonth] = useState(new Date())
  const monthKey = useMemo(() => monthKeyFromDate(currentMonth), [currentMonth])

  const [budgets, setBudgets] = useState<BudgetRow[]>([])
  const [budgetStatus, setBudgetStatus] = useState<BudgetStatusResponse | null>(null)
  const [categories, setCategories] = useState<CategoryOpt[]>([])
  const [loading, setLoading] = useState(true)

  const [addOpen, setAddOpen] = useState(false)
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
      const [buds, catRes] = await Promise.all([
        listGroupBudgets(groupId, monthKey),
        apiFetch<CategoryOpt[]>('/api/categories', { method: 'GET' }),
      ])
      setBudgets(buds.map(b => ({ ...b, amount: Number(b.amount) })))
      setCategories(catRes.data ?? [])
      try {
        const st = await getGroupBudgetStatus(groupId, monthKey)
        setBudgetStatus(st)
      } catch {
        setBudgetStatus(null)
      }
    } catch (err) {
      const message = err instanceof ApiError ? err.message : 'Không tải được ngân sách'
      toast({ title: 'Lỗi', description: message, variant: 'destructive' })
      setBudgets([])
    } finally {
      setLoading(false)
    }
  }, [groupId, monthKey, toast])

  useEffect(() => {
    load()
  }, [load])

  const statusByCategory = useMemo(() => {
    const m = new Map<number, BudgetStatusItem>()
    for (const it of budgetStatus?.items ?? []) m.set(it.categoryId, it)
    return m
  }, [budgetStatus])

  const expenseCategories = useMemo(
    () => categories.filter(c => c.type === 'EXPENSE' || c.type === 'BOTH'),
    [categories]
  )

  const existingIds = useMemo(() => new Set(budgets.map(b => b.categoryId)), [budgets])
  const addable = useMemo(
    () => expenseCategories.filter(c => !existingIds.has(c.id)),
    [expenseCategories, existingIds]
  )

  const handleAdd = async (e: React.FormEvent) => {
    e.preventDefault()
    try {
      await createGroupBudget(groupId, {
        categoryId: Number(addCategoryId),
        amount: Number(addAmount),
        month: monthKey,
      })
      toast({ title: 'Đã đặt ngân sách chung' })
      setAddOpen(false)
      setAddCategoryId('')
      setAddAmount('')
      await load()
    } catch (err) {
      toast({
        title: 'Lỗi',
        description: err instanceof ApiError ? err.message : 'Thất bại',
        variant: 'destructive',
      })
    }
  }

  const handleEdit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!editing) return
    try {
      await updateGroupBudget(groupId, editing.id, Number(editAmount))
      toast({ title: 'Đã cập nhật' })
      setEditOpen(false)
      await load()
    } catch (err) {
      toast({
        title: 'Lỗi',
        description: err instanceof ApiError ? err.message : 'Thất bại',
        variant: 'destructive',
      })
    }
  }

  const handleDelete = async () => {
    if (!deleteTarget) return
    try {
      await deleteGroupBudget(groupId, deleteTarget.id)
      toast({ title: 'Đã xóa ngân sách' })
      setDeleteTarget(null)
      await load()
    } catch (err) {
      toast({
        title: 'Lỗi',
        description: err instanceof ApiError ? err.message : 'Thất bại',
        variant: 'destructive',
      })
    }
  }

  if (loading) {
    return (
      <div className="flex justify-center py-16">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2">
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
        {isAdmin && addable.length > 0 && (
          <Dialog open={addOpen} onOpenChange={setAddOpen}>
            <DialogTrigger asChild>
              <Button size="sm" className="gap-2">
                <Plus className="h-4 w-4" />
                Đặt ngân sách chung
              </Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>Ngân sách chung</DialogTitle>
              </DialogHeader>
              <form onSubmit={handleAdd} className="mt-4 space-y-4">
                <div className="space-y-2">
                  <Label>Danh mục</Label>
                  <Select value={addCategoryId} onValueChange={setAddCategoryId}>
                    <SelectTrigger>
                      <SelectValue placeholder="Chọn danh mục" />
                    </SelectTrigger>
                    <SelectContent>
                      {addable.map(c => (
                        <SelectItem key={c.id} value={String(c.id)}>
                          {c.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div className="space-y-2">
                  <Label>Số tiền</Label>
                  <Input
                    type="number"
                    min={1}
                    value={addAmount}
                    onChange={e => setAddAmount(e.target.value)}
                    required
                  />
                </div>
                <Button type="submit" className="w-full">
                  Lưu
                </Button>
              </form>
            </DialogContent>
          </Dialog>
        )}
      </div>

      {!isAdmin && (
        <p className="text-sm text-muted-foreground">
          Chỉ ADMIN mới đặt hoặc sửa ngân sách chung. Bạn có thể xem trạng thái chi tiêu.
        </p>
      )}

      {budgets.length === 0 ? (
        <div className="rounded-xl border border-dashed p-10 text-center text-muted-foreground">
          Chưa có ngân sách chung tháng này.
        </div>
      ) : (
        <div className="space-y-3">
          {budgets.map(b => {
            const st = statusByCategory.get(b.categoryId)
            const amount = st ? num(st.budgetAmount) : budgetAmount(b)
            const spent = st ? num(st.actualAmount) : 0
            const usedPct = st?.usedPercentage ?? (amount > 0 ? (spent / amount) * 100 : 0)
            const lineStatus = st?.status ?? 'OK'
            return (
              <div
                key={b.id}
                className="rounded-xl border border-border bg-card p-4 space-y-2"
              >
                <div className="flex justify-between items-start gap-2">
                  <div>
                    <p className="font-medium">{b.categoryName}</p>
                    <p className={cn('text-xs', statusTextClass(lineStatus))}>
                      {statusLabelVi(lineStatus)} · {Math.round(usedPct)}%
                    </p>
                  </div>
                  {isAdmin && (
                    <div className="flex gap-1">
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => {
                          setEditing(b)
                          setEditAmount(String(budgetAmount(b)))
                          setEditOpen(true)
                        }}
                      >
                        <Edit className="h-4 w-4" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon"
                        className="text-destructive"
                        onClick={() => setDeleteTarget(b)}
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </div>
                  )}
                </div>
                <div className="h-2 rounded-full bg-muted overflow-hidden">
                  <div
                    className={cn('h-full rounded-full transition-all', statusBarClass(lineStatus))}
                    style={{ width: `${Math.min(usedPct, 100)}%` }}
                  />
                </div>
                <div className="flex justify-between text-sm text-muted-foreground">
                  <span>Đã chi: {formatCurrency(spent)}</span>
                  <span>Ngân sách: {formatCurrency(amount)}</span>
                </div>
              </div>
            )
          })}
        </div>
      )}

      <Dialog open={editOpen} onOpenChange={setEditOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Sửa ngân sách</DialogTitle>
          </DialogHeader>
          <form onSubmit={handleEdit} className="mt-4 space-y-4">
            <Input
              type="number"
              min={1}
              value={editAmount}
              onChange={e => setEditAmount(e.target.value)}
              required
            />
            <Button type="submit" className="w-full">
              Cập nhật
            </Button>
          </form>
        </DialogContent>
      </Dialog>

      <AlertDialog open={!!deleteTarget} onOpenChange={o => !o && setDeleteTarget(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Xóa ngân sách?</AlertDialogTitle>
            <AlertDialogDescription>Hành động không thể hoàn tác.</AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Hủy</AlertDialogCancel>
            <AlertDialogAction onClick={handleDelete}>Xóa</AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}
