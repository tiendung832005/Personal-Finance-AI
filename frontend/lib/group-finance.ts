import { apiFetch } from '@/lib/api'
import type { BudgetRow, BudgetStatusResponse } from '@/lib/budget'
import type {
  CategoryBreakdownItem,
  TrendItem,
} from '@/lib/summary'

export type AccountScope = 'PERSONAL' | 'SHARED'
export type AccountType = 'CASH' | 'BANK' | 'CREDIT_CARD' | 'E_WALLET'
export type TransactionType = 'INCOME' | 'EXPENSE'

export type PagedResponse<T> = {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type SharedAccount = {
  id: number
  userId: number
  familyId: number | null
  groupId: number | null
  scope: AccountScope
  name: string
  type: AccountType
  balance: number | string
  currentBalance: number | string
  currency: string
  defaultAccount?: boolean
}

export type CreateSharedAccountRequest = {
  name: string
  type: AccountType
  initialBalance: number
  currency?: string
}

export type SharedTransaction = {
  id: number
  amount: number | string
  type: TransactionType
  description: string | null
  transactionDate: string
  categoryId: number | null
  categoryName: string | null
  accountId: number
  accountName: string
  createdByUserId: number
  createdByName: string
  scope: AccountScope
  isAutoCategorized?: boolean
  createdAt?: string
}

export type CreateSharedTransactionRequest = {
  accountId: number
  categoryId?: number | null
  amount: number
  type: TransactionType
  description?: string
  transactionDate: string
  note?: string
  isAutoCategorized?: boolean
}

export type FamilySummaryResponse = {
  month: string
  groupId: number
  groupName: string
  totalIncome: number | string
  totalExpense: number | string
  netBalance: number | string
  groupAccountBalance: number | string
  categoryBreakdown: CategoryBreakdownItem[]
}

export type MemberSummaryItem = {
  userId: number
  fullName: string
  totalIncome: number | string
  totalExpense: number | string
  netContribution: number | string
  transactionCount: number
}

export type FamilyMemberBreakdownResponse = {
  month: string
  groupId: number
  members: MemberSummaryItem[]
}

export type FamilyTrendResponse = {
  groupId: number
  trend: TrendItem[]
}

function qs(params: Record<string, string | number | undefined | null>): string {
  const sp = new URLSearchParams()
  for (const [k, v] of Object.entries(params)) {
    if (v !== undefined && v !== null && v !== '') sp.set(k, String(v))
  }
  const s = sp.toString()
  return s ? `?${s}` : ''
}

// —— Shared accounts ——

export async function listSharedAccounts(groupId: number): Promise<SharedAccount[]> {
  const res = await apiFetch<SharedAccount[]>(`/api/groups/${groupId}/accounts`, {
    method: 'GET',
  })
  return res.data ?? []
}

export async function createSharedAccount(
  groupId: number,
  body: CreateSharedAccountRequest
): Promise<SharedAccount> {
  const res = await apiFetch<SharedAccount>(`/api/groups/${groupId}/accounts`, {
    method: 'POST',
    body: JSON.stringify(body),
  })
  if (!res.data) throw new Error('Failed to create shared account')
  return res.data
}

// —— Shared transactions ——

export async function listSharedTransactions(
  groupId: number,
  opts: {
    month?: string
    categoryId?: number
    type?: TransactionType
    page?: number
    size?: number
  } = {}
): Promise<PagedResponse<SharedTransaction>> {
  const query = qs({
    month: opts.month,
    categoryId: opts.categoryId,
    type: opts.type,
    page: opts.page ?? 0,
    size: opts.size ?? 20,
  })
  const res = await apiFetch<PagedResponse<SharedTransaction>>(
    `/api/groups/${groupId}/transactions${query}`,
    { method: 'GET' }
  )
  return (
    res.data ?? {
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    }
  )
}

export async function createSharedTransaction(
  groupId: number,
  body: CreateSharedTransactionRequest
): Promise<SharedTransaction> {
  const res = await apiFetch<SharedTransaction>(`/api/groups/${groupId}/transactions`, {
    method: 'POST',
    body: JSON.stringify(body),
  })
  if (!res.data) throw new Error('Failed to create transaction')
  return res.data
}

export async function deleteSharedTransaction(
  groupId: number,
  transactionId: number
): Promise<void> {
  await apiFetch<void>(`/api/groups/${groupId}/transactions/${transactionId}`, {
    method: 'DELETE',
  })
}

// —— Group budgets ——

export async function listGroupBudgets(
  groupId: number,
  month: string
): Promise<BudgetRow[]> {
  const res = await apiFetch<BudgetRow[]>(
    `/api/groups/${groupId}/budgets?month=${encodeURIComponent(month)}`,
    { method: 'GET' }
  )
  return res.data ?? []
}

export async function getGroupBudgetStatus(
  groupId: number,
  month: string
): Promise<BudgetStatusResponse> {
  const res = await apiFetch<BudgetStatusResponse>(
    `/api/groups/${groupId}/budgets/status?month=${encodeURIComponent(month)}`,
    { method: 'GET' }
  )
  if (!res.data) throw new Error('Failed to load budget status')
  return res.data
}

export async function createGroupBudget(
  groupId: number,
  body: { categoryId: number; amount: number; month: string }
): Promise<BudgetRow> {
  const res = await apiFetch<BudgetRow>(`/api/groups/${groupId}/budgets`, {
    method: 'POST',
    body: JSON.stringify(body),
  })
  if (!res.data) throw new Error('Failed to create budget')
  return res.data
}

export async function updateGroupBudget(
  groupId: number,
  budgetId: number,
  amount: number
): Promise<BudgetRow> {
  const res = await apiFetch<BudgetRow>(`/api/groups/${groupId}/budgets/${budgetId}`, {
    method: 'PUT',
    body: JSON.stringify({ amount }),
  })
  if (!res.data) throw new Error('Failed to update budget')
  return res.data
}

export async function deleteGroupBudget(groupId: number, budgetId: number): Promise<void> {
  await apiFetch<void>(`/api/groups/${groupId}/budgets/${budgetId}`, { method: 'DELETE' })
}

// —— Family summary ——

export async function getFamilySummary(
  groupId: number,
  month?: string
): Promise<FamilySummaryResponse> {
  const query = month ? `?month=${encodeURIComponent(month)}` : ''
  const res = await apiFetch<FamilySummaryResponse>(
    `/api/groups/${groupId}/summary${query}`,
    { method: 'GET' }
  )
  if (!res.data) throw new Error('Failed to load summary')
  return res.data
}

export async function getFamilyByMember(
  groupId: number,
  month?: string
): Promise<FamilyMemberBreakdownResponse> {
  const query = month ? `?month=${encodeURIComponent(month)}` : ''
  const res = await apiFetch<FamilyMemberBreakdownResponse>(
    `/api/groups/${groupId}/summary/by-member${query}`,
    { method: 'GET' }
  )
  if (!res.data) throw new Error('Failed to load by-member breakdown')
  return res.data
}

export async function getFamilyTrend(
  groupId: number,
  months = 6
): Promise<FamilyTrendResponse> {
  const res = await apiFetch<FamilyTrendResponse>(
    `/api/groups/${groupId}/summary/trend?months=${months}`,
    { method: 'GET' }
  )
  if (!res.data) throw new Error('Failed to load trend')
  return res.data
}

export function monthKeyFromDate(d: Date): string {
  const y = d.getFullYear()
  const m = d.getMonth() + 1
  return `${y}-${String(m).padStart(2, '0')}`
}
