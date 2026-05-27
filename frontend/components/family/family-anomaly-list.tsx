'use client'

import { useState, useEffect, useCallback } from 'react'
import { AlertCircle, Loader2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { getGroupAnomalies, dismissAnomaly, type AnomalyResponse } from '@/lib/insights'
import { formatCurrency } from '@/lib/mock-data'
import { cn } from '@/lib/utils'
import { toast } from 'react-hot-toast'

interface FamilyAnomalyListProps {
  groupId: number
  month: string
}

export function FamilyAnomalyList({ groupId, month }: FamilyAnomalyListProps) {
  const [anomalies, setAnomalies] = useState<AnomalyResponse[]>([])
  const [loading, setLoading] = useState(true)

  const loadAnomalies = useCallback(async () => {
    setLoading(true)
    try {
      const res = await getGroupAnomalies(groupId, month)
      if (res.success) {
        setAnomalies(res.data || [])
      }
    } catch (error) {
      console.error('Failed to load group anomalies:', error)
    } finally {
      setLoading(false)
    }
  }, [groupId, month])

  useEffect(() => {
    loadAnomalies()
  }, [loadAnomalies])

  const handleDismiss = async (id: number) => {
    try {
      const res = await dismissAnomaly(id)
      if (res.success) {
        setAnomalies(prev => prev.filter(a => a.id !== id))
        toast.success('Đã bỏ qua cảnh báo')
      }
    } catch (error) {
      toast.error('Không thể bỏ qua cảnh báo')
    }
  }

  if (loading) {
    return (
      <div className="flex justify-center p-8">
        <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
      </div>
    )
  }

  if (anomalies.length === 0) return null

  return (
    <section className="rounded-xl border border-border bg-card p-5 shadow-sm">
      <div className="flex items-center gap-2 mb-4">
        <AlertCircle className="h-5 w-5 text-destructive" />
        <h3 className="font-semibold text-foreground">Giao dịch chung bất thường cần chú ý</h3>
      </div>
      
      <div className="space-y-4">
        {anomalies.map((tx) => (
          <div
            key={tx.id}
            className={cn(
              "flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 rounded-lg border p-4 transition-colors",
              tx.severity === 'HIGH' ? "border-destructive/20 bg-destructive/5" : "border-warning/20 bg-warning/5"
            )}
          >
            <div className="flex-1 min-w-0">
              <p className="font-semibold text-foreground truncate">
                {tx.description}
              </p>
              <p className="text-sm text-foreground/80 mt-1 font-medium italic">
                &quot;{tx.explanation}&quot;
              </p>
              <div className="mt-2 flex gap-2 overflow-hidden">
                 <span className="text-[10px] bg-background/80 px-2 py-0.5 rounded border border-border/50 uppercase tracking-wider font-semibold">
                   {tx.anomalyType.replace(/_/g, ' ')}
                 </span>
              </div>
            </div>
            
            <div className="flex flex-row sm:flex-col items-center sm:items-end justify-between w-full sm:w-auto gap-2 border-t sm:border-t-0 pt-3 sm:pt-0">
              <div className="text-left sm:text-right">
                <p className="font-bold tabular-nums text-destructive">
                  -{formatCurrency(Number(tx.amount))}
                </p>
                <p className="text-xs text-muted-foreground whitespace-nowrap">
                  {new Date(tx.detectedAt).toLocaleDateString('vi-VN')}
                </p>
              </div>
              <Button 
                variant="ghost" 
                size="sm" 
                className="h-8 text-xs hover:bg-background/50 text-muted-foreground"
                onClick={() => handleDismiss(tx.id)}
              >
                Bỏ qua
              </Button>
            </div>
          </div>
        ))}
      </div>
    </section>
  )
}
