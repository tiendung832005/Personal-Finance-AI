'use client'

import { cn } from '@/lib/utils'

interface HealthScoreCardProps {
  score: number
  size?: 'sm' | 'md' | 'lg'
}

export function HealthScoreCard({ score, size = 'md' }: HealthScoreCardProps) {
  const getScoreColor = (score: number) => {
    if (score >= 80) return 'text-success'
    if (score >= 60) return 'text-warning'
    return 'text-destructive'
  }

  const getScoreLabel = (score: number) => {
    if (score >= 80) return 'Xuất sắc'
    if (score >= 60) return 'Khá tốt'
    if (score >= 40) return 'Cần cải thiện'
    return 'Cần chú ý'
  }

  const getScoreStroke = (score: number) => {
    if (score >= 80) return 'stroke-success'
    if (score >= 60) return 'stroke-warning'
    return 'stroke-destructive'
  }

  const sizeConfig = {
    sm: { container: 'w-20 h-20', text: 'text-xl', label: 'text-xs' },
    md: { container: 'w-32 h-32', text: 'text-3xl', label: 'text-sm' },
    lg: { container: 'w-48 h-48', text: 'text-5xl', label: 'text-base' },
  }

  const radius = size === 'lg' ? 80 : size === 'md' ? 52 : 32
  const circumference = 2 * Math.PI * radius
  const strokeDashoffset = circumference - (score / 100) * circumference

  return (
    <div className="rounded-xl border border-border bg-card p-5 shadow-sm">
      <p className="mb-4 text-sm font-medium text-muted-foreground">
        Điểm tài chính
      </p>
      <div className="flex flex-col items-center">
        <div className={cn('relative', sizeConfig[size].container)}>
          <svg className="h-full w-full -rotate-90" viewBox={`0 0 ${(radius + 8) * 2} ${(radius + 8) * 2}`}>
            <circle
              cx={radius + 8}
              cy={radius + 8}
              r={radius}
              strokeWidth="8"
              fill="none"
              className="stroke-secondary"
            />
            <circle
              cx={radius + 8}
              cy={radius + 8}
              r={radius}
              strokeWidth="8"
              fill="none"
              strokeLinecap="round"
              className={cn('transition-all duration-1000', getScoreStroke(score))}
              style={{
                strokeDasharray: circumference,
                strokeDashoffset: strokeDashoffset,
              }}
            />
          </svg>
          <div className="absolute inset-0 flex flex-col items-center justify-center">
            <span
              className={cn(
                'font-bold tabular-nums',
                sizeConfig[size].text,
                getScoreColor(score)
              )}
            >
              {score}
            </span>
          </div>
        </div>
        <p
          className={cn(
            'mt-2 font-medium',
            sizeConfig[size].label,
            getScoreColor(score)
          )}
        >
          {getScoreLabel(score)}
        </p>
      </div>
    </div>
  )
}
