# 🏃 Sprint 4 — Daily Breakdown (Day 1–3)
**Family Group Management | 6 ngày × 6h = 36h**

---

## Sprint Planning (30 phút — trước Day 1)

**Sprint Goal:** ADMIN tạo được nhóm gia đình, mời thành viên qua email/link, quản lý role. Hệ thống phân quyền trong group hoạt động chính xác.

> ⚠️ **Sprint này là nền tảng cho Family Finance.** Authorization logic phải đúng từ đầu — sai ở đây sẽ kéo theo bug ở Sprint 5 (Shared Transactions).

---

## Backlog Sprint 4

| ID | Task | Giờ | Ngày |
|----|------|-----|------|
| T01 | Migration V6: bảng family_groups | 0.5h | Day 1 |
| T02 | Migration V7: bảng group_members | 0.5h | Day 1 |
| T03 | Migration V8: bảng group_invitations | 0.5h | Day 1 |
| T04 | Entity: FamilyGroup, GroupMember, GroupInvitation | 2h | Day 1 |
| T05 | Enum: GroupRole (ADMIN/MEMBER) | 0.5h | Day 1 |
| T06 | Repository: 3 interfaces | 1h | Day 1 |
| T07 | DTO layer Sprint 4 | 1.5h | Day 1 |
| T08 | Group CRUD API (create/get list/get detail) | 2h | Day 2 |
| T09 | GroupAuthorizationService | 2h | Day 2 |
| T10 | Unit test GroupService | 1.5h | Day 2 |
| T11 | Email/Console invitation service | 1h | Day 2 |
| T12 | POST /groups/{id}/invite (tạo invitation) | 2h | Day 3 |
| T13 | POST /invitations/accept?token= (join group) | 2h | Day 3 |
| T14 | GET /groups/{id}/members (xem danh sách) | 1h | Day 3 |
| T15 | DELETE /groups/{id}/members/{userId} (kick) | 1h | Day 4 |
| T16 | PATCH /groups/{id}/members/{userId}/role (đổi role) | 1h | Day 4 |
| T17 | Edge cases & business rules | 1.5h | Day 4 |
| T18 | Unit test Invitation + Member management | 2h | Day 4 |
| T19 | Postman collection Sprint 4 | 1h | Day 4 |
| T20 | FE integration: Group management pages | 3h | Day 5 |
| T21 | FE integration: Invitation flow | 2h | Day 5 |
| T22 | Integration test end-to-end | 1h | Day 5 |
| T23 | Bug fix + data isolation verification | 2.5h | Day 6 |
| T24 | Sprint Review + Retro | 1h | Day 6 |

---

## 📅 DAY 1 — Migrations, Entities & DTOs
**6 tiếng | Thứ Hai**

### 🌅 Daily Standup
```
✅ Hôm qua: Sprint 3 xong — Personal MVP hoạt động
🎯 Hôm nay: 3 migration + 3 entity + DTO layer cho Family features
🚧 Blocker: Sprint 3 deliverables ổn chưa? Kiểm tra Summary API + Budget Status
```

---

### ⏰ 09:00–10:00 | T01–T03: 3 Migrations (1h)

**`V6__create_family_groups_table.sql`:**
```sql
CREATE TABLE family_groups (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    created_by  BIGINT NOT NULL,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_group_creator FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**`V7__create_group_members_table.sql`:**
```sql
CREATE TABLE group_members (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id  BIGINT NOT NULL,
    user_id   BIGINT NOT NULL,
    role      ENUM('ADMIN','MEMBER') NOT NULL DEFAULT 'MEMBER',
    joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_gm_group FOREIGN KEY (group_id) REFERENCES family_groups(id) ON DELETE CASCADE,
    CONSTRAINT fk_gm_user  FOREIGN KEY (user_id)  REFERENCES users(id),
    UNIQUE KEY uk_group_member (group_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**`V8__create_group_invitations_table.sql`:**
```sql
CREATE TABLE group_invitations (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id   BIGINT NOT NULL,
    email      VARCHAR(255) NOT NULL,
    token      VARCHAR(64) NOT NULL UNIQUE,
    status     ENUM('PENDING','ACCEPTED','EXPIRED','CANCELLED') NOT NULL DEFAULT 'PENDING',
    expires_at DATETIME NOT NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inv_group   FOREIGN KEY (group_id)   REFERENCES family_groups(id),
    CONSTRAINT fk_inv_creator FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**✅ Done khi:** App start, Flyway log `"Successfully applied 8 migrations"`

---

### ⏰ 10:00–12:00 | T04–T05: Entities + Enum (2h)

**Enum `GroupRole`:**
```java
public enum GroupRole { ADMIN, MEMBER }
```

**Entity `FamilyGroup`:**
- id, name, description
- createdBy (Long — userId, không cần @ManyToOne để tránh N+1)
- createdAt, updatedAt

**Entity `GroupMember`:**
- id, groupId (Long), userId (Long)
- role (GroupRole enum)
- joinedAt (LocalDateTime)

**Entity `GroupInvitation`:**
- id, groupId (Long), email, token, status (InvitationStatus enum)
- expiresAt (LocalDateTime), createdBy (Long), createdAt

**Enum `InvitationStatus`:** PENDING, ACCEPTED, EXPIRED, CANCELLED

> 💡 **Lưu ý thiết kế:** Dùng `Long groupId` thay vì `@ManyToOne FamilyGroup group` trong `GroupMember` để tránh lazy loading issues. Chỉ dùng @ManyToOne khi thực sự cần join trong query.

**✅ Done khi:** App compile, không lỗi JPA mapping

---

### ⏰ 13:00–14:30 | T06: Repository Layer (1h)

**`FamilyGroupRepository`:**
```java
public interface FamilyGroupRepository extends JpaRepository<FamilyGroup, Long> {
    // Groups mà user là member (bất kỳ role nào)
    @Query("SELECT g FROM FamilyGroup g WHERE g.id IN " +
           "(SELECT gm.groupId FROM GroupMember gm WHERE gm.userId = :userId)")
    List<FamilyGroup> findGroupsByUserId(@Param("userId") Long userId);
}
```

**`GroupMemberRepository`:**
```java
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);
    List<GroupMember> findByGroupId(Long groupId);
    boolean existsByGroupIdAndUserId(Long groupId, Long userId);
    long countByGroupId(Long groupId);
}
```

**`GroupInvitationRepository`:**
```java
public interface GroupInvitationRepository extends JpaRepository<GroupInvitation, Long> {
    Optional<GroupInvitation> findByToken(String token);
    Optional<GroupInvitation> findByGroupIdAndEmailAndStatus(
        Long groupId, String email, InvitationStatus status);
    List<GroupInvitation> findByGroupIdAndStatus(Long groupId, InvitationStatus status);
}
```

---

### ⏰ 14:30–16:00 | T07: DTO Layer (1.5h)

> 💡 **AI Assist:** Generate DTO boilerplate nhanh

**Request DTOs:**
- `CreateGroupRequest`: name (NotBlank, max 100), description (optional)
- `InviteMemberRequest`: email (NotBlank, @Email)
- `UpdateMemberRoleRequest`: role (NotNull — GroupRole enum)

**Response DTOs:**
- `GroupResponse`: id, name, description, createdBy, memberCount, myRole, createdAt
- `GroupMemberResponse`: userId, fullName, email, role, joinedAt
- `InvitationResponse`: id, groupId, groupName, email, status, expiresAt, inviteLink

**Commit Day 1:**
```bash
git commit -m "feat: migrations V6-V8 (family_groups, group_members, invitations), entities, repositories, DTOs"
```

---

## 📅 DAY 2 — Group CRUD + Authorization Service
**6 tiếng | Thứ Ba**

### 🌅 Daily Standup
```
✅ Hôm qua: 3 migration, 3 entity, repository, DTO xong
🎯 Hôm nay: Group CRUD API + GroupAuthorizationService (quan trọng nhất Sprint 4)
🚧 Blocker: App compile không lỗi không? Start app check trước
```

---

### ⏰ 09:00–11:00 | T08: Group CRUD API (2h)

**Endpoints:**

**`POST /api/groups`** — Tạo group mới:

Logic:
1. Tạo `FamilyGroup` với `createdBy = currentUserId`
2. Tự động thêm creator vào `group_members` với role = **ADMIN**
3. Trả `GroupResponse`

```java
@Transactional
public GroupResponse createGroup(CreateGroupRequest request, Long currentUserId) {
    FamilyGroup group = FamilyGroup.builder()
        .name(request.getName())
        .description(request.getDescription())
        .createdBy(currentUserId)
        .build();
    group = familyGroupRepository.save(group);

    // Auto-add creator as ADMIN
    GroupMember adminMember = GroupMember.builder()
        .groupId(group.getId())
        .userId(currentUserId)
        .role(GroupRole.ADMIN)
        .build();
    groupMemberRepository.save(adminMember);

    return mapToResponse(group, GroupRole.ADMIN, 1);
}
```

**`GET /api/groups`** — Danh sách groups của user hiện tại:
- Chỉ trả groups mà user là member
- Kèm theo `myRole` (ADMIN hay MEMBER) + `memberCount`

**`GET /api/groups/{id}`** — Chi tiết 1 group:
- Kiểm tra user phải là member của group → 403 nếu không phải
- Trả thông tin group + memberCount + myRole

**✅ Done khi:**
- Tạo group → creator tự động là ADMIN
- GET /api/groups → chỉ thấy group mình tham gia
- User không trong group gọi GET /api/groups/{id} → 403

---

### ⏰ 11:00–13:00 | T09: GroupAuthorizationService (2h)

> ⚠️ **Đây là component quan trọng nhất Sprint 4.** Được tái sử dụng ở mọi Group API.

```java
@Service
@RequiredArgsConstructor
public class GroupAuthorizationService {

    private final GroupMemberRepository groupMemberRepository;

    /** Kiểm tra user có phải MEMBER (bất kỳ role) của group không */
    public GroupMember requireMember(Long groupId, Long userId) {
        return groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
            .orElseThrow(() -> new ForbiddenException(
                "Bạn không phải thành viên của nhóm này"));
    }

    /** Kiểm tra user phải là ADMIN */
    public GroupMember requireAdmin(Long groupId, Long userId) {
        GroupMember member = requireMember(groupId, userId);
        if (member.getRole() != GroupRole.ADMIN) {
            throw new ForbiddenException("Chỉ ADMIN mới có quyền thực hiện thao tác này");
        }
        return member;
    }

    /** Kiểm tra có phải member nhưng không throw — dùng cho boolean check */
    public boolean isMember(Long groupId, Long userId) {
        return groupMemberRepository.existsByGroupIdAndUserId(groupId, userId);
    }

    /** Kiểm tra có phải ADMIN không — dùng cho boolean check */
    public boolean isAdmin(Long groupId, Long userId) {
        return groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
            .map(m -> m.getRole() == GroupRole.ADMIN)
            .orElse(false);
    }
}
```

**Tạo `ForbiddenException`:**
```java
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) { super(message); }
}
```

**Thêm handler vào `GlobalExceptionHandler`:**
```java
@ExceptionHandler(ForbiddenException.class)
public ResponseEntity<ApiResponse<Void>> handleForbidden(ForbiddenException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ApiResponse.error(ex.getMessage()));
}
```

**Cách dùng trong mọi Group endpoint:**
```java
// Ví dụ: Endpoint chỉ ADMIN mới gọi được
public void someAdminAction(Long groupId, Long currentUserId) {
    groupAuthService.requireAdmin(groupId, currentUserId); // throw 403 nếu không phải ADMIN
    // ... business logic
}

// Ví dụ: Endpoint mọi member đều gọi được
public void someMemberAction(Long groupId, Long currentUserId) {
    groupAuthService.requireMember(groupId, currentUserId); // throw 403 nếu không phải member
    // ... business logic
}
```

---

### ⏰ 14:00–15:30 | T10: Unit Test GroupService + T11: Email Service (1.5h + 1h)

**Unit tests GroupService:**
- `createGroup_success()` — group tạo được, creator là ADMIN
- `createGroup_creatorAutoAddedAsAdmin()` — verify `groupMemberRepository.save()` được gọi với role ADMIN
- `getGroups_onlyReturnUserGroups()` — không thấy group mình không tham gia
- `getGroupDetail_notMember()` → `ForbiddenException`

**Unit tests GroupAuthorizationService:**
- `requireAdmin_success()` — user là ADMIN → không throw
- `requireAdmin_notAdmin()` → `ForbiddenException`
- `requireMember_notMember()` → `ForbiddenException`

**Email/Console Service (T11):**

> **Quyết định Sprint 4:** Dùng **console log** thay vì SMTP thật. Setup email phức tạp → để Sprint 8.

```java
@Service
public class NotificationService {

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    public void sendGroupInvitation(String email, String token, String groupName) {
        String link = frontendUrl + "/invitations/accept?token=" + token;

        // Sprint 4: log ra console
        log.info("=== INVITATION LINK ===");
        log.info("To: {}", email);
        log.info("Group: {}", groupName);
        log.info("Link: {}", link);
        log.info("======================");

        // Sprint 8: thay bằng JavaMailSender thật
    }
}
```

**Commit Day 2:**
```bash
git commit -m "feat: Group CRUD API, GroupAuthorizationService, unit tests, NotificationService (console)"
```

---

## 📅 DAY 3 — Invitation Flow + Member List
**6 tiếng | Thứ Tư**

### 🌅 Daily Standup
```
✅ Hôm qua: Group CRUD + Auth Service xong
🎯 Hôm nay: Invite flow (send + accept) + xem danh sách member
🚧 Blocker: GroupAuthorizationService test pass chưa?
```

---

### ⏰ 09:00–11:00 | T12: POST /groups/{id}/invite (2h)

**Endpoint:** `POST /api/groups/{groupId}/invite`

**Body:** `{ "email": "member@gmail.com" }`

**Logic đầy đủ:**
```java
@Transactional
public InvitationResponse inviteMember(Long groupId, String email, Long currentUserId) {

    // 1. Chỉ ADMIN mới invite được
    groupAuthService.requireAdmin(groupId, currentUserId);

    // 2. Lấy group info
    FamilyGroup group = familyGroupRepository.findById(groupId)
        .orElseThrow(() -> new ResourceNotFoundException("Group không tồn tại"));

    // 3. Tìm user được mời (có thể chưa đăng ký)
    Optional<User> invitedUser = userRepository.findByEmail(email);

    // 4. Nếu đã là member → báo lỗi
    if (invitedUser.isPresent() &&
        groupMemberRepository.existsByGroupIdAndUserId(groupId, invitedUser.get().getId())) {
        throw new BusinessException(email + " đã là thành viên của nhóm này");
    }

    // 5. Nếu đã có invitation PENDING → huỷ cái cũ, tạo cái mới
    groupInvitationRepository
        .findByGroupIdAndEmailAndStatus(groupId, email, InvitationStatus.PENDING)
        .ifPresent(old -> {
            old.setStatus(InvitationStatus.CANCELLED);
            groupInvitationRepository.save(old);
        });

    // 6. Tạo invitation mới
    String token = UUID.randomUUID().toString().replace("-", "");
    GroupInvitation invitation = GroupInvitation.builder()
        .groupId(groupId)
        .email(email)
        .token(token)
        .status(InvitationStatus.PENDING)
        .expiresAt(LocalDateTime.now().plusDays(7))  // hết hạn sau 7 ngày
        .createdBy(currentUserId)
        .build();
    groupInvitationRepository.save(invitation);

    // 7. Gửi notification (console log)
    notificationService.sendGroupInvitation(email, token, group.getName());

    return mapToInvitationResponse(invitation, group.getName());
}
```

**Response trả về `inviteLink`** để FE hiển thị cho user copy chia sẻ:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "email": "member@gmail.com",
    "status": "PENDING",
    "expiresAt": "2026-05-21T09:00:00",
    "inviteLink": "http://localhost:5173/invitations/accept?token=abc123..."
  }
}
```

**✅ Done khi:**
- ADMIN invite → tạo invitation, log link ra console
- MEMBER invite → 403 Forbidden
- Invite email đã là member → lỗi rõ ràng
- Invite lần 2 cùng email → cái cũ bị CANCELLED, cái mới tạo ra

---

### ⏰ 11:00–13:00 | T13: POST /invitations/accept?token= (2h)

**Endpoint:** `POST /api/invitations/accept?token=abc123`

> **Note:** Endpoint này **không cần auth** nếu muốn cho phép người chưa đăng ký nhận link. Nhưng trong scope này → **yêu cầu đăng nhập trước**, user phải có account.

**Logic:**
```java
@Transactional
public GroupResponse acceptInvitation(String token, Long currentUserId) {

    // 1. Tìm invitation theo token
    GroupInvitation invitation = groupInvitationRepository.findByToken(token)
        .orElseThrow(() -> new ResourceNotFoundException("Link mời không hợp lệ"));

    // 2. Kiểm tra status
    if (invitation.getStatus() != InvitationStatus.PENDING) {
        throw new BusinessException("Link mời đã được sử dụng hoặc đã bị huỷ");
    }

    // 3. Kiểm tra hết hạn
    if (LocalDateTime.now().isAfter(invitation.getExpiresAt())) {
        invitation.setStatus(InvitationStatus.EXPIRED);
        groupInvitationRepository.save(invitation);
        throw new BusinessException("Link mời đã hết hạn (7 ngày)");
    }

    // 4. Kiểm tra email khớp với user đang đăng nhập
    User currentUser = userRepository.findById(currentUserId)
        .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));
    if (!currentUser.getEmail().equals(invitation.getEmail())) {
        throw new ForbiddenException(
            "Link mời này dành cho " + invitation.getEmail() + ", không phải tài khoản của bạn");
    }

    // 5. Kiểm tra đã là member chưa
    if (groupMemberRepository.existsByGroupIdAndUserId(invitation.getGroupId(), currentUserId)) {
        throw new BusinessException("Bạn đã là thành viên của nhóm này");
    }

    // 6. Thêm vào group với role MEMBER
    GroupMember newMember = GroupMember.builder()
        .groupId(invitation.getGroupId())
        .userId(currentUserId)
        .role(GroupRole.MEMBER)
        .build();
    groupMemberRepository.save(newMember);

    // 7. Update invitation status → ACCEPTED
    invitation.setStatus(InvitationStatus.ACCEPTED);
    groupInvitationRepository.save(invitation);

    // 8. Trả về GroupResponse để FE redirect vào group
    FamilyGroup group = familyGroupRepository.findById(invitation.getGroupId())
        .orElseThrow();
    long memberCount = groupMemberRepository.countByGroupId(group.getId());
    return mapToGroupResponse(group, GroupRole.MEMBER, memberCount);
}
```

**✅ Done khi:**
- Accept đúng token + đúng email → join group thành công
- Token không tồn tại → 404
- Token đã dùng → lỗi rõ ràng
- Token hết hạn → lỗi rõ ràng
- Email không khớp → 403

---

### ⏰ 14:00–15:00 | T14: GET /groups/{id}/members (1h)

**Endpoint:** `GET /api/groups/{groupId}/members`

- Yêu cầu: phải là member của group (dùng `groupAuthService.requireMember()`)
- Trả danh sách `GroupMemberResponse`: userId, fullName, email, role, joinedAt

```java
public List<GroupMemberResponse> getMembers(Long groupId, Long currentUserId) {
    groupAuthService.requireMember(groupId, currentUserId);

    List<GroupMember> members = groupMemberRepository.findByGroupId(groupId);

    // Lấy thông tin user cho từng member
    return members.stream().map(m -> {
        User user = userRepository.findById(m.getUserId()).orElseThrow();
        return GroupMemberResponse.builder()
            .userId(user.getId())
            .fullName(user.getFullName())
            .email(user.getEmail())
            .role(m.getRole())
            .joinedAt(m.getJoinedAt())
            .build();
    }).collect(Collectors.toList());
}
```

> 💡 Nếu group có nhiều member, query N+1 ở trên sẽ chậm. Với scope demo (~5 members/group) thì OK. Optimize bằng JOIN query nếu cần sau.

**Commit Day 3:**
```bash
git commit -m "feat: invitation flow (invite + accept with validation), GET members endpoint"
```

**📊 Check end of Day 3:**
```
T01–T03 ✅ 3 Migrations
T04–T07 ✅ Entities + Repository + DTO
T08     ✅ Group CRUD API
T09     ✅ GroupAuthorizationService
T10     ✅ Unit tests Group
T11     ✅ NotificationService (console)
T12     ✅ Invite endpoint (đầy đủ validation)
T13     ✅ Accept invitation (đầy đủ validation)
T14     ✅ GET members
```

---

*→ Tiếp theo: Day 4–6 (Member management + FE Integration) trong file sprint4_daily_p2.md*
