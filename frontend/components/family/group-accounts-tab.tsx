'use client'

import { useCallback, useEffect, useState } from 'react'
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
import { formatCurrency } from '@/lib/mock-data'
import { ApiError } from '@/lib/api'
import {
  createSharedAccount,
  listSharedAccounts,
  type AccountType,
  type SharedAccount,
} from '@/lib/group-finance'
import { num } from '@/lib/summary'
import { useToast } from '@/hooks/use-toast'
import { Loader2, Plus, Wallet } from 'lucide-react'

const accountTypeLabels: Record<AccountType, string> = {
  CASH: 'Tiền mặt',
  BANK: 'Ngân hàng',
  CREDIT_CARD: 'Thẻ tín dụng',
  E_WALLET: 'Ví điện tử',
}

type Props = {
  groupId: number
  isAdmin: boolean
}

export function GroupAccountsTab({ groupId, isAdmin }: Props) {
  const { toast } = useToast()
  const [accounts, setAccounts] = useState<SharedAccount[]>([])
  const [loading, setLoading] = useState(true)
  const [open, setOpen] = useState(false)
  const [saving, setSaving] = useState(false)

  const [name, setName] = useState('')
  const [type, setType] = useState<AccountType | ''>('')
  const [initialBalance, setInitialBalance] = useState('0')

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const data = await listSharedAccounts(groupId)
      setAccounts(data)
    } catch (err) {
      const message = err instanceof ApiError ? err.message : 'Không tải được tài khoản'
      toast({ title: 'Lỗi', description: message, variant: 'destructive' })
      setAccounts([])
    } finally {
      setLoading(false)
    }
  }, [groupId, toast])

  useEffect(() => {
    load()
  }, [load])

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!type) return
    setSaving(true)
    try {
      await createSharedAccount(groupId, {
        name: name.trim(),
        type,
        initialBalance: Number(initialBalance) || 0,
        currency: 'VND',
      })
      toast({ title: 'Đã tạo tài khoản chung' })
      setOpen(false)
      setName('')
      setType('')
      setInitialBalance('0')
      await load()
    } catch (err) {
      const message = err instanceof ApiError ? err.message : 'Tạo thất bại'
      toast({ title: 'Lỗi', description: message, variant: 'destructive' })
    } finally {
      setSaving(false)
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
        <p className="text-sm text-muted-foreground">
          Số dư hiển thị đã bao gồm giao dịch chung (thu − chi).
        </p>
        {isAdmin && (
          <Dialog open={open} onOpenChange={setOpen}>
            <DialogTrigger asChild>
              <Button size="sm" className="gap-2">
                <Plus className="h-4 w-4" />
                Tạo tài khoản chung
              </Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>Tạo tài khoản chung</DialogTitle>
              </DialogHeader>
              <form onSubmit={handleCreate} className="mt-4 space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="accName">Tên</Label>
                  <Input
                    id="accName"
                    value={name}
                    onChange={e => setName(e.target.value)}
                    placeholder="Quỹ gia đình"
                    required
                  />
                </div>
                <div className="space-y-2">
                  <Label>Loại</Label>
                  <Select value={type} onValueChange={v => setType(v as AccountType)}>
                    <SelectTrigger>
                      <SelectValue placeholder="Chọn loại" />
                    </SelectTrigger>
                    <SelectContent>
                      {(Object.keys(accountTypeLabels) as AccountType[]).map(t => (
                        <SelectItem key={t} value={t}>
                          {accountTypeLabels[t]}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="initial">Số dư ban đầu (VND)</Label>
                  <Input
                    id="initial"
                    type="number"
                    min={0}
                    value={initialBalance}
                    onChange={e => setInitialBalance(e.target.value)}
                  />
                </div>
                <Button type="submit" className="w-full" disabled={saving}>
                  {saving && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                  Tạo
                </Button>
              </form>
            </DialogContent>
          </Dialog>
        )}
      </div>

      {accounts.length === 0 ? (
        <div className="rounded-xl border border-dashed border-border p-10 text-center text-muted-foreground">
          <Wallet className="mx-auto h-10 w-10 opacity-40 mb-3" />
          <p>Chưa có tài khoản chung.{isAdmin ? ' Hãy tạo quỹ để bắt đầu.' : ''}</p>
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2">
          {accounts.map(acc => (
            <div
              key={acc.id}
              className="rounded-xl border border-border bg-card p-5 shadow-sm"
            >
              <div className="flex items-start justify-between gap-2">
                <div>
                  <p className="font-semibold">{acc.name}</p>
                  <p className="text-xs text-muted-foreground">
                    {accountTypeLabels[acc.type]}
                  </p>
                </div>
                <Badge variant="secondary">Chung</Badge>
              </div>
              <p className="mt-4 text-2xl font-bold tabular-nums">
                {formatCurrency(num(acc.currentBalance ?? acc.balance))}
              </p>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
