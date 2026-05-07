# 🏦 Personal + Family Finance Insight AI — Sprint Plan v2

> **Solo Developer** | **Spring Boot + MySQL + OpenAI** | **~8 tuần** | **~6h/ngày, 5–6 ngày/tuần**

---

## ⚠️ Lưu ý về Scope

Family Finance **không nhỏ** như bạn nghĩ. Nó thêm vào:
- Role-based access control trong group (ADMIN / MEMBER)
- Data isolation phức tạp (private vs shared transaction)
- Multi-tenant logic ở gần như mọi API
- Invitation flow qua email

> Kế hoạch này mở rộng từ **7 lên 8 Sprint (~8 tuần)**. Nếu muốn giữ 7 tuần, hãy cắt Sprint 7 AI Family xuống còn MVP tối giản.

---

## 📐 Timeline Tổng Quan

| Sprint | Tuần | Chủ đề | Milestone |
|--------|------|--------|-----------|
| Sprint 1 | Tuần 1 | Foundation & Auth | App chạy, login được |
| Sprint 2 | Tuần 2 | Personal Transaction System | CRUD giao dịch cá nhân |
| Sprint 3 | Tuần 3 | Personal Dashboard & Budget | ✅ **Personal MVP** |
| Sprint 4 | Tuần 4 | Family Group Management | Tạo nhóm, mời thành viên |
| Sprint 5 | Tuần 5 | Shared Accounts, Transactions & Budget | ✅ **Family MVP** |
| Sprint 6 | Tuần 6 | AI: Auto-Categorization (Personal + Family) | AI classify giao dịch |
| Sprint 7 | Tuần 7 | AI: Insight, Anomaly & Health Score | AI phân tích cả nhà |
| Sprint 8 | Tuần 8 | Polish, Security & Demo Prep | ✅ **Production Ready** |

---

## 🗃️ Database Schema Tổng Thể (Thiết kế trước khi code)

> ⚠️ **Phải tự thiết kế — không nhờ AI**. Đây là quyết định kiến trúc quan trọng nhất.

```
users               → id, email, password_hash, full_name
family_groups       → id, name, created_by (FK users)
group_members       → id, group_id, user_id, role (ADMIN/MEMBER), joined_at
group_invitations   → id, group_id, email, token, status, expires_at

accounts            → id, user_id, group_id (nullable), name, type, balance, scope (PERSONAL/SHARED)
categories          → id, user_id (nullable=system default), name, type, icon
transactions        → id, user_id, account_id, category_id, group_id (nullable),
                       amount, type, description, transaction_date,
                       scope (PERSONAL/SHARED), is_auto_categorized, is_anomaly, deleted_at
budgets             → id, user_id (nullable), group_id (nullable), category_id, amount, month

ai_categorization_cache → description_hash, category_id, created_at
ai_call_logs            → id, type, prompt_summary, response_summary, latency_ms, success, created_at
insights                → id, user_id (nullable), group_id (nullable), month, content, created_at
```

**Quy tắc phân quyền data:**
- `group_id = NULL` → personal, chỉ owner thấy
- `group_id IS NOT NULL` → shared, mọi member trong group thấy
- `scope = PERSONAL` → luôn private dù nằm trong group context

---

## ✅ SPRINT 1 — Foundation & Auth
**Tuần 1 | 36h**

### 🎯 Sprint Goal
App khởi động, kết nối DB, đăng ký/đăng nhập trả JWT. Không crash.

### 📖 User Stories
| ID | Story | Priority |
|----|-------|----------|
| US-01 | Đăng ký tài khoản bằng email + password | Must |
| US-02 | Đăng nhập nhận JWT token | Must |
| US-03 | Swagger UI để test API | Should |

### 🔧 Technical Tasks
- [ ] Khởi tạo Spring Boot 3.x + dependencies (Web, JPA, Security, MySQL, Lombok, Flyway, Validation)
- [ ] Cấu trúc package: `controller / service / repository / entity / dto / config / exception / security / util`
- [ ] `application.yml` với dev/prod profile, đọc secret từ `.env`
- [ ] Flyway Migration V1: bảng `users`
- [ ] `User` entity + `UserRepository`
- [ ] `UserDetailsServiceImpl`
- [ ] `JwtUtil` (generate + validate token với jjwt 0.12.x)
- [ ] `JwtAuthenticationFilter`
- [ ] `SecurityFilterChain` config (stateless, permit `/api/auth/**`)
- [ ] `POST /api/auth/register` + `POST /api/auth/login`
- [ ] `GlobalExceptionHandler` với `@ControllerAdvice`
- [ ] `ApiResponse<T>` generic wrapper
- [ ] Swagger/OpenAPI config với JWT Bearer support
- [ ] `.gitignore`, `.env.example`, `README.md`
- [ ] Unit test: `AuthServiceTest` (register thành công, email trùng, login sai pass)

### 💡 AI Coding Assistant
✅ Dùng cho: boilerplate DTOs, Flyway SQL, Swagger annotations  
❌ Tự làm: Security config (hiểu rõ filter chain), JWT logic

### 📦 Deliverables
- App start được, kết nối MySQL
- Register + Login API trả JWT
- Swagger UI tại `/swagger-ui.html`
- Tests GREEN

---

## ✅ SPRINT 2 — Personal Transaction System
**Tuần 2 | 36h**

### 🎯 Sprint Goal
User CRUD được giao dịch cá nhân, gắn danh mục, xem lịch sử. Luồng tài chính cá nhân hoạt động.

### 📖 User Stories
| ID | Story | Priority |
|----|-------|----------|
| US-04 | Thêm giao dịch: số tiền, mô tả, ngày, loại thu/chi | Must |
| US-05 | Gắn danh mục cho giao dịch | Must |
| US-06 | Xem + filter giao dịch theo tháng/danh mục | Must |
| US-07 | Sửa/xóa giao dịch | Should |
| US-08 | Quản lý nhiều tài khoản cá nhân | Should |

### 🔧 Technical Tasks
- [ ] Migration V2: `accounts` (user_id, group_id nullable, name, type, balance, scope)
- [ ] Migration V3: `categories` + seed 15 danh mục mặc định
- [ ] Migration V4: `transactions` (đầy đủ các trường kể cả `group_id`, `scope`, `is_auto_categorized`, `is_anomaly`, `deleted_at`)
- [ ] CRUD API `accounts` — chỉ trả account personal (group_id IS NULL)
- [ ] CRUD API `categories`
- [ ] `POST /api/transactions` — tạo giao dịch (default scope=PERSONAL)
- [ ] `GET /api/transactions?month=&categoryId=&type=` — filter, user chỉ thấy data mình
- [ ] `PUT /api/transactions/{id}` + soft delete
- [ ] Validation: amount > 0, date <= today, user chỉ sửa transaction của mình
- [ ] Unit test: `TransactionServiceTest`

### 💡 AI Coding Assistant
✅ Dùng cho: DTO generation, seed data SQL, test stubs  
❌ Tự làm: business rule validation, data isolation logic

### 📦 Deliverables
- Full CRUD Transaction cá nhân
- Category system với seed data
- Multi-account cá nhân
- Data isolation: user A không thấy data user B

---

## ✅ SPRINT 3 — Personal Dashboard & Budget
**Tuần 3 | 36h**

### 🎯 Sprint Goal
Xem được tổng quan tài chính cá nhân, đặt budget, có UI cơ bản. **Personal MVP hoàn chỉnh.**

### 📖 User Stories
| ID | Story | Priority |
|----|-------|----------|
| US-09 | Xem tổng thu, tổng chi, số dư tháng hiện tại | Must |
| US-10 | Biểu đồ chi tiêu theo danh mục | Must |
| US-11 | Đặt budget cho từng danh mục, xem % đã dùng | Should |
| US-12 | UI cơ bản để nhập và xem giao dịch | Should |

### 🔧 Technical Tasks
- [ ] Migration V5: `budgets` (user_id nullable, group_id nullable, category_id, amount, month)
- [ ] `GET /api/summary?month=` — tổng thu/chi/số dư + breakdown category
- [ ] `GET /api/summary/trend?months=6` — xu hướng 6 tháng
- [ ] CRUD `budgets` (cá nhân: group_id = NULL)
- [ ] `GET /api/budgets/status?month=` — thực tế vs budget
- [ ] Frontend đơn giản (React hoặc Thymeleaf):
  - Trang Login/Register
  - Dashboard: tổng thu/chi + donut chart (Chart.js)
  - Transaction list với filter
  - Form thêm giao dịch nhanh
- [ ] Unit test: `SummaryServiceTest`

### 💡 AI Coding Assistant
✅ Dùng cho: SQL GROUP BY phức tạp, React boilerplate, Chart.js config  
❌ Tự làm: logic tính toán summary, budget comparison

### 📦 Deliverables
- ✅ **Personal MVP demo được**: CRUD + Dashboard + Budget
- Summary API + trend
- Frontend hiển thị dashboard
- Push lên GitHub, người khác có thể chạy được

---

## ✅ SPRINT 4 — Family Group Management
**Tuần 4 | 36h**

### 🎯 Sprint Goal
ADMIN tạo được nhóm gia đình, mời thành viên qua email, quản lý role. Hệ thống phân quyền trong group hoạt động.

### 📖 User Stories
| ID | Story | Priority |
|----|-------|----------|
| US-13 | ADMIN tạo group gia đình, đặt tên | Must |
| US-14 | Mời thành viên qua email (gửi link invitation) | Must |
| US-15 | Thành viên accept invitation, join group | Must |
| US-16 | ADMIN kick member, đổi role ADMIN/MEMBER | Should |
| US-17 | Xem danh sách thành viên trong group | Must |

### 🔧 Technical Tasks
- [ ] Migration V6: `family_groups` (id, name, created_by)
- [ ] Migration V7: `group_members` (group_id, user_id, role ENUM, joined_at)
- [ ] Migration V8: `group_invitations` (group_id, email, token UUID, status, expires_at 7 ngày)
- [ ] CRUD `family_groups` — user có thể tạo/xem group mình thuộc về
- [ ] `POST /api/groups/{id}/invite` — ADMIN gửi invitation (tạo token, gửi email hoặc trả link)
- [ ] `POST /api/invitations/accept?token=` — user accept, thêm vào group_members
- [ ] `DELETE /api/groups/{id}/members/{userId}` — ADMIN kick member
- [ ] `PATCH /api/groups/{id}/members/{userId}/role` — ADMIN đổi role
- [ ] `GroupAuthorizationService` — helper check quyền ADMIN trong group
- [ ] Annotation hoặc helper `@RequireGroupAdmin` / manual check
- [ ] Email service đơn giản (JavaMailSender hoặc chỉ log link ra console nếu không có SMTP)
- [ ] Unit test: `GroupServiceTest` (tạo group, invite, accept, kick)

### 💡 AI Coding Assistant
✅ Dùng cho: UUID token generation, email template, migration SQL  
❌ Tự làm: authorization logic (ai được làm gì trong group), invitation flow design

### 📦 Deliverables
- Tạo group, invite qua email (hoặc console link), accept join
- ADMIN quản lý được member và role
- Authorization: MEMBER không thể kick người khác
- Data isolation: chỉ member trong group mới gọi được group APIs

---

## ✅ SPRINT 5 — Shared Accounts, Transactions & Budget
**Tuần 5 | 36h**

### 🎯 Sprint Goal
Thành viên gia đình cùng nhập giao dịch vào quỹ chung, xem được ai chi gì, đặt budget chung. **Family MVP hoàn chỉnh.**

### 📖 User Stories
| ID | Story | Priority |
|----|-------|----------|
| US-18 | Tạo tài khoản chung (quỹ gia đình), mọi member thấy số dư | Must |
| US-19 | Member nhập giao dịch vào quỹ chung | Must |
| US-20 | Mọi member xem được giao dịch chung: ai chi gì, bao nhiêu | Must |
| US-21 | Giao dịch cá nhân vẫn private, không lộ ra group | Must |
| US-22 | ADMIN đặt budget chung theo danh mục, member xem % đã dùng | Should |
| US-23 | Dashboard gia đình: tổng thu/chi cả nhà, ai đóng góp/chi nhiều nhất | Must |

### 🔧 Technical Tasks

**Shared Accounts:**
- [ ] `POST /api/groups/{groupId}/accounts` — tạo tài khoản chung (group_id != NULL)
- [ ] `GET /api/groups/{groupId}/accounts` — xem tài khoản chung của group

**Shared Transactions:**
- [ ] `POST /api/groups/{groupId}/transactions` — tạo giao dịch chung (scope=SHARED, group_id set)
- [ ] `GET /api/groups/{groupId}/transactions?month=` — xem giao dịch chung, kèm tên người tạo
- [ ] Logic: chỉ member trong group mới gọi được; private transaction không lộ ra

**Shared Budget:**
- [ ] `POST /api/groups/{groupId}/budgets` — ADMIN đặt budget chung
- [ ] `GET /api/groups/{groupId}/budgets/status?month=` — thực tế vs budget chung

**Family Dashboard:**
- [ ] `GET /api/groups/{groupId}/summary?month=` — tổng thu/chi cả nhà
- [ ] `GET /api/groups/{groupId}/summary/by-member?month=` — breakdown theo từng thành viên
- [ ] `GET /api/groups/{groupId}/summary/trend?months=6` — xu hướng 6 tháng
- [ ] Frontend: thêm tab/page "Gia đình" trong UI hiện có

**Security:**
- [ ] Kiểm tra mọi group API: user phải là member của group đó
- [ ] Unit test: shared transaction chỉ visible với member, không visible với outsider

### 💡 AI Coding Assistant
✅ Dùng cho: SQL queries GROUP BY member, dashboard aggregate queries  
❌ Tự làm: data isolation logic (private vs shared), authorization checks

### 📦 Deliverables
- ✅ **Family MVP**: tài khoản chung, giao dịch chung, budget chung
- Family dashboard: tổng quan + breakdown theo thành viên
- Privacy đảm bảo: giao dịch cá nhân không lộ ra group
- Member không thể xem data group khác

---

## ✅ SPRINT 6 — AI: Auto-Categorization
**Tuần 6 | 36h**

### 🎯 Sprint Goal
Nhập mô tả giao dịch → AI tự gợi ý danh mục. Áp dụng cho cả giao dịch cá nhân và giao dịch nhóm.

### 📖 User Stories
| ID | Story | Priority |
|----|-------|----------|
| US-24 | Nhập "Grab đi làm" → AI gợi ý "Đi lại" | Must |
| US-25 | Ghi đè gợi ý AI nếu sai, hệ thống nhớ | Should |
| US-26 | Cơ chế cache giảm OpenAI API calls | Must |
| US-27 | Log tất cả AI calls để kiểm soát cost | Must |

### 🔧 Technical Tasks
- [ ] Migration V9: `ai_categorization_cache` (description_hash, category_id, created_at)
- [ ] Migration V10: `ai_call_logs` (type, prompt_summary, response_summary, latency_ms, success)
- [ ] `OpenAIClient` service wrapper (dùng OkHttpClient hoặc RestClient gọi OpenAI REST API)
- [ ] `POST /api/transactions/categorize` — nhận description, trả category suggestion
- [ ] Logic: hash description → check cache → nếu miss → gọi OpenAI → lưu cache
- [ ] Tích hợp vào `POST /api/transactions` và `POST /api/groups/{id}/transactions`
- [ ] Fallback an toàn khi OpenAI timeout/lỗi (giữ category = null, không crash)
- [ ] Retry với exponential backoff
- [ ] Rate limiting riêng cho AI endpoints
- [ ] Unit test: mock OpenAIClient, test cache hit/miss, test fallback

### ⚠️ Phải tự thiết kế
- Prompt engineering: test nhiều prompt, timebox 4h
- Cache strategy: exact hash match
- Fallback behavior khi AI trả category không tồn tại

### 💡 AI Coding Assistant
✅ Dùng cho: OkHttpClient boilerplate, cache logic, retry mechanism

### 📦 Deliverables
- Auto-categorization hoạt động cho cả personal và group transaction
- Cache giảm duplicate API calls
- Không crash khi OpenAI lỗi
- AI call log đầy đủ

---

## ✅ SPRINT 7 — AI: Insight, Anomaly & Health Score
**Tuần 7 | 36h**

### 🎯 Sprint Goal
AI phân tích chi tiêu cá nhân + gia đình, phát hiện bất thường, tính điểm sức khỏe tài chính.

### 📖 User Stories
| ID | Story | Priority |
|----|-------|----------|
| US-28 | Insight cá nhân: "Tháng này bạn chi Giải trí nhiều hơn 35%" | Must |
| US-29 | Insight gia đình: "Gia đình vượt ngân sách ăn uống 20%" | Must |
| US-30 | Cảnh báo anomaly: giao dịch bất thường (>3x trung bình) | Must |
| US-31 | Financial Health Score (0–100) cá nhân + lý do | Must |
| US-32 | Gợi ý tiết kiệm cho gia đình dựa trên pattern cả nhà | Should |

### 🔧 Technical Tasks

**Analytics Engine (pure Java/SQL, không dùng OpenAI):**
- [ ] `AnalyticsService`: so sánh chi tiêu tháng này vs 3 tháng trước (personal + group)
- [ ] Anomaly detection: flag transaction nếu > mean + 2×stddev của category
- [ ] Weighted moving average cho spending prediction (3 tháng gần nhất)

**Personal AI Features:**
- [ ] `GET /api/insights?month=` — insight text tiếng Việt (cache theo tháng)
- [ ] `GET /api/anomalies?month=` — danh sách giao dịch bất thường cá nhân
- [ ] `GET /api/health-score` — điểm + breakdown

**Family AI Features:**
- [ ] `GET /api/groups/{id}/insights?month=` — insight cho cả nhà
- [ ] `GET /api/groups/{id}/anomalies?month=` — giao dịch bất thường của nhóm
- [ ] `GET /api/groups/{id}/health-score` — điểm tài chính gia đình

**Scoring Model (tự thiết kế):**
```
Personal Score (100đ):
  - Budget adherence   30đ  (chi < 80% budget)
  - Saving rate        25đ  ((thu-chi)/thu > 20%)
  - Spending stability 20đ  (stddev thấp)
  - Anomaly penalty    15đ  (trừ nếu nhiều anomaly)
  - Consistency        10đ  (ghi chép đều)

Family Score: tính tương tự trên shared transactions
```

- [ ] `@Scheduled` job: cuối tháng tự generate + cache insight
- [ ] Unit test: `AnalyticsServiceTest`, `ScoringServiceTest`

### ⚠️ Phải tự thiết kế
- Scoring formula và trọng số
- Ngưỡng "bất thường" (2×stddev là gợi ý, có thể điều chỉnh)
- Prompt template gửi cho OpenAI để nhận insight tiếng Việt

### 📦 Deliverables
- Insight API cá nhân + gia đình (tiếng Việt)
- Anomaly detection cho cả hai scope
- Financial Health Score với breakdown
- Scheduled job tự động cuối tháng

---

## ✅ SPRINT 8 — Polish, Security & Demo Prep
**Tuần 8 | 36h**

### 🎯 Sprint Goal
App sẵn sàng demo, bảo mật, có seed data thực tế, documentation đầy đủ. Đưa được vào CV.

### 📖 User Stories
| ID | Story | Priority |
|----|-------|----------|
| US-33 | App chạy ổn định, không lỗi 500 khi dùng bình thường | Must |
| US-34 | API bảo vệ đúng: user A không xem data user B | Must |
| US-35 | Seed data realistic để demo (3 tháng, 2 user, 1 family group) | Must |
| US-36 | API docs rõ ràng | Should |

### 🔧 Technical Tasks

**Security Audit:**
- [ ] Kiểm tra toàn bộ: user chỉ truy cập data của mình
- [ ] Kiểm tra group API: outsider không vào được
- [ ] Rate limiting cho tất cả API (Bucket4j)
- [ ] Rate limiting riêng cho AI endpoints
- [ ] Input sanitization + validation review

**Testing:**
- [ ] Integration test các happy path quan trọng
- [ ] Test: mời thành viên → accept → tạo giao dịch chung → xem dashboard
- [ ] Postman collection đầy đủ (export vào repo)

**Demo Prep:**
- [ ] SQL seed data: 2 user, 1 family group, 3 tháng giao dịch mix personal + shared
- [ ] `DEMO.md`: kịch bản demo step-by-step (10 phút)
- [ ] Update Swagger với description đầy đủ

**Documentation:**
- [ ] `README.md` với architecture diagram + API overview
- [ ] Ghi lại prompt templates đã dùng
- [ ] `BACKLOG.md`: những gì chưa làm

**Deployment (Optional):**
- [ ] `Dockerfile` + `docker-compose.yml` (app + MySQL)
- [ ] Deploy lên Railway/Render free tier

### 💡 AI Coding Assistant
✅ Dùng cho: seed data SQL, Dockerfile, README template

### 📦 Deliverables
- ✅ **Production-ready demo**
- Security hardened, no data leaks
- Postman collection + Swagger docs
- Demo script + seed data
- (Optional) Live URL

---

## ⚠️ Rủi Ro & Lỗi Thường Gặp

### 🔴 HIGH Risk

| Rủi ro | Giải pháp |
|--------|-----------|
| Authorization logic sai → data leak giữa users/groups | Viết test isolation ngay khi implement, không để đến Sprint 8 |
| OpenAI cost vượt budget | Set limit $10/month, dùng `gpt-4o-mini`, cache aggressively |
| Sprint 4–5 (Family) mất nhiều hơn 2 tuần | Timebox cứng. Nếu trễ: cắt email invitation → dùng console link |
| Scope creep từ Family features | Mọi idea mới → `BACKLOG.md`, không làm ngay |

### 🟡 MEDIUM Risk

| Rủi ro | Giải pháp |
|--------|-----------|
| Multi-tenant query phức tạp gây bug | Luôn thêm `userId` hoặc `groupId` vào mọi query, không bao giờ query all |
| Frontend tốn thời gian | Frontend chỉ cần đủ demo, không cần đẹp. Ưu tiên backend |
| Email service (SMTP setup) | Sprint 4: dùng console log link trước, cài SMTP ở Sprint 8 nếu còn thời gian |

### 🟢 Lỗi Code Thường Gặp

```
❌ Quên filter group_id khi query → user thấy data group khác
✅ Mọi group query phải có: AND gm.user_id = :currentUserId

❌ ADMIN check bằng hardcode role string
✅ Dùng enum GroupRole.ADMIN + GroupAuthorizationService tái sử dụng

❌ Commit OpenAI API key
✅ .env + .gitignore + GitHub secret scanning bật lên ngay

❌ Call OpenAI trong Controller hoặc @Scheduled mỗi giây
✅ Service layer + cache + chỉ gọi khi cần thiết
```

---

## 🎯 Chiến Lược Kiểm Soát Scope — Solo Dev

### Rule #1: Personal trước, Family sau
Không code Family feature khi Personal feature chưa xong và tested. Sprint 1–3 phải stable trước.

### Rule #2: Timebox cứng
```
Task thông thường  → max 4h, nếu quá → break nhỏ hơn
AI prompt tuning   → max 4h experiment → accept "good enough"
Bug fix            → max 2h → workaround trước, fix sau
Email/SMTP setup   → max 2h → nếu quá → log link ra console
```

### Rule #3: Definition of Done
Task "Done" khi: Code chạy ✅ | Không break feature khác ✅ | Git commit ✅ | Test qua Postman ✅

### Rule #4: Cắt gọn nếu cần
Nếu trễ timeline, cắt theo thứ tự:
1. ~~Email invitation~~ → console log link
2. ~~AI Family Prediction~~ → chỉ giữ Personal prediction
3. ~~Frontend đẹp~~ → Swagger/Postman để demo

---

## 🤖 AI Coding Assistant Guide

| ✅ Nên dùng | ❌ Phải tự làm |
|------------|---------------|
| Boilerplate DTOs, Controllers | Database schema design |
| SQL GROUP BY phức tạp | Authorization logic (ai xem được gì) |
| Flyway migration scripts | Prompt engineering cho AI |
| Unit test stubs | Scoring algorithm |
| Dockerfile, docker-compose | Multi-tenant data isolation |
| README, documentation | Architecture decisions |

> **⚡ Golden Rule:** AI viết code nhanh hơn, nhưng bạn phải hiểu 100% code đó làm gì.

---

*Personal + Family Finance Insight AI | Solo Developer | v2.0 | 2026*
