'use client'

import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { DashboardLayout } from '@/components/layout/dashboard-layout'
import { Header } from '@/components/layout/header'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { Separator } from '@/components/ui/separator'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import { useCurrentUser } from '@/hooks/use-current-user'
import { changeMyPassword, profileInitials, updateMyProfile } from '@/lib/user'
import { ApiError, logout } from '@/lib/api'
import { useToast } from '@/hooks/use-toast'
import { Loader2, LogOut, Shield, User } from 'lucide-react'

export default function SettingsPage() {
  const router = useRouter()
  const { toast } = useToast()
  const { user, loading, refresh } = useCurrentUser()

  const [fullName, setFullName] = useState('')
  const [phone, setPhone] = useState('')
  const [avatarUrl, setAvatarUrl] = useState('')
  const [saving, setSaving] = useState(false)

  const [pwdOpen, setPwdOpen] = useState(false)
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [changingPwd, setChangingPwd] = useState(false)

  useEffect(() => {
    if (user) {
      setFullName(user.fullName)
      setPhone(user.phone ?? '')
      setAvatarUrl(user.avatarUrl ?? '')
    }
  }, [user])

  const handleSaveProfile = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!fullName.trim()) return
    setSaving(true)
    try {
      await updateMyProfile({
        fullName: fullName.trim(),
        phone: phone.trim() || null,
        avatarUrl: avatarUrl.trim() || null,
      })
      await refresh()
      toast({ title: 'Đã lưu thông tin cá nhân' })
    } catch (err) {
      const message = err instanceof ApiError ? err.message : 'Lưu thất bại'
      toast({ title: 'Lỗi', description: message, variant: 'destructive' })
    } finally {
      setSaving(false)
    }
  }

  const handleChangePassword = async (e: React.FormEvent) => {
    e.preventDefault()
    if (newPassword !== confirmPassword) {
      toast({
        title: 'Mật khẩu không khớp',
        description: 'Xác nhận mật khẩu phải trùng với mật khẩu mới.',
        variant: 'destructive',
      })
      return
    }
    setChangingPwd(true)
    try {
      await changeMyPassword({ currentPassword, newPassword })
      setPwdOpen(false)
      setCurrentPassword('')
      setNewPassword('')
      setConfirmPassword('')
      toast({ title: 'Đã đổi mật khẩu' })
    } catch (err) {
      const message = err instanceof ApiError ? err.message : 'Đổi mật khẩu thất bại'
      toast({ title: 'Lỗi', description: message, variant: 'destructive' })
    } finally {
      setChangingPwd(false)
    }
  }

  const handleLogout = () => {
    logout()
    router.push('/')
  }

  const initials = user ? profileInitials(user.fullName) : '?'

  return (
    <DashboardLayout>
      <Header title="Cài đặt" subtitle="Quản lý tài khoản và bảo mật" />

      <div className="p-6">
        <div className="mx-auto max-w-2xl space-y-8">
          {loading ? (
            <div className="flex justify-center py-16">
              <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
            </div>
          ) : (
            <>
              <section className="rounded-xl border border-border bg-card p-6">
                <div className="mb-6 flex items-center gap-2">
                  <User className="h-5 w-5 text-muted-foreground" />
                  <h2 className="font-semibold text-foreground">Thông tin cá nhân</h2>
                </div>

                <div className="mb-6 flex items-center gap-6">
                  <Avatar className="h-20 w-20">
                    {avatarUrl && <AvatarImage src={avatarUrl} alt={fullName} />}
                    <AvatarFallback className="bg-secondary text-xl">{initials}</AvatarFallback>
                  </Avatar>
                  <div>
                    <h3 className="font-semibold text-foreground">{fullName || '—'}</h3>
                    <p className="text-sm text-muted-foreground">{user?.email}</p>
                    {user?.createdAt && (
                      <p className="mt-1 text-xs text-muted-foreground">
                        Tham gia từ {new Date(user.createdAt).toLocaleDateString('vi-VN')}
                      </p>
                    )}
                  </div>
                </div>

                <form onSubmit={handleSaveProfile} className="grid gap-4 sm:grid-cols-2">
                  <div className="space-y-2 sm:col-span-2">
                    <Label htmlFor="name">Họ và tên</Label>
                    <Input
                      id="name"
                      value={fullName}
                      onChange={e => setFullName(e.target.value)}
                      maxLength={100}
                      required
                    />
                  </div>
                  <div className="space-y-2 sm:col-span-2">
                    <Label htmlFor="email">Email</Label>
                    <Input id="email" type="email" value={user?.email ?? ''} disabled />
                    <p className="text-xs text-muted-foreground">
                      Email đăng nhập không thể đổi tại đây.
                    </p>
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="phone">Số điện thoại</Label>
                    <Input
                      id="phone"
                      placeholder="0912345678"
                      value={phone}
                      onChange={e => setPhone(e.target.value)}
                      maxLength={20}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="avatar">Ảnh đại diện (URL)</Label>
                    <Input
                      id="avatar"
                      placeholder="https://..."
                      value={avatarUrl}
                      onChange={e => setAvatarUrl(e.target.value)}
                      maxLength={500}
                    />
                  </div>
                  <div className="mt-2 flex justify-end sm:col-span-2">
                    <Button type="submit" disabled={saving} className="gap-2">
                      {saving && <Loader2 className="h-4 w-4 animate-spin" />}
                      Lưu thay đổi
                    </Button>
                  </div>
                </form>
              </section>

              <section className="rounded-xl border border-border bg-card p-6">
                <div className="mb-6 flex items-center gap-2">
                  <Shield className="h-5 w-5 text-muted-foreground" />
                  <h2 className="font-semibold text-foreground">Bảo mật</h2>
                </div>

                <div className="flex flex-wrap items-center justify-between gap-4">
                  <div>
                    <p className="font-medium text-foreground">Mật khẩu</p>
                    <p className="text-sm text-muted-foreground">
                      Đổi mật khẩu đăng nhập bằng mật khẩu hiện tại
                    </p>
                  </div>
                  <Dialog open={pwdOpen} onOpenChange={setPwdOpen}>
                    <DialogTrigger asChild>
                      <Button variant="outline">Đổi mật khẩu</Button>
                    </DialogTrigger>
                    <DialogContent className="sm:max-w-md">
                      <DialogHeader>
                        <DialogTitle>Đổi mật khẩu</DialogTitle>
                      </DialogHeader>
                      <form onSubmit={handleChangePassword} className="mt-4 space-y-4">
                        <div className="space-y-2">
                          <Label htmlFor="currentPwd">Mật khẩu hiện tại</Label>
                          <Input
                            id="currentPwd"
                            type="password"
                            value={currentPassword}
                            onChange={e => setCurrentPassword(e.target.value)}
                            required
                          />
                        </div>
                        <div className="space-y-2">
                          <Label htmlFor="newPwd">Mật khẩu mới</Label>
                          <Input
                            id="newPwd"
                            type="password"
                            value={newPassword}
                            onChange={e => setNewPassword(e.target.value)}
                            minLength={8}
                            required
                          />
                        </div>
                        <div className="space-y-2">
                          <Label htmlFor="confirmPwd">Xác nhận mật khẩu mới</Label>
                          <Input
                            id="confirmPwd"
                            type="password"
                            value={confirmPassword}
                            onChange={e => setConfirmPassword(e.target.value)}
                            minLength={8}
                            required
                          />
                        </div>
                        <Button type="submit" className="w-full gap-2" disabled={changingPwd}>
                          {changingPwd && <Loader2 className="h-4 w-4 animate-spin" />}
                          Cập nhật mật khẩu
                        </Button>
                      </form>
                    </DialogContent>
                  </Dialog>
                </div>

                <Separator className="my-4" />

                <p className="text-sm text-muted-foreground">
                  Quên mật khẩu? Dùng{' '}
                  <a href="/forgot-password" className="font-medium text-primary underline">
                    khôi phục qua OTP
                  </a>
                  .
                </p>
              </section>

              <Button variant="outline" className="w-full gap-2" onClick={handleLogout}>
                <LogOut className="h-4 w-4" />
                Đăng xuất
              </Button>
            </>
          )}
        </div>
      </div>
    </DashboardLayout>
  )
}
