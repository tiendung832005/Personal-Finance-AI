// Mock data for Personal Finance Insight AI

export interface User {
  id: string
  name: string
  email: string
  avatar?: string
}

export interface Account {
  id: string
  name: string
  type: 'cash' | 'bank' | 'credit' | 'ewallet' | 'savings'
  balance: number
  currency: string
  isDefault: boolean
  icon: string
}

export interface Category {
  id: string
  name: string
  icon: string
  type: 'income' | 'expense'
  color: string
}

export interface Transaction {
  id: string
  amount: number
  type: 'income' | 'expense'
  description: string
  categoryId: string
  accountId: string
  date: string
  note?: string
  aiSuggested?: boolean
}

export interface Budget {
  id: string
  categoryId: string
  amount: number
  spent: number
  month: string // YYYY-MM
}

export interface Insight {
  id: string
  type: 'tip' | 'warning' | 'achievement' | 'anomaly'
  title: string
  description: string
  icon: string
  date: string
}

export interface FamilyMember {
  id: string
  name: string
  avatar?: string
  role: 'owner' | 'member'
  totalSpent: number
}

export interface Family {
  id: string
  name: string
  inviteCode: string
  members: FamilyMember[]
}

// Mock current user
export const currentUser: User = {
  id: '1',
  name: 'Nguyễn Văn An',
  email: 'an.nguyen@email.com',
}

// Mock accounts
export const accounts: Account[] = [
  { id: '1', name: 'Ví tiền mặt', type: 'cash', balance: 2500000, currency: 'VND', isDefault: true, icon: 'wallet' },
  { id: '2', name: 'Vietcombank', type: 'bank', balance: 45000000, currency: 'VND', isDefault: false, icon: 'building' },
  { id: '3', name: 'Techcombank', type: 'bank', balance: 12300000, currency: 'VND', isDefault: false, icon: 'building' },
  { id: '4', name: 'MoMo', type: 'ewallet', balance: 850000, currency: 'VND', isDefault: false, icon: 'smartphone' },
  { id: '5', name: 'Thẻ tín dụng VCB', type: 'credit', balance: -5200000, currency: 'VND', isDefault: false, icon: 'credit-card' },
  { id: '6', name: 'Tiết kiệm', type: 'savings', balance: 100000000, currency: 'VND', isDefault: false, icon: 'piggy-bank' },
]

// Mock categories
export const categories: Category[] = [
  { id: '1', name: 'Ăn uống', icon: 'utensils', type: 'expense', color: '#A0522D' },
  { id: '2', name: 'Di chuyển', icon: 'car', type: 'expense', color: '#6B7FA3' },
  { id: '3', name: 'Mua sắm', icon: 'shopping-bag', type: 'expense', color: '#8B6F8B' },
  { id: '4', name: 'Hóa đơn', icon: 'file-text', type: 'expense', color: '#C49A3C' },
  { id: '5', name: 'Giải trí', icon: 'gamepad-2', type: 'expense', color: '#7A9E9F' },
  { id: '6', name: 'Sức khỏe', icon: 'heart-pulse', type: 'expense', color: '#A0522D' },
  { id: '7', name: 'Giáo dục', icon: 'graduation-cap', type: 'expense', color: '#6B7FA3' },
  { id: '8', name: 'Khác', icon: 'more-horizontal', type: 'expense', color: '#9E9890' },
  { id: '9', name: 'Lương', icon: 'briefcase', type: 'income', color: '#4A7C59' },
  { id: '10', name: 'Thưởng', icon: 'gift', type: 'income', color: '#4A7C59' },
  { id: '11', name: 'Đầu tư', icon: 'trending-up', type: 'income', color: '#4A7C59' },
  { id: '12', name: 'Thu nhập khác', icon: 'plus-circle', type: 'income', color: '#4A7C59' },
]

// Mock transactions
export const transactions: Transaction[] = [
  { id: '1', amount: 35000, type: 'expense', description: 'Cà phê Highlands', categoryId: '1', accountId: '4', date: '2024-01-15', aiSuggested: true },
  { id: '2', amount: 150000, type: 'expense', description: 'Grab đi làm', categoryId: '2', accountId: '4', date: '2024-01-15' },
  { id: '3', amount: 25000000, type: 'income', description: 'Lương tháng 1', categoryId: '9', accountId: '2', date: '2024-01-10' },
  { id: '4', amount: 2500000, type: 'expense', description: 'Tiền điện tháng 12', categoryId: '4', accountId: '2', date: '2024-01-08' },
  { id: '5', amount: 890000, type: 'expense', description: 'Shopee - Áo khoác', categoryId: '3', accountId: '5', date: '2024-01-07', aiSuggested: true },
  { id: '6', amount: 180000, type: 'expense', description: 'Bún bò Huế', categoryId: '1', accountId: '1', date: '2024-01-06' },
  { id: '7', amount: 5000000, type: 'income', description: 'Thưởng dự án', categoryId: '10', accountId: '2', date: '2024-01-05' },
  { id: '8', amount: 450000, type: 'expense', description: 'Xem phim CGV', categoryId: '5', accountId: '4', date: '2024-01-04' },
  { id: '9', amount: 1200000, type: 'expense', description: 'Khám bệnh', categoryId: '6', accountId: '2', date: '2024-01-03' },
  { id: '10', amount: 350000, type: 'expense', description: 'Sách lập trình', categoryId: '7', accountId: '4', date: '2024-01-02' },
]

// Mock budgets
export const budgets: Budget[] = [
  { id: '1', categoryId: '1', amount: 5000000, spent: 4200000, month: '2024-01' },
  { id: '2', categoryId: '2', amount: 2000000, spent: 1800000, month: '2024-01' },
  { id: '3', categoryId: '3', amount: 3000000, spent: 2100000, month: '2024-01' },
  { id: '4', categoryId: '4', amount: 4000000, spent: 3500000, month: '2024-01' },
  { id: '5', categoryId: '5', amount: 1500000, spent: 950000, month: '2024-01' },
  { id: '6', categoryId: '6', amount: 2000000, spent: 1200000, month: '2024-01' },
]

// Mock insights
export const insights: Insight[] = [
  {
    id: '1',
    type: 'warning',
    title: 'Chi tiêu ăn uống vượt ngưỡng',
    description: 'Bạn đã chi 4.200.000₫ cho ăn uống, chiếm 84% ngân sách tháng này. Còn 2 tuần nữa mới hết tháng.',
    icon: 'alert-triangle',
    date: '2024-01-15',
  },
  {
    id: '2',
    type: 'tip',
    title: 'Gợi ý tiết kiệm',
    description: 'Nếu giảm 20% chi tiêu Grab, bạn có thể tiết kiệm thêm 360.000₫/tháng.',
    icon: 'lightbulb',
    date: '2024-01-14',
  },
  {
    id: '3',
    type: 'achievement',
    title: 'Hoàn thành mục tiêu',
    description: 'Chúc mừng! Bạn đã tiết kiệm được 10 triệu đồng trong quý này.',
    icon: 'trophy',
    date: '2024-01-13',
  },
  {
    id: '4',
    type: 'anomaly',
    title: 'Giao dịch bất thường',
    description: 'Phát hiện giao dịch 890.000₫ tại Shopee - cao hơn 150% so với trung bình.',
    icon: 'alert-circle',
    date: '2024-01-07',
  },
]

// Mock family
export const family: Family = {
  id: '1',
  name: 'Gia đình Nguyễn',
  inviteCode: 'NGUYEN2024',
  members: [
    { id: '1', name: 'Nguyễn Văn An', role: 'owner', totalSpent: 8500000 },
    { id: '2', name: 'Trần Thị Mai', role: 'member', totalSpent: 6200000 },
    { id: '3', name: 'Nguyễn Minh Tuấn', role: 'member', totalSpent: 2100000 },
  ],
}

// Chart data - Monthly trend
export const monthlyTrendData = [
  { month: 'T8', income: 28000000, expense: 22000000 },
  { month: 'T9', income: 25000000, expense: 24500000 },
  { month: 'T10', income: 30000000, expense: 21000000 },
  { month: 'T11', income: 27000000, expense: 25000000 },
  { month: 'T12', income: 32000000, expense: 28000000 },
  { month: 'T1', income: 30000000, expense: 18500000 },
]

// Chart data - Expense by category
export const expenseByCategoryData = [
  { name: 'Ăn uống', value: 4200000, color: '#A0522D' },
  { name: 'Di chuyển', value: 1800000, color: '#6B7FA3' },
  { name: 'Mua sắm', value: 2100000, color: '#8B6F8B' },
  { name: 'Hóa đơn', value: 3500000, color: '#C49A3C' },
  { name: 'Giải trí', value: 950000, color: '#7A9E9F' },
  { name: 'Khác', value: 1450000, color: '#9E9890' },
]

// Summary data
export const summaryData = {
  totalIncome: 30000000,
  totalExpense: 18500000,
  balance: 155450000,
  healthScore: 78,
}

// Helper function to format currency
export function formatCurrency(amount: number): string {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(amount)
}

// Helper function to format date
export function formatDate(dateString: string): string {
  const date = new Date(dateString)
  return new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(date)
}

// Helper to get category by id
export function getCategoryById(id: string): Category | undefined {
  return categories.find(c => c.id === id)
}

// Helper to get account by id
export function getAccountById(id: string): Account | undefined {
  return accounts.find(a => a.id === id)
}
