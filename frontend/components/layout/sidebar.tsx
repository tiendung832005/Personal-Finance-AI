'use client'

import { useState } from 'react'
import Link from 'next/link'
import { usePathname, useRouter } from 'next/navigation'
import { cn } from '@/lib/utils'
import {
  LayoutDashboard,
  ArrowLeftRight,
  Wallet,
  PiggyBank,
  Sparkles,
  Users,
  Settings,
  ChevronLeft,
  ChevronRight,
  LogOut,
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { useCurrentUser } from '@/hooks/use-current-user'
import { profileInitials } from '@/lib/user'
import { logout } from '@/lib/api'

const navItems = [
  { href: '/dashboard', label: 'Tổng quan', icon: LayoutDashboard },
  { href: '/transactions', label: 'Giao dịch', icon: ArrowLeftRight },
  { href: '/accounts', label: 'Tài khoản', icon: Wallet },
  { href: '/budgets', label: 'Ngân sách', icon: PiggyBank },
  { href: '/insights', label: 'AI Insight', icon: Sparkles },
  { href: '/family', label: 'Gia đình', icon: Users },
  { href: '/settings', label: 'Cài đặt', icon: Settings },
]

export function Sidebar() {
  const pathname = usePathname()
  const router = useRouter()
  const [collapsed, setCollapsed] = useState(false)
  const { user } = useCurrentUser()

  const displayName = user?.fullName ?? 'Tài khoản'
  const displayEmail = user?.email ?? ''
  const initials = user ? profileInitials(user.fullName) : '?'

  const handleLogout = () => {
    logout()
    router.push('/')
  }

  return (
    <aside
      className={cn(
        'fixed left-0 top-0 z-40 flex h-screen flex-col border-r border-border bg-sidebar transition-all duration-300',
        collapsed ? 'w-[72px]' : 'w-64'
      )}
    >
      <div className="flex h-16 items-center justify-between border-b border-border px-4">
        {!collapsed && (
          <Link href="/dashboard" className="flex items-center gap-2">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary">
              <Sparkles className="h-4 w-4 text-primary-foreground" />
            </div>
            <span className="font-semibold text-foreground">Finance AI</span>
          </Link>
        )}
        {collapsed && (
          <Link href="/dashboard" className="mx-auto">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary">
              <Sparkles className="h-4 w-4 text-primary-foreground" />
            </div>
          </Link>
        )}
      </div>

      <nav className="flex-1 space-y-1 p-3">
        {navItems.map(item => {
          const isActive =
            pathname === item.href ||
            (item.href !== '/dashboard' && pathname.startsWith(item.href + '/'))
          const Icon = item.icon
          return (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                'flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors',
                isActive
                  ? 'bg-sidebar-accent text-sidebar-accent-foreground'
                  : 'text-muted-foreground hover:bg-sidebar-accent hover:text-sidebar-accent-foreground',
                collapsed && 'justify-center px-2'
              )}
              title={collapsed ? item.label : undefined}
            >
              <Icon className="h-5 w-5 shrink-0" />
              {!collapsed && <span>{item.label}</span>}
            </Link>
          )
        })}
      </nav>

      <div className="border-t border-border p-3">
        <div
          className={cn(
            'flex items-center gap-3 rounded-lg p-2',
            collapsed && 'justify-center'
          )}
        >
          <Avatar className="h-9 w-9 shrink-0">
            {user?.avatarUrl && <AvatarImage src={user.avatarUrl} alt={displayName} />}
            <AvatarFallback className="bg-secondary text-secondary-foreground text-sm">
              {initials}
            </AvatarFallback>
          </Avatar>
          {!collapsed && (
            <div className="flex-1 truncate">
              <p className="truncate text-sm font-medium text-foreground">{displayName}</p>
              <p className="truncate text-xs text-muted-foreground">{displayEmail}</p>
            </div>
          )}
          {!collapsed && (
            <Button
              variant="ghost"
              size="icon"
              className="h-8 w-8 shrink-0"
              onClick={handleLogout}
              title="Đăng xuất"
            >
              <LogOut className="h-4 w-4" />
            </Button>
          )}
        </div>
      </div>

      <Button
        variant="ghost"
        size="icon"
        className="absolute -right-3 top-20 h-6 w-6 rounded-full border border-border bg-card shadow-sm"
        onClick={() => setCollapsed(!collapsed)}
      >
        {collapsed ? (
          <ChevronRight className="h-3 w-3" />
        ) : (
          <ChevronLeft className="h-3 w-3" />
        )}
      </Button>
    </aside>
  )
}
