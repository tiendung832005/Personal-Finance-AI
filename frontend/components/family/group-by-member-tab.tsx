'use client'

import { useCallback, useEffect, useMemo, useState } from 'react'
import { Button } from '@/components/ui/button'
import { formatCurrency } from '@/lib/mock-data'
import { ApiError } from '@/lib/api'
import { getFamilyByMember, monthKeyFromDate } from '@/lib/group-finance'
import { num } from '@/lib/summary'
import { useToast } from '@/hooks/use-toast'
import { ChevronLeft, ChevronRight, Loader2, Users } from 'lucide-react'

type Props = {
  groupId: number
}

export function GroupByMemberTab({ groupId }: Props) {
  const { toast } = useToast()
  const [currentMonth, setCurrentMonth] = useState(new Date())
  const monthKey = useMemo(() => monthKeyFromDate(currentMonth), [currentMonth])
  const [loading, setLoading] = useState(true)
  const [members, setMembers] = useState<
    Awaited<ReturnType<typeof getFamilyByMember>>['members']
  >([])

  const monthLabel = currentMonth.toLocaleDateString('vi-VN', {
    month: 'long',
    year: 'numeric',
  })

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const data = await getFamilyByMember(groupId, monthKey)
      setMembers(data.members)
    } catch (err) {
      const message = err instanceof ApiError ? err.message : 'Không tải được dữ liệu'
      toast({ title: 'Lỗi', description: message, variant: 'destructive' })
      setMembers([])
    } finally {
      setLoading(false)
    }
  }, [groupId, monthKey, toast])

  useEffect(() => {
    load()
  }, [load])

  if (loading) {
    return (
      <div className="flex justify-center py-16">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center gap-2">
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

      {members.length === 0 ? (
        <div className="rounded-xl border border-dashed border-border p-10 text-center text-muted-foreground">
          <Users className="mx-auto h-10 w-10 opacity-40 mb-3" />
          <p>Chưa có giao dịch chung trong tháng này.</p>
        </div>
      ) : (
        <div className="rounded-xl border border-border bg-card overflow-hidden">
          <div className="grid grid-cols-2 sm:grid-cols-5 gap-2 px-4 py-3 bg-muted/50 text-xs font-medium text-muted-foreground">
            <span className="col-span-2">Thành viên</span>
            <span className="text-right">Thu</span>
            <span className="text-right">Chi</span>
            <span className="text-right">Giao dịch</span>
          </div>
          <ul className="divide-y divide-border">
            {members.map(m => (
              <li
                key={m.userId}
                className="grid grid-cols-2 sm:grid-cols-5 gap-2 px-4 py-4 items-center"
              >
                <div className="col-span-2">
                  <p className="font-medium">{m.fullName}</p>
                  <p
                    className={`text-sm ${
                      num(m.netContribution) >= 0 ? 'text-income' : 'text-expense'
                    }`}
                  >
                    Ròng: {formatCurrency(num(m.netContribution))}
                  </p>
                </div>
                <span className="text-right text-sm text-income">
                  {formatCurrency(num(m.totalIncome))}
                </span>
                <span className="text-right text-sm text-expense">
                  {formatCurrency(num(m.totalExpense))}
                </span>
                <span className="text-right text-sm text-muted-foreground">
                  {m.transactionCount}
                </span>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}
