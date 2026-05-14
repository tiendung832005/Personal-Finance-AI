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
import { formatCurrency } from '@/lib/mock-data'
import { apiFetch, ApiError } from '@/lib/api'
import { useToast } from '@/hooks/use-toast'
import {
  Plus,
  Wallet,
  Building,
  CreditCard,
  Smartphone,
  MoreHorizontal,
  Star,
} from 'lucide-react'

const iconMap: Record<string, React.ElementType> = {
  wallet: Wallet,
  building: Building,
  'credit-card': CreditCard,
  smartphone: Smartphone,
}

type AccountType = 'CASH' | 'BANK' | 'CREDIT_CARD' | 'E_WALLET'

type AccountResponse = {
  id: number
  name: string
  type: AccountType
  balance: number
  currency: string
  defaultAccount: boolean
}

const accountTypeLabels: Record<AccountType, string> = {
  CASH: 'Tiền mặt',
  BANK: 'Ngân hàng',
  CREDIT_CARD: 'Thẻ tín dụng',
  E_WALLET: 'Ví điện tử',
}

export default function AccountsPage() {
  const [isAddOpen, setIsAddOpen] = useState(false)
  const { toast } = useToast()

  const [accounts, setAccounts] = useState<AccountResponse[]>([])
  const [loading, setLoading] = useState(true)

  const [name, setName] = useState('')
  const [type, setType] = useState<AccountType | ''>('')
  const [balance, setBalance] = useState<string>('0')
  const [currency, setCurrency] = useState('VND')
  const [defaultAccount, setDefaultAccount] = useState(false)

  const loadAccounts = async () => {
    setLoading(true)
    try {
      const res = await apiFetch<AccountResponse[]>('/api/accounts', { method: 'GET' })
      setAccounts((res.data || []).map(a => ({ ...a, balance: Number(a.balance) })))
    } catch (err: any) {
      toast({
        title: 'Không tải được tài khoản',
        description: err instanceof ApiError ? err.message : 'Vui lòng đăng nhập lại.',
        variant: 'destructive',
      })
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadAccounts()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const totals = useMemo(() => {
    const totalBalance = accounts.reduce((sum, acc) => sum + (acc.balance || 0), 0)
    const totalAssets = accounts.filter(acc => acc.balance > 0).reduce((sum, acc) => sum + acc.balance, 0)
    const totalLiabilities = accounts.filter(acc => acc.balance < 0).reduce((sum, acc) => sum + Math.abs(acc.balance), 0)
    return { totalBalance, totalAssets, totalLiabilities }
  }, [accounts])

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!type) {
      toast({ title: 'Thiếu loại tài khoản', description: 'Vui lòng chọn loại.', variant: 'destructive' })
      return
    }
    try {
      await apiFetch<AccountResponse>('/api/accounts', {
        method: 'POST',
        body: JSON.stringify({
          name,
          type,
          balance: Number(balance || 0),
          currency,
          defaultAccount,
        }),
      })
      setIsAddOpen(false)
      setName('')
      setType('')
      setBalance('0')
      setCurrency('VND')
      setDefaultAccount(false)
      await loadAccounts()
      toast({ title: 'Đã tạo tài khoản' })
    } catch (err: any) {
      toast({
        title: 'Tạo tài khoản thất bại',
        description: err instanceof ApiError ? err.message : 'Vui lòng thử lại',
        variant: 'destructive',
      })
    }
  }

  return (
    <DashboardLayout>
      <Header title="Tài khoản" subtitle="Quản lý tài khoản và số dư" />

      <div className="p-6">
        {/* Summary */}
        <div className="mb-6 grid gap-4 sm:grid-cols-3">
          <div className="rounded-xl border border-border bg-card p-5">
            <p className="text-sm text-muted-foreground">Tổng tài sản ròng</p>
            <p
              className={cn(
                'text-2xl font-bold tabular-nums',
                totals.totalBalance >= 0 ? 'text-success' : 'text-destructive'
              )}
            >
              {formatCurrency(totals.totalBalance)}
            </p>
          </div>
          <div className="rounded-xl border border-border bg-card p-5">
            <p className="text-sm text-muted-foreground">Tài sản</p>
            <p className="text-2xl font-bold tabular-nums text-success">
              {formatCurrency(totals.totalAssets)}
            </p>
          </div>
          <div className="rounded-xl border border-border bg-card p-5">
            <p className="text-sm text-muted-foreground">Nợ</p>
            <p className="text-2xl font-bold tabular-nums text-destructive">
              {formatCurrency(totals.totalLiabilities)}
            </p>
          </div>
        </div>

        {/* Add account button */}
        <div className="mb-6 flex justify-end">
          <Dialog open={isAddOpen} onOpenChange={setIsAddOpen}>
            <DialogTrigger asChild>
              <Button className="gap-2">
                <Plus className="h-4 w-4" />
                Thêm tài khoản
              </Button>
            </DialogTrigger>
            <DialogContent className="sm:max-w-md">
              <DialogHeader>
                <DialogTitle>Thêm tài khoản mới</DialogTitle>
              </DialogHeader>
              <form className="space-y-4 mt-4" onSubmit={handleCreate}>
                <div className="space-y-2">
                  <Label htmlFor="name">Tên tài khoản</Label>
                  <Input id="name" placeholder="VD: Vietcombank" value={name} onChange={e => setName(e.target.value)} />
                </div>

                <div className="space-y-2">
                  <Label>Loại tài khoản</Label>
                  <Select value={type} onValueChange={v => setType(v as AccountType)}>
                    <SelectTrigger>
                      <SelectValue placeholder="Chọn loại" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="CASH">Tiền mặt</SelectItem>
                      <SelectItem value="BANK">Ngân hàng</SelectItem>
                      <SelectItem value="CREDIT_CARD">Thẻ tín dụng</SelectItem>
                      <SelectItem value="E_WALLET">Ví điện tử</SelectItem>
                    </SelectContent>
                  </Select>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="balance">Số dư ban đầu</Label>
                  <Input
                    id="balance"
                    type="number"
                    placeholder="0"
                    className="font-semibold"
                    value={balance}
                    onChange={e => setBalance(e.target.value)}
                  />
                </div>

                <div className="space-y-2">
                  <Label>Đơn vị tiền tệ</Label>
                  <Select defaultValue="VND">
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="VND">VND - Việt Nam Đồng</SelectItem>
                      <SelectItem value="USD">USD - Đô la Mỹ</SelectItem>
                    </SelectContent>
                  </Select>
                </div>

                <div className="flex items-center gap-2">
                  <input
                    type="checkbox"
                    id="isDefault"
                    className="h-4 w-4 rounded border-border"
                    checked={defaultAccount}
                    onChange={e => setDefaultAccount(e.target.checked)}
                  />
                  <Label htmlFor="isDefault" className="font-normal">
                    Đặt làm tài khoản mặc định
                  </Label>
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
                    Lưu tài khoản
                  </Button>
                </div>
              </form>
            </DialogContent>
          </Dialog>
        </div>

        {/* Accounts Grid */}
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {loading ? (
            <div className="text-sm text-muted-foreground">Đang tải...</div>
          ) : accounts.length === 0 ? (
            <div className="text-sm text-muted-foreground">
              Chưa có tài khoản nào. Hãy tạo tài khoản đầu tiên.
            </div>
          ) : (
            accounts.map(account => {
              const Icon = Wallet
            const isNegative = account.balance < 0

            return (
              <div
                key={account.id}
                className={cn(
                  'rounded-xl border bg-card p-5 shadow-sm transition-shadow hover:shadow-md',
                  isNegative ? 'border-destructive/30' : 'border-border'
                )}
              >
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-3">
                    <div
                      className={cn(
                        'flex h-11 w-11 items-center justify-center rounded-xl',
                        isNegative
                          ? 'bg-destructive/10 text-destructive'
                          : 'bg-secondary text-foreground'
                      )}
                    >
                      <Icon className="h-5 w-5" />
                    </div>
                    <div>
                      <div className="flex items-center gap-2">
                        <h3 className="font-semibold text-foreground">
                          {account.name}
                        </h3>
                        {account.defaultAccount && (
                          <Star className="h-4 w-4 fill-warning text-warning" />
                        )}
                      </div>
                      <p className="text-sm text-muted-foreground">
                        {accountTypeLabels[account.type] || account.type}
                      </p>
                    </div>
                  </div>
                  <Button variant="ghost" size="icon" className="h-8 w-8">
                    <MoreHorizontal className="h-4 w-4" />
                  </Button>
                </div>
                <div className="mt-4">
                  <p
                    className={cn(
                      'text-2xl font-bold tabular-nums',
                      isNegative ? 'text-destructive' : 'text-foreground'
                    )}
                  >
                    {formatCurrency(account.balance)}
                  </p>
                  {isNegative && (
                    <Badge
                      variant="destructive"
                      className="mt-2 bg-destructive/10 text-destructive hover:bg-destructive/10"
                    >
                      Số dư âm
                    </Badge>
                  )}
                </div>
              </div>
            )
            })
          )}
        </div>
      </div>
    </DashboardLayout>
  )
}
