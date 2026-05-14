'use client'

import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from 'recharts'
import { expenseByCategoryData, formatCurrency } from '@/lib/mock-data'

export function ExpenseChart() {
  const total = expenseByCategoryData.reduce((sum, item) => sum + item.value, 0)

  return (
    <div className="rounded-xl border border-border bg-card p-5 shadow-sm">
      <h3 className="font-semibold text-foreground">Chi tiêu theo danh mục</h3>
      <p className="text-sm text-muted-foreground">Tháng này</p>

      <div className="mt-4 flex items-center gap-6">
        <div className="h-44 w-44">
          <ResponsiveContainer width="100%" height="100%">
            <PieChart>
              <Pie
                data={expenseByCategoryData}
                cx="50%"
                cy="50%"
                innerRadius={50}
                outerRadius={70}
                paddingAngle={2}
                dataKey="value"
              >
                {expenseByCategoryData.map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={entry.color} />
                ))}
              </Pie>
              <Tooltip
                content={({ active, payload }) => {
                  if (active && payload && payload.length) {
                    const data = payload[0].payload
                    return (
                      <div className="rounded-lg border border-border bg-card px-3 py-2 shadow-lg">
                        <p className="font-medium text-foreground">{data.name}</p>
                        <p className="text-sm text-muted-foreground">
                          {formatCurrency(data.value)}
                        </p>
                        <p className="text-xs text-muted-foreground">
                          {((data.value / total) * 100).toFixed(1)}%
                        </p>
                      </div>
                    )
                  }
                  return null
                }}
              />
            </PieChart>
          </ResponsiveContainer>
        </div>

        <div className="flex-1 space-y-2">
          {expenseByCategoryData.slice(0, 5).map(item => (
            <div key={item.name} className="flex items-center gap-3">
              <div
                className="h-3 w-3 rounded-full"
                style={{ backgroundColor: item.color }}
              />
              <span className="flex-1 text-sm text-foreground">{item.name}</span>
              <span className="text-sm tabular-nums text-muted-foreground">
                {((item.value / total) * 100).toFixed(0)}%
              </span>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
