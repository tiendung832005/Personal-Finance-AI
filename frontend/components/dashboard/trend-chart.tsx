'use client'

import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from 'recharts'
import { monthlyTrendData, formatCurrency } from '@/lib/mock-data'

export function TrendChart() {
  return (
    <div className="rounded-xl border border-border bg-card p-5 shadow-sm">
      <h3 className="font-semibold text-foreground">Xu hướng thu chi</h3>
      <p className="text-sm text-muted-foreground">6 tháng gần nhất</p>

      <div className="mt-4 h-64">
        <ResponsiveContainer width="100%" height="100%">
          <LineChart data={monthlyTrendData}>
            <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
            <XAxis
              dataKey="month"
              tick={{ fill: 'var(--muted-foreground)', fontSize: 12 }}
              tickLine={false}
              axisLine={false}
            />
            <YAxis
              tick={{ fill: 'var(--muted-foreground)', fontSize: 12 }}
              tickLine={false}
              axisLine={false}
              tickFormatter={value => `${(value / 1000000).toFixed(0)}M`}
            />
            <Tooltip
              content={({ active, payload, label }) => {
                if (active && payload && payload.length) {
                  return (
                    <div className="rounded-lg border border-border bg-card px-3 py-2 shadow-lg">
                      <p className="font-medium text-foreground">Tháng {label}</p>
                      {payload.map(item => (
                        <p
                          key={item.dataKey}
                          className="text-sm"
                          style={{ color: item.color }}
                        >
                          {item.dataKey === 'income' ? 'Thu nhập' : 'Chi tiêu'}:{' '}
                          {formatCurrency(item.value as number)}
                        </p>
                      ))}
                    </div>
                  )
                }
                return null
              }}
            />
            <Legend
              formatter={value =>
                value === 'income' ? 'Thu nhập' : 'Chi tiêu'
              }
            />
            <Line
              type="monotone"
              dataKey="income"
              stroke="var(--success)"
              strokeWidth={2}
              dot={{ fill: 'var(--success)', strokeWidth: 0, r: 4 }}
              activeDot={{ r: 6 }}
            />
            <Line
              type="monotone"
              dataKey="expense"
              stroke="var(--destructive)"
              strokeWidth={2}
              dot={{ fill: 'var(--destructive)', strokeWidth: 0, r: 4 }}
              activeDot={{ r: 6 }}
            />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </div>
  )
}
