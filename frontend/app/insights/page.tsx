'use client'

import { useState, useEffect } from 'react'
import { DashboardLayout } from '@/components/layout/dashboard-layout'
import { Header } from '@/components/layout/header'
import { HealthScoreCard } from '@/components/dashboard/health-score-card'
import { cn } from '@/lib/utils'
import { formatCurrency } from '@/lib/mock-data'
import {
  AlertTriangle,
  Lightbulb,
  Trophy,
  AlertCircle,
  TrendingDown,
  TrendingUp,
  Sparkles,
  PiggyBank,
  ShoppingBag,
  Coffee,
  Car,
  RefreshCcw,
  Loader2,
  Info,
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import { 
  getMonthlyInsight, 
  regenerateInsight, 
  getHealthScore, 
  getAnomalies,
  dismissAnomaly,
  InsightResponse,
  HealthScoreResponse,
  AnomalyResponse
} from '@/lib/insights'
import { toast } from 'sonner'

const iconMap: Record<string, React.ElementType> = {
  'alert-triangle': AlertTriangle,
  lightbulb: Lightbulb,
  trophy: Trophy,
  'alert-circle': AlertCircle,
  'UNUSUAL_AMOUNT': AlertCircle,
  'NEW_CATEGORY': Lightbulb,
}

// Default fallback tips
const defaultTips = [
  {
    icon: Coffee,
    title: 'Giảm chi tiêu cà phê',
    description: 'Tiết kiệm ~500.000₫/tháng nếu pha cà phê tại nhà',
  },

  {
    icon: Car,
    title: 'Tối ưu chi phí di chuyển',
    description: 'Sử dụng phương tiện công cộng 2 ngày/tuần tiết kiệm ~400.000₫',
  },
  {
    icon: ShoppingBag,
    title: 'Lên danh sách mua sắm',
    description: 'Tránh mua sắm bốc đồng, tiết kiệm ~300.000₫/tháng',
  },
  {
    icon: PiggyBank,
    title: 'Tự động tiết kiệm',
    description: 'Chuyển 10% lương vào tiết kiệm ngay khi nhận lương',
  },
]

export default function InsightsPage() {
  const [insight, setInsight] = useState<InsightResponse | null>(null)
  const [healthScore, setHealthScore] = useState<HealthScoreResponse | null>(null)
  const [anomalies, setAnomalies] = useState<AnomalyResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [regenerating, setRegenerating] = useState(false)

  const fetchData = async () => {
    try {
      setLoading(true)
      const [insightRes, healthRes, anomalyRes] = await Promise.all([
        getMonthlyInsight(),
        getHealthScore(),
        getAnomalies()
      ])

      if (insightRes.success) setInsight(insightRes.data || null)
      if (healthRes.success) setHealthScore(healthRes.data || null)
      if (anomalyRes.success) setAnomalies(anomalyRes.data || [])
    } catch (error) {
      console.error('Failed to fetch insights:', error)
      toast.error('Không thể tải thông tin từ AI')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchData()
  }, [])

  const handleRegenerate = async () => {
    try {
      setRegenerating(true)
      const [iRes, hRes] = await Promise.all([
        regenerateInsight(),
        regenerateHealthScore()
      ])

      if (iRes.success) setInsight(iRes.data || null)
      if (hRes.success) setHealthScore(hRes.data || null)
      
      if (iRes.success || hRes.success) {
        toast.success('Đã cập nhật phân tích mới từ AI')
      }
    } catch (error) {
      toast.error('Không thể làm mới nhận xét')
    } finally {
      setRegenerating(false)
    }
  }

  const handleDismissAnomaly = async (id: number) => {
    try {
      const res = await dismissAnomaly(id)
      if (res.success) {
        setAnomalies(prev => prev.filter(a => a.id !== id))
        toast.success('Đã bỏ qua cảnh báo')
      }
    } catch (error) {
      toast.error('Lỗi khi bỏ qua cảnh báo')
    }
  }

  const displayTips = (() => {
    if (!healthScore?.savingsTips) return defaultTips;
    try {
      const parsed = JSON.parse(healthScore.savingsTips);
      return parsed.map((item: any) => ({
        ...item,
        icon: item.title.toLowerCase().includes('mua') ? ShoppingBag : 
              item.title.toLowerCase().includes('di chuyển') ? Car : 
              item.title.toLowerCase().includes('ăn') || item.title.toLowerCase().includes('uống') ? Coffee : PiggyBank
      }));
    } catch (e) {
      return defaultTips;
    }
  })();

  if (loading) {
    return (
      <DashboardLayout>
        <div className="flex h-[calc(100vh-100px)] items-center justify-center">
          <Loader2 className="h-8 w-8 animate-spin text-primary" />
        </div>
      </DashboardLayout>
    )
  }

  return (
    <DashboardLayout>
      <Header
        title="AI Insight"
        subtitle="Phân tích và gợi ý thông minh từ AI"
      >
        <Button 
          variant="outline" 
          size="sm" 
          className="gap-2"
          onClick={handleRegenerate}
          disabled={regenerating}
        >
          {regenerating ? <Loader2 className="h-4 w-4 animate-spin" /> : <RefreshCcw className="h-4 w-4" />}
          Làm mới từ AI
        </Button>
      </Header>

      <div className="p-6">
        <div className="grid gap-6 lg:grid-cols-3">
          {/* Health Score */}
          <div className="lg:row-span-2 space-y-4">
            <HealthScoreCard score={healthScore?.overallScore || 0} size="lg" />
           

            {/* Score Breakdown */}
            {healthScore && (
              <div className="rounded-xl border border-border bg-card p-5 shadow-sm">
                <h3 className="font-semibold text-foreground">
                  Cấu trúc điểm số
                </h3>
                <div className="mt-4 space-y-4">
                  {healthScore.breakdown.map((item, index) => (
                    <div key={index}>
                      <div className="flex items-center justify-between mb-1">
                        <span className="text-sm text-foreground">
                          {item.label}
                        </span>
                        <span className="text-sm font-medium tabular-nums text-muted-foreground">
                          {item.score}/{item.maxScore}
                        </span>
                      </div>
                      <div className="h-2 w-full overflow-hidden rounded-full bg-secondary">
                        <div
                          className={cn(
                             'h-full transition-all',
                             (item.score / item.maxScore) >= 0.8 ? 'bg-success' : 
                             (item.score / item.maxScore) >= 0.5 ? 'bg-warning' : 'bg-destructive'
                          )}
                          style={{ width: `${(item.score / item.maxScore) * 100}%` }}
                        />
                      </div>
                      <p className="mt-1 text-[10px] text-muted-foreground">{item.description}</p>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>

          {/* Insights Content */}
          <div className="lg:col-span-2">
            <h3 className="mb-4 font-semibold text-foreground flex items-center gap-2">
              <Sparkles className="h-5 w-5 text-primary" />
              Tóm tắt tài chính tháng này
            </h3>
            <div className="rounded-xl border border-primary/20 bg-primary/5 p-6 shadow-sm">
              <p className="text-foreground leading-relaxed italic text-lg whitespace-pre-wrap">
                "{insight?.content || 'Hiện AI chưa có nhận xét cho dữ liệu tháng này của bạn.'}"
              </p>
              {insight?.createdAt && (
                <p className="mt-4 text-xs text-muted-foreground">
                  Cập nhật lần cuối: {new Date(insight.createdAt).toLocaleString('vi-VN')}
                </p>
              )}
            </div>
          </div>

          {/* Anomaly Detection */}
          {anomalies.length > 0 && (
            <div className="lg:col-span-2">
              <div className="rounded-xl border border-border bg-card p-5 shadow-sm">
                <div className="flex items-center gap-2 mb-4">
                  <AlertCircle className="h-5 w-5 text-destructive" />
                  <h3 className="font-semibold text-foreground">
                    Giao dịch bất thường cần chú ý
                  </h3>
                </div>
                <div className="space-y-4">
                  {anomalies.map((tx) => (
                    <div
                      key={tx.id}
                      className={cn(
                        "relative flex items-center justify-between rounded-lg border p-4",
                        tx.severity === 'HIGH' ? "border-destructive/30 bg-destructive/5" : "border-warning/30 bg-warning/5"
                      )}
                    >
                      <div className="flex-1">
                        <p className="font-semibold text-foreground">
                          {tx.description}
                        </p>
                        <p className="text-sm text-foreground mt-1 font-medium italic">
                          "{tx.explanation}"
                        </p>
                        <div className="mt-2 flex gap-2 overflow-hidden">
                           <span className="text-[10px] bg-background/50 px-2 py-0.5 rounded border">
                             {tx.anomalyType}
                           </span>
                        </div>
                      </div>
                      <div className="text-right ml-4">
                        <p className="font-semibold tabular-nums text-destructive">
                          -{formatCurrency(Number(tx.amount))}
                        </p>
                        <p className="text-xs text-muted-foreground">{new Date(tx.detectedAt).toLocaleDateString('vi-VN')}</p>
                        <Button 
                          variant="ghost" 
                          size="sm" 
                          className="mt-2 h-7 text-xs"
                          onClick={() => handleDismissAnomaly(tx.id)}
                        >
                          Bỏ qua
                        </Button>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Savings Tips (AI Generated) */}
        <div className="mt-6 rounded-xl border border-border bg-card p-5 shadow-sm">
          <div className="flex items-center gap-2 mb-4">
            <Lightbulb className="h-5 w-5 text-warning" />
            <h3 className="font-semibold text-foreground">Gợi ý tiết kiệm cá nhân hóa</h3>
          </div>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {displayTips.map((tip: any, index: number) => {
              const Icon = tip.icon || Lightbulb
              return (
                <div
                  key={index}
                  className="rounded-lg border border-border p-4 transition-colors hover:bg-secondary/50"
                >
                  <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10 text-primary mb-3">
                    <Icon className="h-5 w-5" />
                  </div>
                  <h4 className="font-medium text-foreground">{tip.title}</h4>
                  <p className="mt-1 text-sm text-muted-foreground">
                    {tip.description}
                  </p>
                </div>
              )
            })}
          </div>
        </div>
      </div>
    </DashboardLayout>
  )
}
