'use client'

import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from 'recharts'
import { expenseByCategoryData, formatCurrency } from '@/lib/mock-data'

export type ExpenseChartSlice = { name: string; value: number; color: string }

type ExpenseChartProps = {
  /** When set, chart uses API breakdown; empty array = no expense data for the month. */
  data?: ExpenseChartSlice[] | null
  loading?: boolean
}

export function ExpenseChart({ data, loading }: ExpenseChartProps) {
  const chartData =
    data != null ? data : expenseByCategoryData
  const total = chartData.reduce((sum, item) => sum + item.value, 0)
  const fromApi = data != null

  return (
    <div className="rounded-xl border border-border bg-card p-5 shadow-sm">
      <h3 className="font-semibold text-foreground">Chi tiêu theo danh mục</h3>
      <p className="text-sm text-muted-foreground">Tháng này</p>

      <div className="mt-4 flex items-center gap-6">
        <div className="h-44 w-44">
          {loading ? (
            <div className="flex h-full items-center justify-center text-sm text-muted-foreground">
              Đang tải...
            </div>
          ) : total <= 0 && fromApi ? (
            <div className="flex h-full items-center justify-center text-center text-sm text-muted-foreground">
              Chưa có chi tiêu trong tháng.
            </div>
          ) : (
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={chartData}
                  cx="50%"
                  cy="50%"
                  innerRadius={50}
                  outerRadius={70}
                  paddingAngle={2}
                  dataKey="value"
                >
                  {chartData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.color} />
                  ))}
                </Pie>
                <Tooltip
                  content={({ active, payload }) => {
                    if (active && payload && payload.length) {
                      const row = payload[0].payload as ExpenseChartSlice
                      return (
                        <div className="rounded-lg border border-border bg-card px-3 py-2 shadow-lg">
                          <p className="font-medium text-foreground">{row.name}</p>
                          <p className="text-sm text-muted-foreground">
                            {formatCurrency(row.value)}
                          </p>
                          <p className="text-xs text-muted-foreground">
                            {total > 0 ? ((row.value / total) * 100).toFixed(1) : '0'}%
                          </p>
                        </div>
                      )
                    }
                    return null
                  }}
                />
              </PieChart>
            </ResponsiveContainer>
          )}
        </div>

        <div className="flex-1 space-y-2">
          {loading ? (
            <p className="text-sm text-muted-foreground">Đang tải...</p>
          ) : total <= 0 && fromApi ? (
            <p className="text-sm text-muted-foreground">Không có danh mục chi tiêu.</p>
          ) : (
            chartData.slice(0, 5).map(item => (
              <div key={item.name} className="flex items-center gap-3">
                <div
                  className="h-3 w-3 rounded-full"
                  style={{ backgroundColor: item.color }}
                />
                <span className="flex-1 text-sm text-foreground">{item.name}</span>
                <span className="text-sm tabular-nums text-muted-foreground">
                  {total > 0 ? ((item.value / total) * 100).toFixed(0) : '0'}%
                </span>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  )
}
