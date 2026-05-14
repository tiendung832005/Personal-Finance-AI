'use client'

import { useState } from 'react'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useToast } from '@/hooks/use-toast'
import { apiFetch, ApiError, setToken } from '@/lib/api'
import { Sparkles, Eye, EyeOff, ArrowRight, ArrowLeft } from 'lucide-react'

export default function RegisterPage() {
  const router = useRouter()
  const { toast } = useToast()
  const [showPassword, setShowPassword] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (password !== confirmPassword) {
      toast({
        title: 'Mật khẩu không khớp',
        description: 'Vui lòng kiểm tra lại mật khẩu xác nhận.',
        variant: 'destructive',
      })
      return
    }
    setIsLoading(true)
    try {
      const res = await apiFetch<{
        token: string
        tokenType: string
        email: string
        fullName: string
      }>('/api/auth/register', {
        method: 'POST',
        body: JSON.stringify({ email, password, fullName }),
        skipAuth: true,
      })
      const token = res.data?.token
      if (!token) throw new Error('Missing token')
      setToken(token)
      router.push('/dashboard')
    } catch (err: any) {
      toast({
        title: 'Đăng ký thất bại',
        description: err instanceof ApiError ? err.message : 'Vui lòng thử lại',
        variant: 'destructive',
      })
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="flex min-h-screen">
      {/* Left side - Illustration */}
      <div className="hidden bg-secondary lg:flex lg:w-1/2 lg:flex-col lg:items-center lg:justify-center lg:p-12">
        <div className="max-w-lg text-center">
          <div className="mx-auto mb-8 flex h-32 w-32 items-center justify-center rounded-3xl bg-primary/10">
            <Sparkles className="h-16 w-16 text-primary" />
          </div>
          <h2 className="text-3xl font-bold text-foreground">
            Bắt đầu hành trình tài chính của bạn
          </h2>
          <p className="mt-4 text-lg text-muted-foreground">
            Tạo tài khoản miễn phí và khám phá cách AI có thể giúp bạn quản lý
            tài chính hiệu quả hơn.
          </p>

          {/* Stats */}
          <div className="mt-10 grid grid-cols-3 gap-4">
            {[
              { value: '50K+', label: 'Người dùng' },
              { value: '99%', label: 'Hài lòng' },
              { value: '24/7', label: 'Hỗ trợ AI' },
            ].map((stat, i) => (
              <div key={i} className="rounded-lg bg-card p-4 shadow-sm">
                <p className="text-2xl font-bold text-foreground">
                  {stat.value}
                </p>
                <p className="text-sm text-muted-foreground">{stat.label}</p>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Right side - Form */}
      <div className="flex w-full flex-col justify-center px-8 lg:w-1/2 lg:px-16 xl:px-24">
        <div className="mx-auto w-full max-w-md">
          {/* Back to login */}
          <Link
            href="/"
            className="mb-8 inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground"
          >
            <ArrowLeft className="h-4 w-4" />
            Quay lại đăng nhập
          </Link>

          {/* Logo */}
          <div className="mb-8 flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary">
              <Sparkles className="h-5 w-5 text-primary-foreground" />
            </div>
            <span className="text-xl font-semibold text-foreground">
              Personal Finance Insight AI
            </span>
          </div>

          {/* Welcome text */}
          <div className="mb-8">
            <h1 className="text-2xl font-bold text-foreground">
              Tạo tài khoản mới
            </h1>
            <p className="mt-2 text-muted-foreground">
              Điền thông tin để bắt đầu quản lý tài chính
            </p>
          </div>

          {/* Form */}
          <form onSubmit={handleSubmit} className="space-y-5">
            <div className="space-y-2">
              <Label htmlFor="name">Họ và tên</Label>
              <Input
                id="name"
                type="text"
                placeholder="Nguyễn Văn A"
                required
                className="h-11"
                value={fullName}
                onChange={e => setFullName(e.target.value)}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="email">Email</Label>
              <Input
                id="email"
                type="email"
                placeholder="email@example.com"
                required
                className="h-11"
                value={email}
                onChange={e => setEmail(e.target.value)}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="password">Mật khẩu</Label>
              <div className="relative">
                <Input
                  id="password"
                  type={showPassword ? 'text' : 'password'}
                  placeholder="Tối thiểu 8 ký tự"
                  required
                  className="h-11 pr-10"
                  minLength={8}
                  value={password}
                  onChange={e => setPassword(e.target.value)}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                >
                  {showPassword ? (
                    <EyeOff className="h-4 w-4" />
                  ) : (
                    <Eye className="h-4 w-4" />
                  )}
                </button>
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="confirmPassword">Xác nhận mật khẩu</Label>
              <Input
                id="confirmPassword"
                type="password"
                placeholder="Nhập lại mật khẩu"
                required
                className="h-11"
                value={confirmPassword}
                onChange={e => setConfirmPassword(e.target.value)}
              />
            </div>

            <Button
              type="submit"
              className="h-11 w-full gap-2"
              disabled={isLoading}
            >
              {isLoading ? (
                'Đang tạo tài khoản...'
              ) : (
                <>
                  Đăng ký
                  <ArrowRight className="h-4 w-4" />
                </>
              )}
            </Button>
          </form>

          {/* Terms */}
          <p className="mt-6 text-center text-xs text-muted-foreground">
            Bằng cách đăng ký, bạn đồng ý với{' '}
            <a href="#" className="underline hover:text-foreground">
              Điều khoản sử dụng
            </a>{' '}
            và{' '}
            <a href="#" className="underline hover:text-foreground">
              Chính sách bảo mật
            </a>{' '}
            của chúng tôi.
          </p>
        </div>
      </div>
    </div>
  )
}
