import { apiFetch, ApiResponse } from './api'

export type InsightResponse = {
  month: string
  content: string
  createdAt: string
  isFromCache: boolean
}

export type ScoreBreakdownItem = {
  label: string
  score: number
  maxScore: number
  description: string
}

export type HealthScoreResponse = {
  month: string
  overallScore: number
  savingsScore: number
  budgetScore: number
  spendingTrendScore: number
  debtScore?: number
  scoreLabel: string
  aiAnalysis?: string
  savingsTips?: string
  breakdown: ScoreBreakdownItem[]
}

export type AnomalyResponse = {
  id: number
  transactionId: number
  description: string
  amount: number | string
  anomalyType: 'UNUSUAL_AMOUNT' | 'NEW_CATEGORY' | 'FREQUENCY_SPIKE' | 'LARGE_SINGLE_TXN'
  severity: 'LOW' | 'MEDIUM' | 'HIGH'
  explanation: string | null
  detectedAt: string
  isDismissed: boolean
}

/** Lấy nhận xét AI theo tháng */
export async function getMonthlyInsight(month?: string): Promise<ApiResponse<InsightResponse>> {
  const query = month ? `?month=${month}` : ''
  return apiFetch<InsightResponse>(`/api/insights/monthly${query}`)
}

/** Yêu cầu AI làm mới nhận xét */
export async function regenerateInsight(month?: string): Promise<ApiResponse<InsightResponse>> {
  const query = month ? `?month=${month}` : ''
  return apiFetch<InsightResponse>(`/api/insights/monthly/regenerate${query}`, { method: 'POST' })
}

/** Lấy nhận xét cho nhóm gia đình */
export async function getFamilyInsight(groupId: number, month?: string): Promise<ApiResponse<InsightResponse>> {
  const query = month ? `?month=${month}` : ''
  return apiFetch<InsightResponse>(`/api/insights/family/${groupId}${query}`)
}

/** Yêu cầu AI làm mới nhận xét cho nhóm gia đình */
export async function regenerateFamilyInsight(groupId: number, month?: string): Promise<ApiResponse<InsightResponse>> {
  const query = month ? `?month=${month}` : ''
  return apiFetch<InsightResponse>(`/api/insights/family/${groupId}/regenerate${query}`, { method: 'POST' })
}

/** Lấy điểm sức khỏe tài chính */
export async function getHealthScore(month?: string): Promise<ApiResponse<HealthScoreResponse>> {
  const query = month ? `?month=${month}` : ''
  return apiFetch<HealthScoreResponse>(`/api/health-score${query}`)
}

/** Yêu cầu AI làm mới điểm sức khỏe và gợi ý tiết kiệm */
export async function regenerateHealthScore(month?: string): Promise<ApiResponse<HealthScoreResponse>> {
  const query = month ? `?month=${month}` : ''
  return apiFetch<HealthScoreResponse>(`/api/health-score/regenerate${query}`, { method: 'POST' })
}

/** Lấy danh sách giao dịch bất thường */
export async function getAnomalies(month?: string): Promise<ApiResponse<AnomalyResponse[]>> {
  const query = month ? `?month=${month}` : ''
  return apiFetch<AnomalyResponse[]>(`/api/anomalies${query}`)
}

/** Lấy danh sách cảnh báo bất thường cho nhóm gia đình */
export async function getGroupAnomalies(groupId: number, month?: string): Promise<ApiResponse<AnomalyResponse[]>> {
  const query = month ? `?month=${month}` : ''
  return apiFetch<AnomalyResponse[]>(`/api/anomalies/family/${groupId}${query}`)
}

/** Bỏ qua cảnh báo bất thường */
export async function dismissAnomaly(id: number): Promise<ApiResponse<void>> {
  return apiFetch<void>(`/api/anomalies/${id}/dismiss`, { method: 'POST' })
}
