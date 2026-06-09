import { formatCurrency } from '@/lib/mock-data'

export type GoalStatus = 'ACTIVE' | 'COMPLETED' | 'OVERDUE' | 'PAUSED'
export type GoalAlertStatus =
  | 'NONE'
  | 'ON_TRACK'
  | 'SLIGHTLY_BEHIND'
  | 'SIGNIFICANTLY_BEHIND'
  | 'COMPLETED'

export type GoalResponse = {
  id: number
  name: string
  targetAmount: number | string
  deadline: string
  status: GoalStatus
  linkedAccountId?: number | null
  linkedAccountName?: string | null
  currentAmount?: number | string | null
  progressPercentage: number
  remainingAmount: number | string
  monthsRemaining: number
  monthlyNeeded: number | string
  onTrack: boolean
  alertStatus?: GoalAlertStatus | null
  alertMessage?: string | null
  createdAt?: string
}

export type GoalDetailResponse = {
  goal: GoalResponse
  aiPlan?: string | null
  aiPlanGeneratedAt?: string | null
  monthlySnapshots: GoalSnapshotResponse[]
}

export type GoalPlanResponse = {
  plan: string
  generatedAt: string
  isFromCache: boolean
}

export type GoalProgressResponse = {
  goal: GoalResponse
  monthlyHistory: GoalSnapshotResponse[]
  summary: {
    totalMonths: number
    onTrackMonths: number
    behindMonths: number
    aheadMonths: number
  }
}

export type GoalSnapshotResponse = {
  month: string
  savedAmount: number | string
  plannedAmount?: number | string | null
  onTrack: boolean
  difference: number | string
}

export type AccountOption = {
  id: number
  name: string
  type: string
  balance: number | string
  scope?: string
  familyId?: number | null
}

export function money(value: number | string | null | undefined): string {
  return formatCurrency(Number(value || 0))
}

export function goalProgress(goal: GoalResponse): number {
  const value = Number(goal.progressPercentage || 0)
  return Math.max(0, Math.min(100, value))
}

export function alertTone(status?: GoalAlertStatus | null): string {
  switch (status) {
    case 'COMPLETED':
      return 'bg-success/10 text-success border-success/30'
    case 'ON_TRACK':
      return 'bg-emerald-500/10 text-emerald-700 border-emerald-500/30'
    case 'SLIGHTLY_BEHIND':
      return 'bg-warning/10 text-warning border-warning/30'
    case 'SIGNIFICANTLY_BEHIND':
      return 'bg-destructive/10 text-destructive border-destructive/30'
    default:
      return 'bg-secondary text-muted-foreground border-border'
  }
}

export function statusLabel(status: GoalStatus): string {
  switch (status) {
    case 'ACTIVE':
      return 'Đang theo dõi'
    case 'COMPLETED':
      return 'Hoàn thành'
    case 'OVERDUE':
      return 'Quá hạn'
    case 'PAUSED':
      return 'Tạm dừng'
  }
}

export function alertLabel(status?: GoalAlertStatus | null): string {
  switch (status) {
    case 'COMPLETED':
      return 'Hoàn thành'
    case 'ON_TRACK':
      return 'Đúng tiến độ'
    case 'SLIGHTLY_BEHIND':
      return 'Hơi chậm'
    case 'SIGNIFICANTLY_BEHIND':
      return 'Chậm tiến độ'
    default:
      return 'Chưa có cảnh báo'
  }
}

export function monthLabel(month: string): string {
  const date = new Date(`${month.slice(0, 7)}-01T00:00:00`)
  return new Intl.DateTimeFormat('vi-VN', { month: 'short', year: '2-digit' }).format(date)
}
