'use client'

import { useCallback, useEffect, useState } from 'react'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { DashboardLayout } from '@/components/layout/dashboard-layout'
import { Header } from '@/components/layout/header'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Badge } from '@/components/ui/badge'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import { PendingInvitationsPanel } from '@/components/family/pending-invitations-panel'
import {
  createGroup,
  listGroups,
  roleLabel,
  type Group,
} from '@/lib/groups'
import { ApiError } from '@/lib/api'
import { useToast } from '@/hooks/use-toast'
import { ChevronRight, Loader2, Plus, Users, UserPlus } from 'lucide-react'

export default function FamilyPage() {
  const router = useRouter()
  const { toast } = useToast()
  const [groups, setGroups] = useState<Group[]>([])
  const [loading, setLoading] = useState(true)

  const [isCreateOpen, setIsCreateOpen] = useState(false)
  const [createName, setCreateName] = useState('')
  const [createDesc, setCreateDesc] = useState('')
  const [creating, setCreating] = useState(false)

  const [isJoinOpen, setIsJoinOpen] = useState(false)
  const [joinToken, setJoinToken] = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const data = await listGroups()
      setGroups(data)
    } catch (err) {
      const message = err instanceof ApiError ? err.message : 'Không tải được danh sách nhóm'
      toast({ title: 'Lỗi', description: message, variant: 'destructive' })
      setGroups([])
    } finally {
      setLoading(false)
    }
  }, [toast])

  useEffect(() => {
    load()
  }, [load])

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!createName.trim()) return
    setCreating(true)
    try {
      const g = await createGroup({
        name: createName.trim(),
        description: createDesc.trim() || undefined,
      })
      setIsCreateOpen(false)
      setCreateName('')
      setCreateDesc('')
      toast({ title: 'Đã tạo nhóm', description: g.name })
      router.push(`/family/${g.id}`)
    } catch (err) {
      const message = err instanceof ApiError ? err.message : 'Tạo nhóm thất bại'
      toast({ title: 'Lỗi', description: message, variant: 'destructive' })
    } finally {
      setCreating(false)
    }
  }

  const handleJoinByToken = (e: React.FormEvent) => {
    e.preventDefault()
    const raw = joinToken.trim()
    if (!raw) return
    let token = raw
    try {
      if (raw.includes('token=')) {
        const u = new URL(raw.startsWith('http') ? raw : `http://x?${raw.split('?')[1] || raw}`)
        token = u.searchParams.get('token') || raw
      }
    } catch {
      // dùng raw làm token
    }
    setIsJoinOpen(false)
    setJoinToken('')
    router.push(`/invitations/accept?token=${encodeURIComponent(token)}`)
  }

  return (
    <DashboardLayout>
      <Header
        title="Nhóm gia đình"
        subtitle="Tạo nhóm, mời thành viên và quản lý quyền"
      />

      <div className="p-6">
        <PendingInvitationsPanel onChanged={load} />

        <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
          <p className="text-sm text-muted-foreground">
            {loading ? 'Đang tải…' : `${groups.length} nhóm bạn tham gia`}
          </p>
          <div className="flex flex-wrap gap-2">
            <Dialog open={isJoinOpen} onOpenChange={setIsJoinOpen}>
              <DialogTrigger asChild>
                <Button variant="outline" className="gap-2">
                  <UserPlus className="h-4 w-4" />
                  Tham gia bằng link
                </Button>
              </DialogTrigger>
              <DialogContent className="sm:max-w-md">
                <DialogHeader>
                  <DialogTitle>Tham gia nhóm</DialogTitle>
                </DialogHeader>
                <form onSubmit={handleJoinByToken} className="mt-4 space-y-4">
                  <div className="space-y-2">
                    <Label htmlFor="inviteToken">Link hoặc mã mời</Label>
                    <Input
                      id="inviteToken"
                      placeholder="Dán link .../invitations/accept?token=..."
                      value={joinToken}
                      onChange={e => setJoinToken(e.target.value)}
                    />
                  </div>
                  <Button type="submit" className="w-full">
                    Tiếp tục
                  </Button>
                </form>
              </DialogContent>
            </Dialog>

            <Dialog open={isCreateOpen} onOpenChange={setIsCreateOpen}>
              <DialogTrigger asChild>
                <Button className="gap-2">
                  <Plus className="h-4 w-4" />
                  Tạo nhóm
                </Button>
              </DialogTrigger>
              <DialogContent className="sm:max-w-md">
                <DialogHeader>
                  <DialogTitle>Tạo nhóm gia đình</DialogTitle>
                </DialogHeader>
                <form onSubmit={handleCreate} className="mt-4 space-y-4">
                  <div className="space-y-2">
                    <Label htmlFor="familyName">Tên nhóm</Label>
                    <Input
                      id="familyName"
                      placeholder="VD: Gia đình Nguyễn"
                      value={createName}
                      onChange={e => setCreateName(e.target.value)}
                      maxLength={100}
                      required
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="familyDesc">Mô tả (tuỳ chọn)</Label>
                    <Textarea
                      id="familyDesc"
                      placeholder="Mô tả ngắn về nhóm"
                      value={createDesc}
                      onChange={e => setCreateDesc(e.target.value)}
                      rows={2}
                    />
                  </div>
                  <div className="flex gap-3 pt-2">
                    <Button
                      type="button"
                      variant="outline"
                      className="flex-1"
                      onClick={() => setIsCreateOpen(false)}
                    >
                      Hủy
                    </Button>
                    <Button type="submit" className="flex-1 gap-2" disabled={creating}>
                      {creating && <Loader2 className="h-4 w-4 animate-spin" />}
                      Tạo
                    </Button>
                  </div>
                </form>
              </DialogContent>
            </Dialog>
          </div>
        </div>

        {loading ? (
          <div className="flex justify-center py-20">
            <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
          </div>
        ) : groups.length === 0 ? (
          <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-border py-20 text-center">
            <Users className="h-12 w-12 text-muted-foreground" />
            <h2 className="mt-4 text-lg font-semibold">Chưa có nhóm nào</h2>
            <p className="mt-1 max-w-sm text-sm text-muted-foreground">
              Tạo nhóm mới hoặc tham gia bằng link mời từ quản trị viên.
            </p>
          </div>
        ) : (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {groups.map(g => (
              <Link
                key={g.id}
                href={`/family/${g.id}`}
                className="group rounded-xl border border-border bg-card p-5 transition-shadow hover:shadow-md"
              >
                <div className="flex items-start justify-between gap-2">
                  <div className="min-w-0 flex-1">
                    <h3 className="truncate font-semibold text-foreground group-hover:text-primary">
                      {g.name}
                    </h3>
                    {g.description && (
                      <p className="mt-1 line-clamp-2 text-sm text-muted-foreground">
                        {g.description}
                      </p>
                    )}
                  </div>
                  <ChevronRight className="h-5 w-5 shrink-0 text-muted-foreground" />
                </div>
                <div className="mt-4 flex flex-wrap items-center gap-2">
                  <Badge variant={g.myRole === 'ADMIN' ? 'default' : 'secondary'}>
                    {roleLabel(g.myRole)}
                  </Badge>
                  <span className="text-xs text-muted-foreground">
                    {g.memberCount} thành viên
                  </span>
                </div>
              </Link>
            ))}
          </div>
        )}
      </div>
    </DashboardLayout>
  )
}
