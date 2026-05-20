'use client'

import { useCallback, useEffect, useMemo, useState } from 'react'
import Link from 'next/link'
import { useParams, useRouter } from 'next/navigation'
import { DashboardLayout } from '@/components/layout/dashboard-layout'
import { Header } from '@/components/layout/header'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Badge } from '@/components/ui/badge'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { GroupAccountsTab } from '@/components/family/group-accounts-tab'
import { GroupBudgetsTab } from '@/components/family/group-budgets-tab'
import { GroupByMemberTab } from '@/components/family/group-by-member-tab'
import { GroupOverviewTab } from '@/components/family/group-overview-tab'
import { GroupTransactionsTab } from '@/components/family/group-transactions-tab'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import {
  deleteGroup,
  getCurrentUserEmail,
  getGroup,
  initialsFromName,
  inviteMember,
  kickMember,
  leaveGroup,
  listMembers,
  listPendingInvitations,
  roleLabel,
  updateMemberRole,
  type Group,
  type GroupMember,
  type GroupRole,
  type Invitation,
} from '@/lib/groups'
import { ApiError } from '@/lib/api'
import { useToast } from '@/hooks/use-toast'
import {
  ArrowLeft,
  Check,
  Copy,
  Crown,
  Loader2,
  LogOut,
  Mail,
  PieChart,
  Shield,
  Trash2,
  UserMinus,
  UserPlus,
  Users,
  Wallet,
  Receipt,
  Target,
} from 'lucide-react'

export default function GroupDetailPage() {
  const params = useParams()
  const router = useRouter()
  const { toast } = useToast()
  const groupId = Number(params.id)

  const [group, setGroup] = useState<Group | null>(null)
  const [members, setMembers] = useState<GroupMember[]>([])
  const [invitations, setInvitations] = useState<Invitation[]>([])
  const [loading, setLoading] = useState(true)

  const [inviteOpen, setInviteOpen] = useState(false)
  const [inviteEmail, setInviteEmail] = useState('')
  const [inviting, setInviting] = useState(false)
  const [lastInviteLink, setLastInviteLink] = useState<string | null>(null)
  const [copied, setCopied] = useState(false)

  const [kickTarget, setKickTarget] = useState<GroupMember | null>(null)
  const [leaveOpen, setLeaveOpen] = useState(false)
  const [deleteOpen, setDeleteOpen] = useState(false)
  const [actionLoading, setActionLoading] = useState(false)
  const [activeTab, setActiveTab] = useState('overview')

  const isAdmin = group?.myRole === 'ADMIN'
  const myEmail = getCurrentUserEmail()
  const myMember = useMemo(
    () => members.find(m => m.email.toLowerCase() === (myEmail || '').toLowerCase()),
    [members, myEmail]
  )
  const isCreator = group && myMember && group.createdBy === myMember.userId

  const load = useCallback(async () => {
    if (!groupId || Number.isNaN(groupId)) return
    setLoading(true)
    try {
      const [g, m] = await Promise.all([getGroup(groupId), listMembers(groupId)])
      setGroup(g)
      setMembers(m)
      if (g.myRole === 'ADMIN') {
        try {
          const inv = await listPendingInvitations(groupId)
          setInvitations(inv)
        } catch {
          setInvitations([])
        }
      } else {
        setInvitations([])
      }
    } catch (err) {
      const message = err instanceof ApiError ? err.message : 'Không tải được nhóm'
      toast({ title: 'Lỗi', description: message, variant: 'destructive' })
      if (err instanceof ApiError && err.status === 403) {
        router.push('/family')
      }
    } finally {
      setLoading(false)
    }
  }, [groupId, router, toast])

  useEffect(() => {
    load()
  }, [load])

  const handleInvite = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!inviteEmail.trim()) return
    setInviting(true)
    try {
      const inv = await inviteMember(groupId, inviteEmail.trim())
      setLastInviteLink(inv.inviteLink)
      setInviteEmail('')
      toast({
        title: 'Đã gửi lời mời',
        description: `Link đã được tạo cho ${inv.email}`,
      })
      await load()
    } catch (err) {
      const message = err instanceof ApiError ? err.message : 'Mời thất bại'
      toast({ title: 'Lỗi', description: message, variant: 'destructive' })
    } finally {
      setInviting(false)
    }
  }

  const handleCopyLink = (link: string) => {
    navigator.clipboard.writeText(link)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  const handleRoleChange = async (userId: number, role: GroupRole) => {
    try {
      await updateMemberRole(groupId, userId, role)
      toast({ title: 'Đã cập nhật vai trò' })
      await load()
    } catch (err) {
      const message = err instanceof ApiError ? err.message : 'Cập nhật thất bại'
      toast({ title: 'Lỗi', description: message, variant: 'destructive' })
    }
  }

  const handleKick = async () => {
    if (!kickTarget) return
    setActionLoading(true)
    try {
      await kickMember(groupId, kickTarget.userId)
      toast({ title: 'Đã xóa thành viên khỏi nhóm' })
      setKickTarget(null)
      await load()
    } catch (err) {
      const message = err instanceof ApiError ? err.message : 'Kick thất bại'
      toast({ title: 'Lỗi', description: message, variant: 'destructive' })
    } finally {
      setActionLoading(false)
    }
  }

  const handleLeave = async () => {
    setActionLoading(true)
    try {
      await leaveGroup(groupId)
      toast({ title: 'Đã rời nhóm' })
      router.push('/family')
    } catch (err) {
      const message = err instanceof ApiError ? err.message : 'Không thể rời nhóm'
      toast({ title: 'Lỗi', description: message, variant: 'destructive' })
    } finally {
      setActionLoading(false)
      setLeaveOpen(false)
    }
  }

  const handleDeleteGroup = async () => {
    setActionLoading(true)
    try {
      await deleteGroup(groupId)
      toast({ title: 'Đã xóa nhóm' })
      router.push('/family')
    } catch (err) {
      const message = err instanceof ApiError ? err.message : 'Xóa nhóm thất bại'
      toast({ title: 'Lỗi', description: message, variant: 'destructive' })
    } finally {
      setActionLoading(false)
      setDeleteOpen(false)
    }
  }

  if (loading) {
    return (
      <DashboardLayout>
        <Header title="Chi tiết nhóm" subtitle="Đang tải…" />
        <div className="flex justify-center py-24">
          <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
        </div>
      </DashboardLayout>
    )
  }

  if (!group) {
    return (
      <DashboardLayout>
        <Header title="Không tìm thấy nhóm" />
        <div className="p-6">
          <Button asChild variant="outline">
            <Link href="/family">
              <ArrowLeft className="mr-2 h-4 w-4" />
              Quay lại
            </Link>
          </Button>
        </div>
      </DashboardLayout>
    )
  }

  return (
    <DashboardLayout>
      <Header
        title={group.name}
        subtitle={group.description || 'Tài chính chung & quản lý thành viên'}
      />

      <div className="p-6 space-y-6">
        <div className="flex flex-wrap items-center gap-3">
          <Button asChild variant="outline" size="sm">
            <Link href="/family">
              <ArrowLeft className="mr-2 h-4 w-4" />
              Danh sách nhóm
            </Link>
          </Button>
          <Badge variant={isAdmin ? 'default' : 'secondary'}>{roleLabel(group.myRole)}</Badge>
          <span className="text-sm text-muted-foreground">{group.memberCount} thành viên</span>

          <div className="ml-auto flex flex-wrap gap-2">
            {isAdmin && (
              <Dialog open={inviteOpen} onOpenChange={setInviteOpen}>
                <DialogTrigger asChild>
                  <Button size="sm" className="gap-2">
                    <UserPlus className="h-4 w-4" />
                    Mời thành viên
                  </Button>
                </DialogTrigger>
                <DialogContent>
                  <DialogHeader>
                    <DialogTitle>Mời qua email</DialogTitle>
                  </DialogHeader>
                  <form onSubmit={handleInvite} className="mt-4 space-y-4">
                    <div className="space-y-2">
                      <Label htmlFor="email">Email</Label>
                      <Input
                        id="email"
                        type="email"
                        placeholder="member@example.com"
                        value={inviteEmail}
                        onChange={e => setInviteEmail(e.target.value)}
                        required
                      />
                    </div>
                    {lastInviteLink && (
                      <div className="rounded-lg border border-border bg-secondary/50 p-3">
                        <p className="text-xs text-muted-foreground mb-2">Link mời (copy gửi cho họ):</p>
                        <div className="flex gap-2">
                          <code className="flex-1 truncate text-xs">{lastInviteLink}</code>
                          <Button
                            type="button"
                            variant="ghost"
                            size="icon"
                            onClick={() => handleCopyLink(lastInviteLink)}
                          >
                            {copied ? <Check className="h-4 w-4" /> : <Copy className="h-4 w-4" />}
                          </Button>
                        </div>
                      </div>
                    )}
                    <Button type="submit" className="w-full gap-2" disabled={inviting}>
                      {inviting && <Loader2 className="h-4 w-4 animate-spin" />}
                      Gửi lời mời
                    </Button>
                  </form>
                </DialogContent>
              </Dialog>
            )}
            <Button size="sm" variant="outline" className="gap-2" onClick={() => setLeaveOpen(true)}>
              <LogOut className="h-4 w-4" />
              Rời nhóm
            </Button>
            {isCreator && (
              <Button
                size="sm"
                variant="destructive"
                className="gap-2"
                onClick={() => setDeleteOpen(true)}
              >
                <Trash2 className="h-4 w-4" />
                Xóa nhóm
              </Button>
            )}
          </div>
        </div>

        <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full">
          <TabsList className="flex h-auto flex-wrap w-full justify-start gap-1">
            <TabsTrigger value="overview" className="gap-1.5">
              <PieChart className="h-4 w-4" />
              Tổng quan
            </TabsTrigger>
            <TabsTrigger value="transactions" className="gap-1.5">
              <Receipt className="h-4 w-4" />
              Giao dịch
            </TabsTrigger>
            <TabsTrigger value="accounts" className="gap-1.5">
              <Wallet className="h-4 w-4" />
              Tài khoản
            </TabsTrigger>
            <TabsTrigger value="budgets" className="gap-1.5">
              <Target className="h-4 w-4" />
              Ngân sách
            </TabsTrigger>
            <TabsTrigger value="by-member" className="gap-1.5">
              <Users className="h-4 w-4" />
              Theo thành viên
            </TabsTrigger>
            <TabsTrigger value="members" className="gap-1.5">
              <Shield className="h-4 w-4" />
              Thành viên
            </TabsTrigger>
          </TabsList>

          <TabsContent value="overview" className="mt-6">
            <GroupOverviewTab groupId={groupId} />
          </TabsContent>

          <TabsContent value="transactions" className="mt-6">
            <GroupTransactionsTab
              groupId={groupId}
              isAdmin={!!isAdmin}
              currentUserId={myMember?.userId}
            />
          </TabsContent>

          <TabsContent value="accounts" className="mt-6">
            <GroupAccountsTab groupId={groupId} isAdmin={!!isAdmin} />
          </TabsContent>

          <TabsContent value="budgets" className="mt-6">
            <GroupBudgetsTab groupId={groupId} isAdmin={!!isAdmin} />
          </TabsContent>

          <TabsContent value="by-member" className="mt-6">
            <GroupByMemberTab groupId={groupId} />
          </TabsContent>

          <TabsContent value="members" className="mt-6 space-y-6">
        {isAdmin && invitations.length > 0 && (
          <section className="rounded-xl border border-border bg-card p-5">
            <h3 className="font-semibold flex items-center gap-2">
              <Mail className="h-4 w-4" />
              Lời mời đang chờ ({invitations.length})
            </h3>
            <ul className="mt-3 space-y-2">
              {invitations.map(inv => (
                <li
                  key={inv.id}
                  className="flex flex-wrap items-center justify-between gap-2 rounded-lg bg-secondary/40 px-3 py-2 text-sm"
                >
                  <span>{inv.email}</span>
                  <div className="flex items-center gap-2">
                    <span className="text-xs text-muted-foreground">
                      Hết hạn: {new Date(inv.expiresAt).toLocaleDateString('vi-VN')}
                    </span>
                    <Button
                      variant="ghost"
                      size="icon"
                      className="h-7 w-7"
                      onClick={() => handleCopyLink(inv.inviteLink)}
                    >
                      <Copy className="h-3.5 w-3.5" />
                    </Button>
                  </div>
                </li>
              ))}
            </ul>
          </section>
        )}

        <section className="rounded-xl border border-border bg-card p-5">
          <h3 className="font-semibold flex items-center gap-2 mb-4">
            <Shield className="h-4 w-4" />
            Thành viên
          </h3>
          <ul className="divide-y divide-border">
            {members.map(m => {
              const isSelf = myMember?.userId === m.userId
              const canManage = isAdmin && !isSelf

              return (
                <li key={m.userId} className="flex flex-wrap items-center gap-4 py-4 first:pt-0 last:pb-0">
                  <Avatar className="h-10 w-10">
                    <AvatarFallback>{initialsFromName(m.fullName)}</AvatarFallback>
                  </Avatar>
                  <div className="min-w-0 flex-1">
                    <p className="font-medium text-foreground flex items-center gap-2">
                      {m.fullName}
                      {isSelf && (
                        <span className="text-xs text-muted-foreground">(bạn)</span>
                      )}
                      {m.role === 'ADMIN' && <Crown className="h-4 w-4 text-warning" />}
                    </p>
                    <p className="text-sm text-muted-foreground truncate">{m.email}</p>
                    <p className="text-xs text-muted-foreground mt-0.5">
                      Tham gia: {new Date(m.joinedAt).toLocaleDateString('vi-VN')}
                    </p>
                  </div>

                  {canManage && m.role === 'MEMBER' && (
                    <div className="flex flex-wrap items-center gap-2">
                      <Select
                        value={m.role}
                        onValueChange={v => handleRoleChange(m.userId, v as GroupRole)}
                      >
                        <SelectTrigger className="w-[130px] h-8">
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="MEMBER">Thành viên</SelectItem>
                          <SelectItem value="ADMIN">Quản trị</SelectItem>
                        </SelectContent>
                      </Select>
                      <Button
                        variant="outline"
                        size="sm"
                        className="gap-1 text-destructive hover:text-destructive"
                        onClick={() => setKickTarget(m)}
                      >
                        <UserMinus className="h-4 w-4" />
                        Kick
                      </Button>
                    </div>
                  )}

                  {canManage && m.role === 'ADMIN' && (
                    <Select
                      value={m.role}
                      onValueChange={v => handleRoleChange(m.userId, v as GroupRole)}
                    >
                      <SelectTrigger className="w-[130px] h-8">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="ADMIN">Quản trị</SelectItem>
                        <SelectItem value="MEMBER">Thành viên</SelectItem>
                      </SelectContent>
                    </Select>
                  )}

                  {!canManage && (
                    <Badge variant={m.role === 'ADMIN' ? 'default' : 'secondary'}>
                      {roleLabel(m.role)}
                    </Badge>
                  )}
                </li>
              )
            })}
          </ul>
        </section>
          </TabsContent>
        </Tabs>
      </div>

      <AlertDialog open={!!kickTarget} onOpenChange={open => !open && setKickTarget(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Xóa thành viên?</AlertDialogTitle>
            <AlertDialogDescription>
              {kickTarget?.fullName} sẽ bị xóa khỏi nhóm và không còn truy cập dữ liệu chung.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Hủy</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleKick}
              disabled={actionLoading}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              Xóa khỏi nhóm
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      <AlertDialog open={leaveOpen} onOpenChange={setLeaveOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Rời nhóm?</AlertDialogTitle>
            <AlertDialogDescription>
              Bạn sẽ không còn là thành viên của &quot;{group.name}&quot;.
              {isAdmin && ' Nếu bạn là ADMIN duy nhất, hãy chỉ định ADMIN khác trước.'}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Hủy</AlertDialogCancel>
            <AlertDialogAction onClick={handleLeave} disabled={actionLoading}>
              Rời nhóm
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      <AlertDialog open={deleteOpen} onOpenChange={setDeleteOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Xóa nhóm vĩnh viễn?</AlertDialogTitle>
            <AlertDialogDescription>
              Toàn bộ thành viên và lời mời sẽ bị xóa. Hành động không thể hoàn tác.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Hủy</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDeleteGroup}
              disabled={actionLoading}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              Xóa nhóm
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </DashboardLayout>
  )
}
