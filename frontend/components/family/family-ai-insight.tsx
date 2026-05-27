'use client'

import { useState, useEffect, useCallback } from 'react'
import { Sparkles, RefreshCcw, Loader2, MessageSquare } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { getFamilyInsight, regenerateFamilyInsight, type InsightResponse } from '@/lib/insights'
import { toast } from 'react-hot-toast'

interface FamilyAiInsightProps {
  groupId: number
  month: string // format "yyyy-MM" 
}

export function FamilyAiInsight({ groupId, month }: FamilyAiInsightProps) {
  const [insight, setInsight] = useState<InsightResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [regenerating, setRegenerating] = useState(false)

  const loadInsight = useCallback(async () => {
    setLoading(true)
    try {
      const res = await getFamilyInsight(groupId, month)
      if (res.success) {
        setInsight(res.data || null)
      }
    } catch (error) {
      console.error('Failed to load family AI insight:', error)
    } finally {
      setLoading(false)
    }
  }, [groupId, month])

  useEffect(() => {
    loadInsight()
  }, [loadInsight])

  const handleRegenerate = async () => {
    setRegenerating(true)
    try {
      const res = await regenerateFamilyInsight(groupId, month)
      if (res.success) {
        setInsight(res.data || null)
        toast.success('Đã cập nhật nhận xét gia đình mới từ AI')
      }
    } catch (error) {
      toast.error('Không thể làm mới nhận xét lúc này')
    } finally {
      setRegenerating(false)
    }
  }

  if (loading) {
    return (
      <div className="rounded-xl border border-border bg-card p-6 flex flex-col items-center justify-center space-y-3 min-h-[160px]">
        <Loader2 className="h-6 w-6 animate-spin text-primary" />
        <p className="text-sm text-muted-foreground italic">AI đang phân tích tài chính gia đình...</p>
      </div>
    )
  }

  return (
    <div className="group relative overflow-hidden rounded-xl border border-primary/20 bg-gradient-to-br from-card via-card to-primary/5 p-6 shadow-sm transition-all hover:shadow-md">
      {/* Decorative pulse effect */}
      <div className="absolute -right-4 -top-4 h-24 w-24 rounded-full bg-primary/5 blur-2xl group-hover:bg-primary/10 transition-colors" />
      
      <div className="flex items-start justify-between gap-4">
        <div className="flex items-center gap-2">
          <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary/10 text-primary">
            <Sparkles className="h-5 w-5" />
          </div>
          <h3 className="font-semibold text-foreground">AI Cố vấn gia đình</h3>
          {insight?.isFromCache && (
            <span className="text-[10px] font-medium uppercase tracking-wider text-muted-foreground/60 bg-muted px-1.5 py-0.5 rounded">
              Đã lưu
            </span>
          )}
        </div>

        <Button
          variant="ghost"
          size="sm"
          className="h-8 px-2 text-xs gap-1.5 text-muted-foreground hover:text-primary hover:bg-primary/5"
          onClick={handleRegenerate}
          disabled={regenerating}
        >
          <RefreshCcw className={`h-3.5 w-3.5 ${regenerating ? 'animate-spin' : ''}`} />
          {regenerating ? 'Đang tính toán...' : 'Làm mới'}
        </Button>
      </div>

      <div className="mt-4 relative">
        <div className="absolute -left-1 top-0 h-full w-0.5 bg-primary/20 rounded-full" />
        <div className="pl-4">
          {insight?.content ? (
            <p className="text-[15px] leading-relaxed text-foreground/90 whitespace-pre-wrap">
              {insight.content}
            </p>
          ) : (
            <p className="text-[15px] text-muted-foreground italic">
              Gia đình chưa có đủ dữ liệu chi tiêu chung để AI đưa ra nhận xét. Hãy cập nhật giao dịch nhé!
            </p>
          )}
        </div>
      </div>

      <div className="mt-5 flex items-center gap-4 text-[11px] text-muted-foreground border-t border-border/50 pt-4">
        <div className="flex items-center gap-1">
          <MessageSquare className="h-3 w-3" />
          <span>Dựa trên giao dịch của tất cả thành viên</span>
        </div>
        <span>•</span>
        <span>Cập nhật: {insight?.createdAt ? new Date(insight.createdAt).toLocaleString('vi-VN') : 'vừa xong'}</span>
      </div>
    </div>
  )
}
