# 🏃 Sprint 4 — Daily Breakdown (Day 4–6)
**Family Group Management | Member Management + FE Integration**

---

## 📅 DAY 4 — Member Management + Tests + Postman
**6 tiếng | Thứ Năm**

### 🌅 Daily Standup
```
✅ Hôm qua: Invitation flow (invite + accept) + GET members xong
🎯 Hôm nay: Kick member, đổi role, business rules, unit tests, Postman
🚧 Blocker: Accept invitation flow đã test thủ công chưa? Test ngay đầu ngày
```

---

### ⏰ 09:00–10:00 | T15: DELETE /groups/{id}/members/{userId} — Kick Member (1h)

**Endpoint:** `DELETE /api/groups/{groupId}/members/{userId}`

**Logic:**
```java
@Transactional
public void kickMember(Long groupId, Long targetUserId, Long currentUserId) {

    // 1. Người thực hiện phải là ADMIN
    groupAuthService.requireAdmin(groupId, currentUserId);

    // 2. Target phải là member của group
    GroupMember target = groupMemberRepository
        .findByGroupIdAndUserId(groupId, targetUserId)
        .orElseThrow(() -> new ResourceNotFoundException(
            "Người dùng này không phải thành viên của nhóm"));

    // 3. ADMIN không thể tự kick mình
    if (targetUserId.equals(currentUserId)) {
        throw new BusinessException(
            "Không thể tự kick chính mình. Hãy dùng chức năng Rời nhóm.");
    }

    // 4. Không thể kick ADMIN khác (chỉ Creator mới kick được ADMIN)
    // → Đơn giản hóa: chỉ ADMIN kick MEMBER, không kick ADMIN khác
    if (target.getRole() == GroupRole.ADMIN) {
        throw new BusinessException(
            "Không thể kick ADMIN khác. Hãy đổi role của họ xuống MEMBER trước.");
    }

    groupMemberRepository.delete(target);
}
```

**HTTP Response:** 204 No Content

**✅ Done khi:**
- ADMIN kick MEMBER → thành công, member biến mất khỏi danh sách
- MEMBER kick người khác → 403
- ADMIN tự kick mình → lỗi rõ ràng
- ADMIN kick ADMIN khác → lỗi rõ ràng

---

### ⏰ 10:00–11:00 | T16: PATCH /groups/{id}/members/{userId}/role (1h)

**Endpoint:** `PATCH /api/groups/{groupId}/members/{userId}/role`

**Body:** `{ "role": "ADMIN" }` hoặc `{ "role": "MEMBER" }`

**Logic:**
```java
@Transactional
public GroupMemberResponse updateMemberRole(Long groupId, Long targetUserId,
                                             GroupRole newRole, Long currentUserId) {
    // 1. Chỉ ADMIN mới đổi role
    groupAuthService.requireAdmin(groupId, currentUserId);

    // 2. Target phải là member
    GroupMember target = groupMemberRepository
        .findByGroupIdAndUserId(groupId, targetUserId)
        .orElseThrow(() -> new ResourceNotFoundException("Member không tồn tại trong group"));

    // 3. Không thể tự đổi role của mình
    if (targetUserId.equals(currentUserId)) {
        throw new BusinessException("Không thể tự thay đổi role của chính mình");
    }

    // 4. Đổi role
    target.setRole(newRole);
    groupMemberRepository.save(target);

    User user = userRepository.findById(targetUserId).orElseThrow();
    return mapToMemberResponse(target, user);
}
```

**✅ Done khi:**
- ADMIN đổi MEMBER → ADMIN thành công
- ADMIN đổi ADMIN → MEMBER thành công (downgrade)
- MEMBER thực hiện → 403
- Đổi role của chính mình → lỗi

---

### ⏰ 11:00–12:30 | T17: Business Rules — Leave Group + Edge Cases (1.5h)

**Thêm endpoint:** `DELETE /api/groups/{groupId}/members/me` — Tự rời nhóm

```java
@Transactional
public void leaveGroup(Long groupId, Long currentUserId) {

    GroupMember member = groupMemberRepository
        .findByGroupIdAndUserId(groupId, currentUserId)
        .orElseThrow(() -> new ResourceNotFoundException("Bạn không phải thành viên nhóm này"));

    // Nếu là ADMIN duy nhất → không được rời
    if (member.getRole() == GroupRole.ADMIN) {
        long adminCount = groupMemberRepository.findByGroupId(groupId)
            .stream().filter(m -> m.getRole() == GroupRole.ADMIN).count();
        if (adminCount <= 1) {
            throw new BusinessException(
                "Bạn là ADMIN duy nhất. Hãy chỉ định ADMIN khác trước khi rời nhóm.");
        }
    }

    groupMemberRepository.delete(member);
}
```

**Thêm endpoint:** `DELETE /api/groups/{groupId}` — ADMIN xóa cả nhóm

```java
@Transactional
public void deleteGroup(Long groupId, Long currentUserId) {
    groupAuthService.requireAdmin(groupId, currentUserId);

    // Chỉ creator mới xóa được group (không phải mọi ADMIN)
    FamilyGroup group = familyGroupRepository.findById(groupId)
        .orElseThrow(() -> new ResourceNotFoundException("Group không tồn tại"));
    if (!group.getCreatedBy().equals(currentUserId)) {
        throw new ForbiddenException("Chỉ người tạo nhóm mới có thể xóa nhóm");
    }

    // ON DELETE CASCADE sẽ tự xóa group_members + invitations
    familyGroupRepository.delete(group);
}
```

**GET /api/groups/{id}/invitations** — ADMIN xem pending invitations:
```java
// Chỉ ADMIN xem được
groupAuthService.requireAdmin(groupId, currentUserId);
return groupInvitationRepository
    .findByGroupIdAndStatus(groupId, InvitationStatus.PENDING);
```

---

### ⏰ 13:00–15:00 | T18: Unit Tests (2h)

**Invitation tests:**
- `inviteMember_success()` — ADMIN invite → invitation tạo ra
- `inviteMember_notAdmin()` → `ForbiddenException`
- `inviteMember_alreadyMember()` → `BusinessException`
- `inviteMember_duplicatePending()` — cái cũ CANCELLED, cái mới tạo ra
- `acceptInvitation_success()` — join group thành công
- `acceptInvitation_tokenNotFound()` → `ResourceNotFoundException`
- `acceptInvitation_expired()` → `BusinessException`
- `acceptInvitation_emailMismatch()` → `ForbiddenException`
- `acceptInvitation_alreadyMember()` → `BusinessException`

**Member management tests:**
- `kickMember_success()` — MEMBER bị kick
- `kickMember_notAdmin()` → `ForbiddenException`
- `kickMember_selfKick()` → `BusinessException`
- `kickMember_kickAdmin()` → `BusinessException`
- `updateRole_success()` — MEMBER → ADMIN
- `leaveGroup_lastAdmin()` → `BusinessException`
- `leaveGroup_success()` — MEMBER rời nhóm OK

---

### ⏰ 15:00–16:00 | T19: Postman Collection Sprint 4 (1h)

```
📁 Family Groups (Sprint 4)
  ├── POST /api/groups                          ← tạo group
  ├── GET  /api/groups                          ← list groups của tôi
  ├── GET  /api/groups/{id}                     ← chi tiết group
  ├── DELETE /api/groups/{id}                   ← xóa group (creator)
  ├── GET  /api/groups/{id}/members             ← danh sách member
  ├── POST /api/groups/{id}/invite              ← mời thành viên
  ├── GET  /api/groups/{id}/invitations         ← pending invitations
  ├── POST /api/invitations/accept?token=       ← accept join
  ├── PATCH /api/groups/{id}/members/{uid}/role ← đổi role
  ├── DELETE /api/groups/{id}/members/{uid}     ← kick member
  └── DELETE /api/groups/{id}/members/me        ← tự rời nhóm
```

**Postman test script — Authorization:**
```javascript
// Test: MEMBER không kick được người khác
pm.test("MEMBER cannot kick", () => {
    pm.response.to.have.status(403);
});
```

**Commit Day 4:**
```bash
git commit -m "feat: kick member, change role, leave group, delete group, unit tests, Postman Sprint 4"
```

---

## 📅 DAY 5 — FE Integration: Group Management
**6 tiếng | Thứ Sáu**

### 🌅 Daily Standup
```
✅ Hôm qua: Member management + tests xong
🎯 Hôm nay: Kết nối FE với Group APIs
🚧 Blocker: FE có sẵn trang Group management chưa? List các pages/components cần kết nối
```

---

### ⏰ 09:00–10:00 | T20a: Group List + Create Group (1h)

**Group list page:**
```javascript
// Load danh sách groups của user
useEffect(() => {
    api.get('/groups').then(res => setGroups(res.data.data));
}, []);
```

**Create group form:**
```javascript
const handleCreate = async (data) => {
    const res = await api.post('/groups', {
        name: data.name,
        description: data.description
    });
    // Redirect vào group detail vừa tạo
    navigate(`/groups/${res.data.data.id}`);
};
```

**Display group card:**
```jsx
<div className="group-card">
    <h3>{group.name}</h3>
    <span>{group.memberCount} thành viên</span>
    <span className={`role-badge ${group.myRole}`}>
        {group.myRole === 'ADMIN' ? '👑 Admin' : '👤 Thành viên'}
    </span>
</div>
```

---

### ⏰ 10:00–12:00 | T20b: Group Detail + Member List (2h)

**Group detail page — Load data:**
```javascript
const loadGroupDetail = async () => {
    const [groupRes, membersRes, invitationsRes] = await Promise.all([
        api.get(`/groups/${groupId}`),
        api.get(`/groups/${groupId}/members`),
        isAdmin ? api.get(`/groups/${groupId}/invitations`) : Promise.resolve(null)
    ]);
    setGroup(groupRes.data.data);
    setMembers(membersRes.data.data);
    if (isAdmin) setPendingInvitations(invitationsRes.data.data);
};
```

**Member list với actions:**
```jsx
{members.map(member => (
    <div key={member.userId} className="member-row">
        <span>{member.fullName}</span>
        <span>{member.email}</span>
        <span className={`role ${member.role}`}>{member.role}</span>
        {isAdmin && member.userId !== currentUserId && (
            <>
                {member.role === 'MEMBER' && (
                    <button onClick={() => handlePromote(member.userId)}>
                        Đặt làm Admin
                    </button>
                )}
                {member.role === 'ADMIN' && (
                    <button onClick={() => handleDemote(member.userId)}>
                        Hạ xuống Member
                    </button>
                )}
                {member.role === 'MEMBER' && (
                    <button onClick={() => handleKick(member.userId)}
                            className="danger">
                        Kick
                    </button>
                )}
            </>
        )}
    </div>
))}
```

**Actions:**
```javascript
const handleKick = async (userId) => {
    if (!window.confirm('Bạn chắc chắn muốn kick thành viên này?')) return;
    await api.delete(`/groups/${groupId}/members/${userId}`);
    loadGroupDetail(); // reload
};

const handlePromote = async (userId) => {
    await api.patch(`/groups/${groupId}/members/${userId}/role`, { role: 'ADMIN' });
    loadGroupDetail();
};

const handleDemote = async (userId) => {
    await api.patch(`/groups/${groupId}/members/${userId}/role`, { role: 'MEMBER' });
    loadGroupDetail();
};
```

---

### ⏰ 13:00–15:00 | T21: Invitation Flow FE (2h)

**Invite form (chỉ ADMIN thấy):**
```javascript
const handleInvite = async (e) => {
    e.preventDefault();
    try {
        const res = await api.post(`/groups/${groupId}/invite`, { email });
        // Hiển thị invite link để copy
        setInviteLink(res.data.data.inviteLink);
        setSuccess('Đã gửi lời mời! Copy link bên dưới để chia sẻ.');
    } catch (err) {
        setError(err.response?.data?.message || 'Có lỗi xảy ra');
    }
};

// Hiển thị link sau khi invite thành công
{inviteLink && (
    <div className="invite-link-box">
        <input readOnly value={inviteLink} />
        <button onClick={() => navigator.clipboard.writeText(inviteLink)}>
            Copy link
        </button>
    </div>
)}
```

**Accept invitation page** — Route: `/invitations/accept?token=xxx`

```javascript
// Component này load khi user click link trong email/console
const AcceptInvitationPage = () => {
    const [searchParams] = useSearchParams();
    const token = searchParams.get('token');
    const navigate = useNavigate();
    const [status, setStatus] = useState('loading'); // loading | success | error
    const [message, setMessage] = useState('');

    useEffect(() => {
        if (!token) {
            setStatus('error');
            setMessage('Link không hợp lệ');
            return;
        }

        api.post(`/invitations/accept?token=${token}`)
            .then(res => {
                setStatus('success');
                // Redirect vào group sau 2 giây
                setTimeout(() => navigate(`/groups/${res.data.data.id}`), 2000);
            })
            .catch(err => {
                setStatus('error');
                setMessage(err.response?.data?.message || 'Có lỗi xảy ra');
            });
    }, [token]);

    return (
        <div className="accept-page">
            {status === 'loading' && <p>Đang xử lý lời mời...</p>}
            {status === 'success' && <p>✅ Tham gia nhóm thành công! Đang chuyển hướng...</p>}
            {status === 'error'   && <p>❌ {message}</p>}
        </div>
    );
};
```

**Thêm route vào Router:**
```jsx
<Route path="/invitations/accept" element={<AcceptInvitationPage />} />
```

**Leave group button:**
```javascript
const handleLeaveGroup = async () => {
    if (!window.confirm('Bạn chắc chắn muốn rời nhóm này?')) return;
    try {
        await api.delete(`/groups/${groupId}/members/me`);
        navigate('/groups'); // về danh sách groups
    } catch (err) {
        setError(err.response?.data?.message); // "Bạn là ADMIN duy nhất..."
    }
};
```

---

### ⏰ 15:00–16:00 | T22: Integration Test Quick (1h)

Test flow 2 user:
```
User A:
1. Tạo group "Gia đình Nguyễn"  ✅/❌
2. GET /api/groups → thấy group vừa tạo ✅/❌
3. Invite "userB@gmail.com"     ✅/❌
4. Copy invite link từ response  ✅/❌

User B:
5. Đăng nhập bằng tài khoản userB
6. Truy cập invite link → Accept ✅/❌
7. GET /api/groups → thấy group "Gia đình Nguyễn" ✅/❌
8. GET /api/groups/{id}/members → thấy cả A và B ✅/❌

User A (ADMIN actions):
9. PATCH role userB → ADMIN ✅/❌
10. PATCH role userB → MEMBER ✅/❌
11. DELETE members/userB (kick) ✅/❌
12. GET members → chỉ còn User A ✅/❌

Isolation test:
13. User B thử GET /api/groups/{id} → 403 (sau khi bị kick) ✅/❌
```

**Commit Day 5:**
```bash
git commit -m "feat: Group management FE integrated (list, detail, invite, accept, kick, role), AcceptInvitationPage"
```

---

## 📅 DAY 6 — Bug Fix + Data Isolation Audit + Sprint Review
**6 tiếng | Thứ Bảy**

### 🌅 Daily Standup
```
✅ Hôm qua: FE Group management kết nối BE thật
🎯 Hôm nay: Security audit, bug fix, Sprint Review
🚧 Blocker: Liệt kê tất cả bug từ Day 4-5
```

---

### ⏰ 09:00–11:30 | T23: Security Audit + Bug Fix (2.5h)

**Security Checklist — Group APIs:**

```
Authorization:
[ ] GET /api/groups → chỉ trả groups mình thuộc về
[ ] GET /api/groups/{id} → user không phải member → 403
[ ] POST /api/groups/{id}/invite → MEMBER → 403
[ ] DELETE /api/groups/{id}/members/{uid} → MEMBER → 403
[ ] PATCH /api/groups/{id}/members/{uid}/role → MEMBER → 403
[ ] DELETE /api/groups/{id} → ADMIN nhưng không phải creator → 403

Invitation:
[ ] Token hết hạn (giả lập expiresAt trong quá khứ) → lỗi rõ
[ ] Token đã dùng (status ACCEPTED) → lỗi rõ
[ ] Accept với email khác → 403
[ ] Token không tồn tại → 404

Business Rules:
[ ] ADMIN tự kick mình → lỗi
[ ] ADMIN tự đổi role mình → lỗi
[ ] ADMIN duy nhất leave group → lỗi
[ ] Invite email đã là member → lỗi
```

**Common bugs khi kết nối FE:**

| Bug | Fix |
|-----|-----|
| Buttons ADMIN action hiện với MEMBER | Check `group.myRole === 'ADMIN'` trước khi render |
| Accept page không load nếu chưa login | Wrap route trong `ProtectedRoute` |
| Invite link copy không work | Đảm bảo `navigator.clipboard` dùng trong HTTPS hoặc localhost |
| Member list không reload sau kick | Gọi lại `loadGroupDetail()` sau mỗi action |
| Role badge hiển thị sai sau update | State update phải trigger re-render |

---

### ⏰ 11:30–13:00 | Sprint Review (1h)

**Deliverables Checklist:**

| # | Deliverable | Status |
|---|------------|--------|
| 1 | ADMIN tạo group → tự động là ADMIN | ⬜ |
| 2 | Invite member qua email (console link) | ⬜ |
| 3 | Accept invitation → join group thành công | ⬜ |
| 4 | Token expired/used/email mismatch → lỗi rõ | ⬜ |
| 5 | GET /api/groups/{id}/members → đầy đủ thông tin | ⬜ |
| 6 | ADMIN kick MEMBER | ⬜ |
| 7 | ADMIN đổi role MEMBER ↔ ADMIN | ⬜ |
| 8 | Leave group (ADMIN duy nhất bị block) | ⬜ |
| 9 | Authorization: mọi group API đều check đúng | ⬜ |
| 10 | FE: Group list, detail, invite form, accept page | ⬜ |
| 11 | Unit tests GREEN (10+ cases) | ⬜ |

**Demo Script (7 phút):**
```
1. User A tạo group "Gia đình Nguyễn"          (1 phút)
2. User A invite User B → copy link             (1 phút)
3. User B login → click link → join group      (1 phút)
4. User A xem member list → thấy cả 2          (30 giây)
5. User A promote User B → ADMIN               (30 giây)
6. User A kick... lỗi (B là ADMIN)             (30 giây)
7. User A demote B → MEMBER → kick thành công  (1 phút)
8. User B thử vào group → 403                  (30 giây)
```

---

### ⏰ 13:00–13:30 | Sprint Retrospective (30 phút)

```
✅ Went WELL:
   Ví dụ: GroupAuthorizationService tái sử dụng tốt,
   không phải lặp lại logic check ở nhiều chỗ

⚠️ Could IMPROVE:
   Ví dụ: Accept invitation cần test nhiều edge case hơn,
   nên viết test trước khi code (TDD)

🚀 Next Sprint differently:
   Ví dụ: Sprint 5 sẽ reuse GroupAuthorizationService
   ngay từ đầu, không phải refactor sau
```

---

### ⏰ 13:30–14:00 | Final Commit + Tag

```bash
git add .
git commit -m "chore: security audit, bug fixes, integration verified, Sprint 4 complete"
git push origin main

git tag -a sprint-4 -m "Sprint 4: Family Group Management"
git push origin sprint-4
```

---

## 📊 Sprint 4 Summary

### Thời Gian Phân Bổ
```
Day 1: Migrations + Entities + DTO         → 6h
Day 2: Group CRUD + Auth Service + Email   → 6h  ← Authorization service quan trọng nhất
Day 3: Invite flow + Accept + GET members  → 6h  ← Business logic phức tạp nhất
Day 4: Kick + Role + Leave + Tests + Postman → 6h
Day 5: FE Integration (group + invitation) → 6h
Day 6: Security audit + Bug fix + Review   → 6h
────────────────────────────────────────────────
Total:                                       36h
```

### Nếu Bị Trễ — Cắt Theo Thứ Tự
```
Giữ:   Group CRUD + Invite + Accept + GroupAuthorizationService
Giảm:  Leave group (tự làm thủ công qua console)
Dời:   PATCH role (có thể làm đầu Sprint 5)
Bỏ:    GET /invitations (ADMIN không cần xem pending list)
```

### Mang Vào Sprint 5
`GroupAuthorizationService` đã tạo ở Sprint 4 sẽ được dùng lại ở **mọi** Shared Transaction/Account/Budget API của Sprint 5.

---

*Sprint 4 Complete → Sẵn sàng Sprint 5: Shared Accounts, Transactions & Budget* 🚀
