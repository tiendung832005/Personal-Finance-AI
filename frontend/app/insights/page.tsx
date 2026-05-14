'use client'

import { DashboardLayout } from '@/components/layout/dashboard-layout'
import { Header } from '@/components/layout/header'
import { HealthScoreCard } from '@/components/dashboard/health-score-card'
import { cn } from '@/lib/utils'
import { summaryData, insights, formatCurrency, expenseByCategoryData } from '@/lib/mock-data'
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
  Wallet,
} from 'lucide-react'

const iconMap: Record<string, React.ElementType> = {
  'alert-triangle': AlertTriangle,
  lightbulb: Lightbulb,
  trophy: Trophy,
  'alert-circle': AlertCircle,
}

const typeStyles = {
  warning: {
    bg: 'bg-warning/10',
    border: 'border-warning/30',
    icon: 'text-warning',
  },
  tip: {
    bg: 'bg-primary/5',
    border: 'border-primary/20',
    icon: 'text-primary',
  },
  achievement: {
    bg: 'bg-success/10',
    border: 'border-success/30',
    icon: 'text-success',
  },
  anomaly: {
    bg: 'bg-destructive/10',
    border: 'border-destructive/30',
    icon: 'text-destructive',
  },
}

// Score breakdown
const scoreBreakdown = [
  { label: 'Tỷ lệ tiết kiệm', score: 85, maxScore: 100 },
  { label: 'Tuân thủ ngân sách', score: 72, maxScore: 100 },
  { label: 'Đa dạng thu nhập', score: 65, maxScore: 100 },
  { label: 'Quỹ dự phòng', score: 90, maxScore: 100 },
]

// Savings tips
const savingsTips = [
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

// Forecast data
const forecastData = [
  { category: 'Ăn uống', current: 4200000, forecast: 4500000 },
  { category: 'Di chuyển', current: 1800000, forecast: 1600000 },
  { category: 'Mua sắm', current: 2100000, forecast: 2800000 },
  { category: 'Hóa đơn', current: 3500000, forecast: 3500000 },
  { category: 'Giải trí', current: 950000, forecast: 1200000 },
]

// Anomaly transactions
const anomalyTransactions = [
  {
    description: 'Shopee - Áo khoác',
    amount: 890000,
    date: '07/01/2024',
    reason: 'Cao hơn 150% so với trung bình mua sắm',
  },
  {
    description: 'Grab Food',
    amount: 450000,
    date: '05/01/2024',
    reason: 'Đơn hàng lớn bất thường',
  },
]

export default function InsightsPage() {
  return (
    <DashboardLayout>
      <Header
        title="AI Insight"
        subtitle="Phân tích và gợi ý thông minh từ AI"
      />

      <div className="p-6">
        <div className="grid gap-6 lg:grid-cols-3">
          {/* Health Score */}
          <div className="lg:row-span-2">
            <HealthScoreCard score={summaryData.healthScore} size="lg" />

            {/* Score Breakdown */}
            <div className="mt-4 rounded-xl border border-border bg-card p-5 shadow-sm">
              <h3 className="font-semibold text-foreground">
                Phân tích điểm số
              </h3>
              <div className="mt-4 space-y-4">
                {scoreBreakdown.map((item, index) => (
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
                          item.score >= 80
                            ? 'bg-success'
                            : item.score >= 60
                            ? 'bg-warning'
                            : 'bg-destructive'
                        )}
                        style={{ width: `${item.score}%` }}
                      />
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>

          {/* Insights */}
          <div className="lg:col-span-2">
            <h3 className="mb-4 font-semibold text-foreground">
              Insight nổi bật
            </h3>
            <div className="grid gap-4 sm:grid-cols-2">
              {insights.map(insight => {
                const Icon = iconMap[insight.icon] || Lightbulb
                const styles = typeStyles[insight.type]

                return (
                  <div
                    key={insight.id}
                    className={cn(
                      'rounded-xl border p-5 shadow-sm',
                      styles.bg,
                      styles.border
                    )}
                  >
                    <div className="flex items-start gap-3">
                      <div
                        className={cn(
                          'flex h-10 w-10 items-center justify-center rounded-lg bg-card shrink-0',
                          styles.icon
                        )}
                      >
                        <Icon className="h-5 w-5" />
                      </div>
                      <div>
                        <h4 className="font-semibold text-foreground">
                          {insight.title}
                        </h4>
                        <p className="mt-1 text-sm text-muted-foreground">
                          {insight.description}
                        </p>
                      </div>
                    </div>
                  </div>
                )
              })}
            </div>
          </div>

          {/* Anomaly Detection */}
          <div className="lg:col-span-2">
            <div className="rounded-xl border border-border bg-card p-5 shadow-sm">
              <div className="flex items-center gap-2 mb-4">
                <AlertCircle className="h-5 w-5 text-destructive" />
                <h3 className="font-semibold text-foreground">
                  Giao dịch bất thường
                </h3>
              </div>
              <div className="space-y-3">
                {anomalyTransactions.map((tx, index) => (
                  <div
                    key={index}
                    className="flex items-center justify-between rounded-lg border border-destructive/20 bg-destructive/5 p-4"
                  >
                    <div>
                      <p className="font-medium text-foreground">
                        {tx.description}
                      </p>
                      <p className="text-sm text-muted-foreground">
                        {tx.reason}
                      </p>
                    </div>
                    <div className="text-right">
                      <p className="font-semibold tabular-nums text-destructive">
                        -{formatCurrency(tx.amount)}
                      </p>
                      <p className="text-sm text-muted-foreground">{tx.date}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>

        {/* Forecast Section */}
        <div className="mt-6 rounded-xl border border-border bg-card p-5 shadow-sm">
          <div className="flex items-center gap-2 mb-4">
            <Sparkles className="h-5 w-5 text-primary" />
            <h3 className="font-semibold text-foreground">
              Dự báo chi tiêu tháng sau
            </h3>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b border-border">
                  <th className="py-3 text-left text-sm font-medium text-muted-foreground">
                    Danh mục
                  </th>
                  <th className="py-3 text-right text-sm font-medium text-muted-foreground">
                    Tháng này
                  </th>
                  <th className="py-3 text-right text-sm font-medium text-muted-foreground">
                    Dự báo
                  </th>
                  <th className="py-3 text-right text-sm font-medium text-muted-foreground">
                    Thay đổi
                  </th>
                </tr>
              </thead>
              <tbody>
                {forecastData.map((item, index) => {
                  const change = item.forecast - item.current
                  const changePercent = Math.round(
                    (change / item.current) * 100
                  )
                  const isIncrease = change > 0

                  return (
                    <tr key={index} className="border-b border-border last:border-0">
                      <td className="py-3 text-sm font-medium text-foreground">
                        {item.category}
                      </td>
                      <td className="py-3 text-right text-sm tabular-nums text-muted-foreground">
                        {formatCurrency(item.current)}
                      </td>
                      <td className="py-3 text-right text-sm font-medium tabular-nums text-foreground">
                        {formatCurrency(item.forecast)}
                      </td>
                      <td className="py-3 text-right">
                        <span
                          className={cn(
                            'inline-flex items-center gap-1 text-sm font-medium tabular-nums',
                            isIncrease ? 'text-destructive' : 'text-success'
                          )}
                        >
                          {isIncrease ? (
                            <TrendingUp className="h-4 w-4" />
                          ) : (
                            <TrendingDown className="h-4 w-4" />
                          )}
                          {isIncrease ? '+' : ''}
                          {changePercent}%
                        </span>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        </div>

        {/* Savings Tips */}
        <div className="mt-6 rounded-xl border border-border bg-card p-5 shadow-sm">
          <div className="flex items-center gap-2 mb-4">
            <Lightbulb className="h-5 w-5 text-warning" />
            <h3 className="font-semibold text-foreground">Gợi ý tiết kiệm</h3>
          </div>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {savingsTips.map((tip, index) => {
              const Icon = tip.icon
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
