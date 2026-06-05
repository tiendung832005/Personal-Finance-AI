'use client'

import { useEffect, useMemo, useState } from 'react'
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
import { Badge } from '@/components/ui/badge'
import { cn } from '@/lib/utils'
import {
  formatCurrency,
  formatDate,
} from '@/lib/mock-data'
import { apiFetch, ApiError } from '@/lib/api'
import { useToast } from '@/hooks/use-toast'
import {
  Plus,
  Upload,
  Filter,
  Utensils,
  Car,
  ShoppingBag,
  FileText,
  Gamepad2,
  HeartPulse,
  GraduationCap,
  MoreHorizontal,
  Briefcase,
  Gift,
  TrendingUp,
  PlusCircle,
  Sparkles,
  Search,
} from 'lucide-react'

const iconMap: Record<string, React.ElementType> = {
  utensils: Utensils,
  car: Car,
  'shopping-bag': ShoppingBag,
  'file-text': FileText,
  'gamepad-2': Gamepad2,
  'heart-pulse': HeartPulse,
  'graduation-cap': GraduationCap,
  'more-horizontal': MoreHorizontal,
  briefcase: Briefcase,
  gift: Gift,
  'trending-up': TrendingUp,
  'plus-circle': PlusCircle,
}

type Category = {
  id: number
  name: string
  icon: string | null
  type: 'EXPENSE' | 'INCOME' | 'BOTH'
  color: string | null
}

type Account = {
  id: number
  name: string
}

type Transaction = {
  id: number
  amount: number
  type: 'INCOME' | 'EXPENSE'
  description: string | null
  categoryId: number | null
  categoryName: string | null
  accountId: number
  transactionDate: string
  note: string | null
  isAutoCategorized: boolean
}

export default function TransactionsPage() {
  const { toast } = useToast()
  const [isAddOpen, setIsAddOpen] = useState(false)
  const [editTarget, setEditTarget] = useState<Transaction | null>(null)
  const [filterType, setFilterType] = useState<string>('all')
  const [filterCategory, setFilterCategory] = useState<string>('all')
  const [searchQuery, setSearchQuery] = useState('')

  const [transactions, setTransactions] = useState<Transaction[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [accounts, setAccounts] = useState<Account[]>([])
  const [loading, setLoading] = useState(true)

  const [amount, setAmount] = useState<string>('')
  const [txType, setTxType] = useState<'EXPENSE' | 'INCOME'>('EXPENSE')
  const [description, setDescription] = useState('')
  const [categoryId, setCategoryId] = useState<string>('') // string for Select
  const [accountId, setAccountId] = useState<string>('') // string for Select
  const [date, setDate] = useState<string>(new Date().toISOString().split('T')[0])
  const [note, setNote] = useState('')
  const [aiSuggestion, setAiSuggestion] = useState<any>(null)
  const [isLoadingAI, setIsLoadingAI] = useState(false)

  // T19: Frontend - AI Suggestion with Debounce
  useEffect(() => {
    // Clear stale suggestion whenever description changes
    setAiSuggestion(null)

    if (!description || description.trim().length < 2) {
      return
    }

    const timer = setTimeout(async () => {
      setIsLoadingAI(true)
      try {
        const res = await apiFetch<any>('/api/transactions/categorize', {
          method: 'POST',
          body: JSON.stringify({ description }),
        })
        const suggestion = res.data

        if (suggestion?.successful) {
          // Use functional update to check if we can safely overwrite the current category
          setAiSuggestion((prevAi: any) => {
            setCategoryId(currentCatId => {
              // Overwrite ONLY if category is empty OR it was set by the PREVIOUS AI suggestion
              const isAiDriven = prevAi && currentCatId === String(prevAi.categoryId);
              if (!currentCatId || isAiDriven) {
                return String(suggestion.categoryId);
              }
              return currentCatId;
            });
            return suggestion;
          });
        }
      } catch {
        // fail silently
      } finally {
        setIsLoadingAI(false)
      }
    }, 800)

    return () => clearTimeout(timer)
  }, [description])

  useEffect(() => {
    if (editTarget) {
      setAmount(String(editTarget.amount))
      setTxType(editTarget.type)
      setDescription(editTarget.description || '')
      setCategoryId(editTarget.categoryId ? String(editTarget.categoryId) : '')
      setAccountId(String(editTarget.accountId))
      setDate(editTarget.transactionDate)
      setNote(editTarget.note || '')
      setAiSuggestion(null)
    } else {
      setAmount('')
      setDescription('')
      setCategoryId('')
      setTxType('EXPENSE')
      setDate(new Date().toISOString().split('T')[0])
      setNote('')
      setAiSuggestion(null)
    }
  }, [editTarget])

  const loadAll = async () => {
    setLoading(true)
    try {
      const [txRes, catRes, accRes] = await Promise.all([
        apiFetch<Transaction[]>('/api/transactions', { method: 'GET' }),
        apiFetch<Category[]>('/api/categories', { method: 'GET' }),
        apiFetch<Account[]>('/api/accounts', { method: 'GET' }),
      ])
      setTransactions((txRes.data || []).map(t => ({ ...t, amount: Number(t.amount) })))
      setCategories(catRes.data || [])
      setAccounts(accRes.data || [])
      if (!accountId && (accRes.data || []).length > 0) setAccountId(String((accRes.data || [])[0].id))
    } catch (err: any) {
      toast({
        title: 'Không tải được dữ liệu',
        description: err instanceof ApiError ? err.message : 'Vui lòng đăng nhập lại.',
        variant: 'destructive',
      })
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadAll()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const getCategoryById = (id: number | null) =>
    id == null ? undefined : categories.find(c => c.id === id)
  const getAccountById = (id: number) => accounts.find(a => a.id === id)

  const filteredTransactions = useMemo(() => {
    return transactions.filter(t => {
      const matchesType =
        filterType === 'all' ||
        (filterType === 'income' && t.type === 'INCOME') ||
        (filterType === 'expense' && t.type === 'EXPENSE')
      const matchesCategory =
        filterCategory === 'all' || String(t.categoryId || '') === filterCategory
      const desc = (t.description || '').toLowerCase()
      const matchesSearch = desc.includes(searchQuery.toLowerCase())
      return matchesType && matchesCategory && matchesSearch
    })
  }, [transactions, filterType, filterCategory, searchQuery])

  const totalIncome = filteredTransactions
    .filter(t => t.type === 'INCOME')
    .reduce((sum, t) => sum + t.amount, 0)

  const totalExpense = filteredTransactions
    .filter(t => t.type === 'EXPENSE')
    .reduce((sum, t) => sum + t.amount, 0)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!accountId) {
      toast({ title: 'Thiếu tài khoản', description: 'Vui lòng chọn tài khoản.', variant: 'destructive' })
      return
    }
    const payload = {
      accountId: Number(accountId),
      categoryId: categoryId ? Number(categoryId) : null,
      amount: Number(amount),
      type: txType,
      description,
      transactionDate: date,
      note: note || null,
      isAutoCategorized: !!aiSuggestion && categoryId === String(aiSuggestion.categoryId),
    }

    try {
      if (editTarget) {
        await apiFetch<Transaction>(`/api/transactions/${editTarget.id}`, {
          method: 'PUT',
          body: JSON.stringify(payload),
        })
        toast({ title: 'Đã cập nhật giao dịch' })
      } else {
        await apiFetch<Transaction>('/api/transactions', {
          method: 'POST',
          body: JSON.stringify(payload),
        })
        toast({ title: 'Đã tạo giao dịch' })
      }
      setIsAddOpen(false)
      setEditTarget(null)
      await loadAll()
    } catch (err: any) {
      toast({
        title: editTarget ? 'Cập nhật thất bại' : 'Tạo giao dịch thất bại',
        description: err instanceof ApiError ? err.message : 'Vui lòng thử lại',
        variant: 'destructive',
      })
    }
  }

  return (
    <DashboardLayout>
      <Header title="Giao dịch" subtitle="Quản lý thu chi của bạn" />

      <div className="p-6">
        {/* Summary */}
        <div className="mb-6 flex flex-wrap items-center gap-6">
          <div>
            <p className="text-sm text-muted-foreground">Tổng thu</p>
            <p className="text-xl font-bold tabular-nums text-success">
              +{formatCurrency(totalIncome)}
            </p>
          </div>
          <div className="h-10 w-px bg-border" />
          <div>
            <p className="text-sm text-muted-foreground">Tổng chi</p>
            <p className="text-xl font-bold tabular-nums text-destructive">
              -{formatCurrency(totalExpense)}
            </p>
          </div>
        </div>

        {/* Filters */}
        <div className="mb-6 flex flex-wrap items-center gap-4">
          <div className="relative flex-1 min-w-[200px] max-w-xs">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder="Tìm giao dịch..."
              value={searchQuery}
              onChange={e => setSearchQuery(e.target.value)}
              className="pl-9"
            />
          </div>

          <Select value={filterType} onValueChange={setFilterType}>
            <SelectTrigger className="w-[140px]">
              <SelectValue placeholder="Loại" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">Tất cả</SelectItem>
              <SelectItem value="income">Thu nhập</SelectItem>
              <SelectItem value="expense">Chi tiêu</SelectItem>
            </SelectContent>
          </Select>

          <Select value={filterCategory} onValueChange={setFilterCategory}>
            <SelectTrigger className="w-[160px]">
              <SelectValue placeholder="Danh mục" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">Tất cả danh mục</SelectItem>
              {categories.map(cat => (
                <SelectItem key={cat.id} value={String(cat.id)}>
                  {cat.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>

          <Button
            variant="outline"
            className="gap-2"
            onClick={() => {
              window.location.href = '/transactions/import'
            }}
          >
            <Upload className="h-4 w-4" />
            Import CSV
          </Button>

          <Dialog open={isAddOpen || !!editTarget} onOpenChange={(o) => {
             if (!o) { setIsAddOpen(false); setEditTarget(null); }
          }}>
            <DialogTrigger asChild>
              <Button className="ml-auto gap-2" onClick={() => setIsAddOpen(true)}>
                <Plus className="h-4 w-4" />
                Thêm giao dịch
              </Button>
            </DialogTrigger>
            <DialogContent className="sm:max-w-md">
              <DialogHeader>
                <DialogTitle>{editTarget ? 'Cập nhật giao dịch' : 'Thêm giao dịch mới'}</DialogTitle>
              </DialogHeader>
              <form className="space-y-4 mt-4" onSubmit={handleSubmit}>
                <div className="space-y-2">
                  <Label htmlFor="amount">Số tiền</Label>
                  <Input
                    id="amount"
                    type="number"
                    placeholder="0"
                    className="text-lg font-semibold"
                    value={amount}
                    onChange={e => setAmount(e.target.value)}
                  />
                </div>

                <div className="space-y-2">
                  <Label>Loại giao dịch</Label>
                  <div className="flex gap-2">
                    <Button
                      type="button"
                      variant={txType === 'EXPENSE' ? 'default' : 'outline'}
                      className="flex-1"
                      onClick={() => setTxType('EXPENSE')}
                    >
                      Chi tiêu
                    </Button>
                    <Button
                      type="button"
                      variant={txType === 'INCOME' ? 'default' : 'outline'}
                      className="flex-1"
                      onClick={() => setTxType('INCOME')}
                    >
                      Thu nhập
                    </Button>
                  </div>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="description">Mô tả</Label>
                  <Input
                    id="description"
                    placeholder="VD: Cà phê sáng"
                    value={description}
                    onChange={e => setDescription(e.target.value)}
                  />
                </div>

                <div className="space-y-1">
                  <div className="flex items-center justify-between">
                    <Label htmlFor="category">Danh mục</Label>
                    {isLoadingAI ? (
                      <Badge variant="secondary" className="gap-1 text-muted-foreground bg-muted">
                        🤖 Đang phân loại...
                      </Badge>
                    ) : aiSuggestion ? (
                      <Badge variant="secondary" className="gap-1 text-indigo-600 bg-indigo-50 border-indigo-200">
                        🤖 AI gợi ý: <strong>{aiSuggestion.categoryName}</strong>
                        {aiSuggestion.source === 'CACHE' && <span className="text-[10px] ml-1 opacity-70">(cached)</span>}
                      </Badge>
                    ) : (
                      <Badge variant="secondary" className="gap-1">
                        <Sparkles className="h-3 w-3" />
                        AI hỗ trợ
                      </Badge>
                    )}
                  </div>
                  <Select value={categoryId} onValueChange={(v) => {
                    setCategoryId(v)
                    setAiSuggestion(null)
                  }}>
                    <SelectTrigger>
                      <SelectValue placeholder="Chọn danh mục" />
                    </SelectTrigger>
                    <SelectContent>
                      {categories.map(cat => (
                        <SelectItem key={cat.id} value={String(cat.id)}>
                          {cat.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  {/* T21: User override hint */}
                  {editTarget && editTarget.isAutoCategorized && categoryId !== String(editTarget.categoryId || '') && (
                    <p className="text-xs text-amber-600 mt-1.5 flex items-start gap-1">
                      <span>💡</span>
                      <span className="leading-tight">Thay đổi này sẽ giúp AI phân loại chính xác hơn cho lần sau.</span>
                    </p>
                  )}
                </div>

                <div className="space-y-2">
                  <Label htmlFor="account">Tài khoản</Label>
                  <Select value={accountId} onValueChange={setAccountId}>
                    <SelectTrigger>
                      <SelectValue placeholder="Chọn tài khoản" />
                    </SelectTrigger>
                    <SelectContent>
                      {accounts.map(acc => (
                        <SelectItem key={acc.id} value={String(acc.id)}>
                          {acc.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="date">Ngày</Label>
                  <div className="relative">
                    <Input
                      id="date"
                      type="date"
                      value={date}
                      onChange={e => setDate(e.target.value)}
                    />
                  </div>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="note">Ghi chú (tùy chọn)</Label>
                  <Input
                    id="note"
                    placeholder="Thêm ghi chú..."
                    value={note}
                    onChange={e => setNote(e.target.value)}
                  />
                </div>

                <div className="flex gap-3 pt-2">
                  <Button
                    type="button"
                    variant="outline"
                    className="flex-1"
                    onClick={() => {
                      setIsAddOpen(false)
                      setEditTarget(null)
                    }}
                  >
                    Hủy
                  </Button>
                  <Button type="submit" className="flex-1">
                    Lưu giao dịch
                  </Button>
                </div>
              </form>
            </DialogContent>
          </Dialog>
        </div>

        {/* Transactions List */}
        <div className="rounded-xl border border-border bg-card shadow-sm overflow-hidden">
          <div className="divide-y divide-border">
            {loading ? (
              <div className="p-6 text-sm text-muted-foreground">Đang tải...</div>
            ) : filteredTransactions.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-12 text-center">
                <div className="flex h-16 w-16 items-center justify-center rounded-full bg-secondary">
                  <Filter className="h-8 w-8 text-muted-foreground" />
                </div>
                <h3 className="mt-4 font-semibold text-foreground">
                  Không tìm thấy giao dịch
                </h3>
                <p className="mt-1 text-sm text-muted-foreground">
                  Thử thay đổi bộ lọc hoặc thêm giao dịch mới
                </p>
              </div>
            ) : (
              filteredTransactions.map(transaction => {
                const category = getCategoryById(transaction.categoryId)
                const account = getAccountById(transaction.accountId)
                const Icon = category
                  ? iconMap[String(category.icon || '')] || MoreHorizontal
                  : MoreHorizontal

                return (
                  <div
                    key={transaction.id}
                    className="group flex items-center gap-4 px-5 py-4 transition-colors hover:bg-secondary/50"
                  >
                    <div
                      className="flex h-11 w-11 items-center justify-center rounded-xl"
                      style={{ backgroundColor: `${category?.color || '#9E9890'}15` }}
                    >
                      <Icon
                        className="h-5 w-5"
                        style={{ color: category?.color || '#9E9890' }}
                      />
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2">
                        <p className="truncate font-medium text-foreground">
                          {transaction.description || '(Không có mô tả)'}
                        </p>
                        {transaction.isAutoCategorized && (
                          <Badge variant="secondary" className="gap-1 text-xs px-1.5 h-5 bg-indigo-50 text-indigo-600 border-indigo-200" title="Danh mục được AI tự động phân loại">
                            🤖 AI
                          </Badge>
                        )}
                      </div>
                      <div className="flex items-center gap-2 text-sm text-muted-foreground">
                        <span>{category?.name || transaction.categoryName || 'Chưa phân loại'}</span>
                        <span>•</span>
                        <span>{account?.name}</span>
                      </div>
                    </div>
                    <div className="flex items-center gap-4">
                      <div className="text-right">
                        <p
                          className={cn(
                            'font-semibold tabular-nums',
                            transaction.type === 'INCOME'
                              ? 'text-success'
                              : 'text-destructive'
                          )}
                        >
                          {transaction.type === 'INCOME' ? '+' : '-'}
                          {formatCurrency(transaction.amount)}
                        </p>
                        <p className="text-sm text-muted-foreground">
                          {formatDate(transaction.transactionDate)}
                        </p>
                      </div>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => setEditTarget(transaction)}
                        className="text-muted-foreground hover:text-foreground md:opacity-0 md:group-hover:opacity-100 transition-opacity"
                      >
                        Sửa
                      </Button>
                    </div>
                  </div>
                )
              })
            )}
          </div>
        </div>
      </div>
    </DashboardLayout>
  )
}

