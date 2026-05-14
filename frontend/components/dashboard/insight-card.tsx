'use client'

import { cn } from '@/lib/utils'
import { Insight } from '@/lib/mock-data'
import {
  AlertTriangle,
  Lightbulb,
  Trophy,
  AlertCircle,
  ArrowRight,
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import Link from 'next/link'

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

interface InsightCardProps {
  insight: Insight
  featured?: boolean
}

export function InsightCard({ insight, featured = false }: InsightCardProps) {
  const Icon = iconMap[insight.icon] || Lightbulb
  const styles = typeStyles[insight.type]

  if (featured) {
    return (
      <div
        className={cn(
          'rounded-xl border p-5 shadow-sm',
          styles.bg,
          styles.border
        )}
      >
        <div className="flex items-start gap-4">
          <div
            className={cn(
              'flex h-10 w-10 items-center justify-center rounded-lg bg-card',
              styles.icon
            )}
          >
            <Icon className="h-5 w-5" />
          </div>
          <div className="flex-1">
            <h4 className="font-semibold text-foreground">{insight.title}</h4>
            <p className="mt-1 text-sm text-muted-foreground">
              {insight.description}
            </p>
            <Link href="/insights" className="mt-3 inline-block">
              <Button variant="ghost" size="sm" className="gap-1 px-0">
                Xem chi tiết
                <ArrowRight className="h-4 w-4" />
              </Button>
            </Link>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="flex items-start gap-3 rounded-lg border border-border bg-card p-4 shadow-sm">
      <div
        className={cn(
          'flex h-8 w-8 items-center justify-center rounded-lg',
          styles.bg,
          styles.icon
        )}
      >
        <Icon className="h-4 w-4" />
      </div>
      <div className="flex-1 min-w-0">
        <h4 className="font-medium text-foreground">{insight.title}</h4>
        <p className="mt-0.5 line-clamp-2 text-sm text-muted-foreground">
          {insight.description}
        </p>
      </div>
    </div>
  )
}
