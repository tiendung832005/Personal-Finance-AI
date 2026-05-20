'use client'

import { useCallback, useEffect, useMemo, useState } from 'react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Badge } from '@/components/ui/badge'
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
import { formatCurrency, formatDate } from '@/lib/mock-data'
import { apiFetch, ApiError } from '@/lib/api'
import {
  createSharedTransaction,
  deleteSharedTransaction,
  listSharedAccounts,
  listSharedTransactions,
  monthKeyFromDate,
  type SharedAccount,
  type SharedTransaction,
  type TransactionType,
} from '@/lib/group-finance'
import { num } from '@/lib/summary'
import { useToast } from '@/hooks/use-toast'
import {
  ChevronLeft,
  ChevronRight,
  Loader2,
  Plus,
  Trash2,
  TrendingDown,
  TrendingUp,
} from 'lucide-react'

type Category = {
  id: number
  name: string
  type: string
}

type Props = {
  groupId: number
  isAdmin: boolean
  currentUserId: number | undefined
}

export function GroupTransactionsTab({ groupId, isAdmin, currentUserId }: Props) {
  const { toast } = useToast()
  const [currentMonth, setCurrentMonth] = useState(new Date())
  const monthKey = useMemo(() => monthKeyFromDate(currentMonth), [currentMonth])

  const [transactions, setTransactions] = useState<SharedTransaction[]>([])
  const [accounts, setAccounts] = useState<SharedAccount[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)

  const [open, setOpen] = useState(false)
  const [saving, setSaving] = useState(false)
  const [deleteTarget, setDeleteTarget] = useState<SharedTransaction | null>(null)

  const [amount, setAmount] = useState('')
  const [txType, setTxType] = useState<TransactionType>('EXPENSE')
  const [description, setDescription] = useState('')
  const [categoryId, setCategoryId] = useState('')
  const [accountId, setAccountId] = useState('')
  const [date, setDate] = useState(new Date().toISOString().split('T')[0])

  const monthLabel = currentMonth.toLocaleDateString('vi-VN', {
    month: 'long',
    year: 'numeric',
  })

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const [txPage, accRes, catRes] = await Promise.all([
        listSharedTransactions(groupId, { month: monthKey, page, size: 20 }),
        listSharedAccounts(groupId),
        apiFetch<Category[]>('/api/categories', { method: 'GET' }),
      ])
      setTransactions(txPage.content)
      setTotalPages(txPage.totalPages)
      setAccounts(accRes)
      setCategories(catRes.data ?? [])
    } catch (err) {
      const message = err instanceof ApiError ? err.message : 'Không tải được giao dịch'
      toast({ title: 'Lỗi', description: message, variant: 'destructive' })
      setTransactions([])
    } finally {
      setLoading(false)
    }
  }, [groupId, monthKey, page, toast])

  useEffect(() => {
    load()
  }, [load])

  useEffect(() => {
    setPage(0)
  }, [monthKey])

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!accountId) {
      toast({
        title: 'Chọn tài khoản',
        description: 'Cần có tài khoản chung trước khi nhập giao dịch.',
        variant: 'destructive',
      })
      return
    }
    setSaving(true)
    try {
      await createSharedTransaction(groupId, {
        accountId: Number(accountId),
        categoryId: categoryId ? Number(categoryId) : undefined,
        amount: Number(amount),
        type: txType,
        description: description.trim() || undefined,
        transactionDate: date,
      })
      toast({ title: 'Đã thêm giao dịch chung' })
      setOpen(false)
      setAmount('')
      setDescription('')
      await load()
    } catch (err) {
      const message = err instanceof ApiError ? err.message : 'Thêm thất bại'
      toast({ title: 'Lỗi', description: message, variant: 'destructive' })
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async () => {
    if (!deleteTarget) return
    try {
      await deleteSharedTransaction(groupId, deleteTarget.id)
      toast({ title: 'Đã xóa giao dịch' })
      setDeleteTarget(null)
      await load()
    } catch (err) {
      const message = err instanceof ApiError ? err.message : 'Xóa thất bại'
      toast({ title: 'Lỗi', description: message, variant: 'destructive' })
    }
  }

  const canDelete = (tx: SharedTransaction) =>
    isAdmin || (currentUserId != null && tx.createdByUserId === currentUserId)

  if (loading && transactions.length === 0) {
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

        <Dialog open={open} onOpenChange={setOpen}>
          <DialogTrigger asChild>
            <Button size="sm" className="gap-2" disabled={accounts.length === 0}>
              <Plus className="h-4 w-4" />
              Thêm giao dịch
            </Button>
          </DialogTrigger>
          <DialogContent className="max-w-md">
            <DialogHeader>
              <DialogTitle>Giao dịch chung</DialogTitle>
            </DialogHeader>
            <form onSubmit={handleCreate} className="mt-4 space-y-4">
              <div className="grid grid-cols-2 gap-2">
                <Button
                  type="button"
                  variant={txType === 'EXPENSE' ? 'default' : 'outline'}
                  onClick={() => setTxType('EXPENSE')}
                >
                  Chi
                </Button>
                <Button
                  type="button"
                  variant={txType === 'INCOME' ? 'default' : 'outline'}
                  onClick={() => setTxType('INCOME')}
                >
                  Thu
                </Button>
              </div>
              <div className="space-y-2">
                <Label>Tài khoản chung</Label>
                <Select value={accountId} onValueChange={setAccountId}>
                  <SelectTrigger>
                    <SelectValue placeholder="Chọn tài khoản" />
                  </SelectTrigger>
                  <SelectContent>
                    {accounts.map(a => (
                      <SelectItem key={a.id} value={String(a.id)}>
                        {a.name} ({formatCurrency(num(a.currentBalance ?? a.balance))})
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
                  value={amount}
                  onChange={e => setAmount(e.target.value)}
                  required
                />
              </div>
              <div className="space-y-2">
                <Label>Danh mục</Label>
                <Select value={categoryId} onValueChange={setCategoryId}>
                  <SelectTrigger>
                    <SelectValue placeholder="Tuỳ chọn" />
                  </SelectTrigger>
                  <SelectContent>
                    {categories.map(c => (
                      <SelectItem key={c.id} value={String(c.id)}>
                        {c.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label>Mô tả</Label>
                <Input value={description} onChange={e => setDescription(e.target.value)} />
              </div>
              <div className="space-y-2">
                <Label>Ngày</Label>
                <Input type="date" value={date} onChange={e => setDate(e.target.value)} required />
              </div>
              <Button type="submit" className="w-full" disabled={saving}>
                {saving && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                Lưu
              </Button>
            </form>
          </DialogContent>
        </Dialog>
      </div>

      {transactions.length === 0 ? (
        <div className="rounded-xl border border-dashed p-10 text-center text-muted-foreground">
          Chưa có giao dịch chung trong tháng này.
        </div>
      ) : (
        <ul className="rounded-xl border border-border bg-card divide-y divide-border">
          {transactions.map(tx => (
            <li key={tx.id} className="flex flex-wrap items-center gap-3 px-4 py-3">
              <div
                className={cn(
                  'flex h-10 w-10 shrink-0 items-center justify-center rounded-full',
                  tx.type === 'INCOME' ? 'bg-success/10 text-success' : 'bg-destructive/10 text-destructive'
                )}
              >
                {tx.type === 'INCOME' ? (
                  <TrendingUp className="h-5 w-5" />
                ) : (
                  <TrendingDown className="h-5 w-5" />
                )}
              </div>
              <div className="min-w-0 flex-1">
                <p className="font-medium truncate">
                  {tx.description || tx.categoryName || 'Giao dịch'}
                </p>
                <p className="text-xs text-muted-foreground">
                  {tx.accountName} · bởi {tx.createdByName} · {formatDate(tx.transactionDate)}
                </p>
              </div>
              <p
                className={cn(
                  'font-semibold tabular-nums',
                  tx.type === 'INCOME' ? 'text-success' : 'text-destructive'
                )}
              >
                {tx.type === 'EXPENSE' ? '−' : '+'}
                {formatCurrency(num(tx.amount))}
              </p>
              {canDelete(tx) && (
                <Button
                  variant="ghost"
                  size="icon"
                  className="text-destructive"
                  onClick={() => setDeleteTarget(tx)}
                >
                  <Trash2 className="h-4 w-4" />
                </Button>
              )}
            </li>
          ))}
        </ul>
      )}

      {totalPages > 1 && (
        <div className="flex justify-center gap-2">
          <Button
            variant="outline"
            size="sm"
            disabled={page <= 0}
            onClick={() => setPage(p => p - 1)}
          >
            Trang trước
          </Button>
          <span className="text-sm text-muted-foreground self-center">
            {page + 1} / {totalPages}
          </span>
          <Button
            variant="outline"
            size="sm"
            disabled={page >= totalPages - 1}
            onClick={() => setPage(p => p + 1)}
          >
            Trang sau
          </Button>
        </div>
      )}

      <AlertDialog open={!!deleteTarget} onOpenChange={o => !o && setDeleteTarget(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Xóa giao dịch?</AlertDialogTitle>
            <AlertDialogDescription>
              Giao dịch sẽ được ẩn khỏi danh sách nhóm (soft delete).
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Hủy</AlertDialogCancel>
            <AlertDialogAction onClick={handleDelete} className="bg-destructive text-destructive-foreground">
              Xóa
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}
