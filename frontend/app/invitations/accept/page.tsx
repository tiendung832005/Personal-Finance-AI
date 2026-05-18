'use client'

import { Suspense, useState } from 'react'
import Link from 'next/link'
import { useRouter, useSearchParams } from 'next/navigation'
import { Button } from '@/components/ui/button'
import { acceptInvitation } from '@/lib/groups'
import { ApiError, getToken } from '@/lib/api'
import { useToast } from '@/hooks/use-toast'
import { CheckCircle2, Loader2, LogIn, Users } from 'lucide-react'

function CardShell({ children }: { children: React.ReactNode }) {
  return (
    <div className="mx-auto max-w-md rounded-xl border border-border bg-card p-8 text-center">
      {children}
    </div>
  )
}

function AcceptInvitationContent() {
  const searchParams = useSearchParams()
  const router = useRouter()
  const { toast } = useToast()
  const token = searchParams.get('token') ?? ''
  const [loading, setLoading] = useState(false)
  const [done, setDone] = useState(false)
  const [groupId, setGroupId] = useState<number | null>(null)

  const loggedIn = !!getToken()

  const handleAccept = async () => {
    if (!token) {
      toast({
        title: 'Link không hợp lệ',
        description: 'Thiếu mã token trong đường dẫn.',
        variant: 'destructive',
      })
      return
    }
    if (!getToken()) {
      router.push(`/?redirect=${encodeURIComponent(`/invitations/accept?token=${token}`)}`)
      return
    }
    setLoading(true)
    try {
      const group = await acceptInvitation(token)
      setGroupId(group.id)
      setDone(true)
      toast({ title: 'Tham gia nhóm thành công', description: group.name })
    } catch (err) {
      const message = err instanceof ApiError ? err.message : 'Không thể chấp nhận lời mời'
      toast({ title: 'Lỗi', description: message, variant: 'destructive' })
    } finally {
      setLoading(false)
    }
  }

  if (!token) {
    return (
      <CardShell>
        <Users className="mx-auto h-12 w-12 text-muted-foreground" />
        <h1 className="mt-4 text-xl font-semibold">Link mời không hợp lệ</h1>
        <p className="mt-2 text-sm text-muted-foreground">
          Không tìm thấy token. Kiểm tra lại link từ người mời.
        </p>
        <Button asChild className="mt-6">
          <Link href="/family">Về trang gia đình</Link>
        </Button>
      </CardShell>
    )
  }

  if (done && groupId) {
    return (
      <CardShell>
        <CheckCircle2 className="mx-auto h-12 w-12 text-success" />
        <h1 className="mt-4 text-xl font-semibold">Đã tham gia nhóm</h1>
        <p className="mt-2 text-sm text-muted-foreground">
          Bạn có thể xem chi tiết nhóm và quản lý thành viên.
        </p>
        <div className="mt-6 flex flex-col gap-2 sm:flex-row sm:justify-center">
          <Button asChild>
            <Link href={`/family/${groupId}`}>Vào nhóm</Link>
          </Button>
          <Button variant="outline" asChild>
            <Link href="/family">Danh sách nhóm</Link>
          </Button>
        </div>
      </CardShell>
    )
  }

  return (
    <CardShell>
      <Users className="mx-auto h-12 w-12 text-primary" />
      <h1 className="mt-4 text-xl font-semibold">Lời mời tham gia nhóm</h1>
      <p className="mt-2 text-sm text-muted-foreground">
        {loggedIn
          ? 'Email đăng nhập phải trùng với email được mời.'
          : 'Đăng nhập trước khi chấp nhận lời mời.'}
      </p>
      <div className="mt-6 flex flex-col gap-2">
        {loggedIn ? (
          <Button onClick={handleAccept} disabled={loading} className="gap-2">
            {loading && <Loader2 className="h-4 w-4 animate-spin" />}
            Chấp nhận lời mời
          </Button>
        ) : (
          <Button asChild className="gap-2">
            <Link href={`/?redirect=${encodeURIComponent(`/invitations/accept?token=${token}`)}`}>
              <LogIn className="h-4 w-4" />
              Đăng nhập để tiếp tục
            </Link>
          </Button>
        )}
        <Button variant="outline" asChild>
          <Link href="/family">Hủy</Link>
        </Button>
      </div>
    </CardShell>
  )
}

export default function AcceptInvitationPage() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-background p-6">
      <Suspense
        fallback={
          <div className="flex items-center gap-2 text-muted-foreground">
            <Loader2 className="h-5 w-5 animate-spin" />
            Đang tải…
          </div>
        }
      >
        <AcceptInvitationContent />
      </Suspense>
    </div>
  )
}
