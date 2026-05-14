'use client'

import { useState } from 'react'
import { DashboardLayout } from '@/components/layout/dashboard-layout'
import { Header } from '@/components/layout/header'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Switch } from '@/components/ui/switch'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { Separator } from '@/components/ui/separator'
import { currentUser } from '@/lib/mock-data'
import {
  User,
  Bell,
  Shield,
  Palette,
  Download,
  Trash2,
  Camera,
  LogOut,
} from 'lucide-react'

export default function SettingsPage() {
  const [notifications, setNotifications] = useState({
    email: true,
    push: true,
    weekly: false,
    anomaly: true,
  })

  const initials = currentUser.name
    .split(' ')
    .map(n => n[0])
    .join('')
    .slice(-2)
    .toUpperCase()

  return (
    <DashboardLayout>
      <Header title="Cài đặt" subtitle="Quản lý tài khoản và tùy chọn" />

      <div className="p-6">
        <div className="mx-auto max-w-2xl space-y-8">
          {/* Profile Section */}
          <section className="rounded-xl border border-border bg-card p-6">
            <div className="flex items-center gap-2 mb-6">
              <User className="h-5 w-5 text-muted-foreground" />
              <h2 className="font-semibold text-foreground">
                Thông tin cá nhân
              </h2>
            </div>

            <div className="flex items-center gap-6 mb-6">
              <div className="relative">
                <Avatar className="h-20 w-20">
                  <AvatarFallback className="bg-secondary text-xl">
                    {initials}
                  </AvatarFallback>
                </Avatar>
                <button className="absolute bottom-0 right-0 flex h-8 w-8 items-center justify-center rounded-full border-2 border-card bg-primary text-primary-foreground">
                  <Camera className="h-4 w-4" />
                </button>
              </div>
              <div>
                <h3 className="font-semibold text-foreground">
                  {currentUser.name}
                </h3>
                <p className="text-sm text-muted-foreground">
                  {currentUser.email}
                </p>
              </div>
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="name">Họ và tên</Label>
                <Input id="name" defaultValue={currentUser.name} />
              </div>
              <div className="space-y-2">
                <Label htmlFor="email">Email</Label>
                <Input
                  id="email"
                  type="email"
                  defaultValue={currentUser.email}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="phone">Số điện thoại</Label>
                <Input id="phone" placeholder="0912 345 678" />
              </div>
              <div className="space-y-2">
                <Label>Ngôn ngữ</Label>
                <Select defaultValue="vi">
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="vi">Tiếng Việt</SelectItem>
                    <SelectItem value="en">English</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>

            <div className="mt-6 flex justify-end">
              <Button>Lưu thay đổi</Button>
            </div>
          </section>

          {/* Notifications Section */}
          <section className="rounded-xl border border-border bg-card p-6">
            <div className="flex items-center gap-2 mb-6">
              <Bell className="h-5 w-5 text-muted-foreground" />
              <h2 className="font-semibold text-foreground">Thông báo</h2>
            </div>

            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="font-medium text-foreground">
                    Thông báo qua email
                  </p>
                  <p className="text-sm text-muted-foreground">
                    Nhận báo cáo và cập nhật qua email
                  </p>
                </div>
                <Switch
                  checked={notifications.email}
                  onCheckedChange={checked =>
                    setNotifications({ ...notifications, email: checked })
                  }
                />
              </div>

              <Separator />

              <div className="flex items-center justify-between">
                <div>
                  <p className="font-medium text-foreground">
                    Thông báo đẩy
                  </p>
                  <p className="text-sm text-muted-foreground">
                    Nhận thông báo trực tiếp trên thiết bị
                  </p>
                </div>
                <Switch
                  checked={notifications.push}
                  onCheckedChange={checked =>
                    setNotifications({ ...notifications, push: checked })
                  }
                />
              </div>

              <Separator />

              <div className="flex items-center justify-between">
                <div>
                  <p className="font-medium text-foreground">
                    Báo cáo tuần
                  </p>
                  <p className="text-sm text-muted-foreground">
                    Nhận tổng hợp chi tiêu hàng tuần
                  </p>
                </div>
                <Switch
                  checked={notifications.weekly}
                  onCheckedChange={checked =>
                    setNotifications({ ...notifications, weekly: checked })
                  }
                />
              </div>

              <Separator />

              <div className="flex items-center justify-between">
                <div>
                  <p className="font-medium text-foreground">
                    Cảnh báo bất thường
                  </p>
                  <p className="text-sm text-muted-foreground">
                    Nhận thông báo khi phát hiện giao dịch bất thường
                  </p>
                </div>
                <Switch
                  checked={notifications.anomaly}
                  onCheckedChange={checked =>
                    setNotifications({ ...notifications, anomaly: checked })
                  }
                />
              </div>
            </div>
          </section>

          {/* Security Section */}
          <section className="rounded-xl border border-border bg-card p-6">
            <div className="flex items-center gap-2 mb-6">
              <Shield className="h-5 w-5 text-muted-foreground" />
              <h2 className="font-semibold text-foreground">Bảo mật</h2>
            </div>

            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="font-medium text-foreground">Đổi mật khẩu</p>
                  <p className="text-sm text-muted-foreground">
                    Cập nhật mật khẩu đăng nhập
                  </p>
                </div>
                <Button variant="outline">Đổi mật khẩu</Button>
              </div>

              <Separator />

              <div className="flex items-center justify-between">
                <div>
                  <p className="font-medium text-foreground">
                    Xác thực 2 bước
                  </p>
                  <p className="text-sm text-muted-foreground">
                    Bảo vệ tài khoản với xác thực 2 bước
                  </p>
                </div>
                <Button variant="outline">Thiết lập</Button>
              </div>
            </div>
          </section>

          {/* Appearance Section */}
          <section className="rounded-xl border border-border bg-card p-6">
            <div className="flex items-center gap-2 mb-6">
              <Palette className="h-5 w-5 text-muted-foreground" />
              <h2 className="font-semibold text-foreground">Giao diện</h2>
            </div>

            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="font-medium text-foreground">Chế độ tối</p>
                  <p className="text-sm text-muted-foreground">
                    Chuyển đổi giao diện sáng/tối
                  </p>
                </div>
                <Select defaultValue="light">
                  <SelectTrigger className="w-[140px]">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="light">Sáng</SelectItem>
                    <SelectItem value="dark">Tối</SelectItem>
                    <SelectItem value="system">Hệ thống</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <Separator />

              <div className="flex items-center justify-between">
                <div>
                  <p className="font-medium text-foreground">Đơn vị tiền tệ</p>
                  <p className="text-sm text-muted-foreground">
                    Đơn vị hiển thị mặc định
                  </p>
                </div>
                <Select defaultValue="VND">
                  <SelectTrigger className="w-[140px]">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="VND">VND (₫)</SelectItem>
                    <SelectItem value="USD">USD ($)</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>
          </section>

          {/* Data Section */}
          <section className="rounded-xl border border-border bg-card p-6">
            <div className="flex items-center gap-2 mb-6">
              <Download className="h-5 w-5 text-muted-foreground" />
              <h2 className="font-semibold text-foreground">Dữ liệu</h2>
            </div>

            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="font-medium text-foreground">Xuất dữ liệu</p>
                  <p className="text-sm text-muted-foreground">
                    Tải xuống tất cả dữ liệu của bạn
                  </p>
                </div>
                <Button variant="outline" className="gap-2">
                  <Download className="h-4 w-4" />
                  Xuất Excel
                </Button>
              </div>

              <Separator />

              <div className="flex items-center justify-between">
                <div>
                  <p className="font-medium text-destructive">Xóa tài khoản</p>
                  <p className="text-sm text-muted-foreground">
                    Xóa vĩnh viễn tài khoản và tất cả dữ liệu
                  </p>
                </div>
                <Button variant="destructive" className="gap-2">
                  <Trash2 className="h-4 w-4" />
                  Xóa tài khoản
                </Button>
              </div>
            </div>
          </section>

          {/* Logout */}
          <Button variant="outline" className="w-full gap-2">
            <LogOut className="h-4 w-4" />
            Đăng xuất
          </Button>
        </div>
      </div>
    </DashboardLayout>
  )
}
