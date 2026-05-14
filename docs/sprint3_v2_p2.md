# 🏃 Sprint 3 (Revised) — Day 4–6: FE Integration
**Kết nối FE đã thiết kế sẵn với BE APIs**

---

## 📅 DAY 4 — CORS + Dashboard + Transaction Integration
**6 tiếng | Thứ Năm**

### 🌅 Daily Standup
```
✅ Hôm qua: Summary, Trend, Budget APIs xong và tested
🎯 Hôm nay: CORS config + kết nối Dashboard + Transaction list với BE thật
🚧 Blocker: FE đang dùng mock data ở đâu? List ra các file cần sửa
```

---

### ⏰ 09:00–10:00 | T12: CORS Config + Axios Setup (1h)

**Bước 1 — Backend CORS config (làm đầu tiên, không có cái này FE không gọi được):**

Trong `SecurityConfig.java`:
```java
.cors(cors -> cors.configurationSource(request -> {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of(
        "http://localhost:5173",   // Vite dev
        "http://localhost:3000"    // CRA dev (nếu dùng)
    ));
    config.setAllowedMethods(List.of("GET","POST","PUT","DELETE","PATCH","OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);
    return config;
}))
```

**Bước 2 — FE: Axios base config:**

Kiểm tra file `axiosConfig.js` (hoặc tương đương) trong FE:
- `baseURL` trỏ đúng `http://localhost:8080/api`
- Interceptor gắn JWT từ localStorage vào mỗi request
- Interceptor response: nếu 401 → clear token + redirect login

```javascript
api.interceptors.request.use(config => {
    const token = localStorage.getItem('token');
    if (token) config.headers.Authorization = `Bearer ${token}`;
    return config;
});

api.interceptors.response.use(
    res => res,
    err => {
        if (err.response?.status === 401) {
            localStorage.removeItem('token');
            window.location.href = '/login';
        }
        return Promise.reject(err);
    }
);
```

**Test CORS ngay:** Mở FE, gọi `GET /api/categories` → nếu không có lỗi CORS là OK.

---

### ⏰ 10:00–12:00 | T13: Kết nối Dashboard (2h)

**FE đang có gì:** Dashboard page với chart layout, đang dùng mock data hoặc hardcode.

**Việc cần làm:** Thay mock data bằng API calls thật.

**API calls cần trong Dashboard:**
```javascript
// Gọi song song để nhanh hơn
const [summaryRes, trendRes] = await Promise.all([
    api.get(`/summary?month=${selectedMonth}`),
    api.get('/summary/trend?months=6')
]);

const summary = summaryRes.data.data;
const trend = trendRes.data.data.trend;
```

**Map data vào Chart.js:**

Donut chart (chi theo category):
```javascript
const expenseCategories = summary.categoryBreakdown
    .filter(c => c.type === 'EXPENSE');

const donutData = {
    labels: expenseCategories.map(c => c.categoryName),
    datasets: [{
        data: expenseCategories.map(c => c.total),
        backgroundColor: CHART_COLORS, // mảng màu cố định trong FE
    }]
};
```

Bar chart (trend 6 tháng):
```javascript
const barData = {
    labels: trend.map(t => t.month),
    datasets: [
        { label: 'Thu', data: trend.map(t => t.income), backgroundColor: '#36A2EB' },
        { label: 'Chi', data: trend.map(t => t.expense), backgroundColor: '#FF6384' }
    ]
};
```

**Cards số liệu:**
```javascript
// Format VND
const formatVND = (n) => new Intl.NumberFormat('vi-VN', {
    style: 'currency', currency: 'VND'
}).format(n ?? 0);

// Hiển thị
<div>{formatVND(summary?.totalIncome)}</div>
<div>{formatVND(summary?.totalExpense)}</div>
<div>{formatVND(summary?.currentBalance)}</div>
```

**Month selector kết nối:**
```javascript
const [selectedMonth, setSelectedMonth] = useState(
    new Date().toISOString().slice(0, 7) // "2026-05"
);
// Khi đổi tháng → re-fetch summary (không cần re-fetch trend)
```

**✅ Done khi:**
- Dashboard hiển thị số thật từ BE
- Đổi tháng → summary cập nhật, chart donut cập nhật
- Trend chart hiển thị 6 tháng đúng

---

### ⏰ 13:00–15:00 | T14 + T15: Kết nối Transaction List + Form (2h + 1h)

**Transaction List:**

```javascript
const fetchTransactions = async () => {
    const params = new URLSearchParams();
    if (filters.month) params.append('month', filters.month);
    if (filters.type)  params.append('type', filters.type);
    if (filters.categoryId) params.append('categoryId', filters.categoryId);
    params.append('page', filters.page);
    params.append('size', 20);

    const res = await api.get(`/transactions?${params}`);
    setTransactions(res.data.data.content);
    setTotalPages(res.data.data.totalPages);
};
```

**Xóa transaction:**
```javascript
const handleDelete = async (id) => {
    if (!window.confirm('Xóa giao dịch này?')) return;
    await api.delete(`/transactions/${id}`);
    fetchTransactions(); // reload list
};
```

**Form thêm giao dịch — Load dropdowns:**
```javascript
// Khi mở form → load categories + accounts
const [categories, setCategories] = useState([]);
const [accounts, setAccounts] = useState([]);

useEffect(() => {
    Promise.all([
        api.get('/categories'),
        api.get('/accounts')
    ]).then(([catRes, accRes]) => {
        setCategories(catRes.data.data);
        setAccounts(accRes.data.data);
    });
}, []);
```

**Submit form:**
```javascript
const handleSubmit = async (e) => {
    e.preventDefault();
    try {
        await api.post('/transactions', formData);
        onSuccess?.(); // đóng modal
        fetchTransactions(); // reload list
    } catch (err) {
        const msg = err.response?.data?.message || 'Có lỗi xảy ra';
        setError(msg);
    }
};
```

**Commit Day 4:**
```bash
git commit -m "feat: CORS config, Dashboard connected to real API, Transaction list + form integrated"
```

---

## 📅 DAY 5 — Budget + Account Integration + Error/Loading States
**6 tiếng | Thứ Sáu**

### 🌅 Daily Standup
```
✅ Hôm qua: Dashboard + Transaction kết nối BE thật
🎯 Hôm nay: Budget page + Account management + error/loading states
🚧 Blocker: Có bug gì phát sinh từ Day 4?
```

---

### ⏰ 09:00–11:00 | T16: Kết nối Budget Page (2h)

**Budget list + status:**
```javascript
const fetchBudgets = async () => {
    const res = await api.get(`/budgets/status?month=${selectedMonth}`);
    setBudgetStatus(res.data.data);
};
```

**Progress bar dùng data thật:**
```javascript
const getStatusColor = (status) => ({
    OK:       '#4BC0C0',
    WARNING:  '#FFCE56',
    EXCEEDED: '#FF6384'
})[status] ?? '#E5E7EB';

// Width của progress bar
const width = Math.min(item.usedPercentage, 100) + '%';
```

**Form tạo budget:**
```javascript
const handleCreateBudget = async (data) => {
    try {
        await api.post('/budgets', {
            categoryId: data.categoryId,
            amount: data.amount,
            month: selectedMonth
        });
        fetchBudgets();
    } catch (err) {
        // 409 Conflict → "Đã có ngân sách cho danh mục này"
        if (err.response?.status === 409) {
            setError(err.response.data.message);
        }
    }
};
```

**✅ Done khi:**
- Budget page hiển thị đúng % thực tế/kế hoạch
- Progress bar đổi màu theo OK/WARNING/EXCEEDED
- Tạo budget trùng category+tháng → hiện thông báo lỗi rõ

---

### ⏰ 11:00–12:00 | T17: Kết nối Account Management (1h)

Nếu FE có trang/modal quản lý tài khoản:
```javascript
// GET accounts
const fetchAccounts = async () => {
    const res = await api.get('/accounts');
    setAccounts(res.data.data);
};

// POST create account
const handleCreate = async (data) => {
    await api.post('/accounts', data);
    fetchAccounts();
};

// PUT update
const handleUpdate = async (id, data) => {
    await api.put(`/accounts/${id}`, data);
    fetchAccounts();
};
```

**Hiển thị current balance từ BE:**

Nếu BE trả `currentBalance` trong AccountResponse → hiển thị trực tiếp.
Nếu chưa có → tạm thời hiển thị `initialBalance` trước, refactor sau.

---

### ⏰ 13:00–15:00 | T18: Error States + Loading States (2h)

> **Đây là phần hay bị bỏ qua nhưng mentor sẽ để ý khi demo.**

**Loading state — Pattern chung cho mọi page:**
```javascript
const [loading, setLoading] = useState(false);
const [error, setError] = useState(null);

const fetchData = async () => {
    setLoading(true);
    setError(null);
    try {
        const res = await api.get('...');
        setData(res.data.data);
    } catch (err) {
        setError('Không thể tải dữ liệu. Vui lòng thử lại.');
    } finally {
        setLoading(false);
    }
};
```

**Loading UI:** Skeleton loader hoặc spinner đơn giản:
```jsx
if (loading) return <div className="spinner">Đang tải...</div>;
if (error)   return <div className="error">{error} <button onClick={fetchData}>Thử lại</button></div>;
```

**Empty state — Khi chưa có data:**
```jsx
{transactions.length === 0 && !loading && (
    <div className="empty-state">
        <p>Chưa có giao dịch nào trong tháng này</p>
        <button onClick={openAddForm}>+ Thêm giao dịch đầu tiên</button>
    </div>
)}
```

**Checklist error states cần xử lý:**
- [ ] API timeout / network error → show "Mất kết nối, thử lại"
- [ ] 401 Unauthorized → auto redirect login (đã có trong interceptor)
- [ ] 400 Bad Request từ form → hiển thị message từ BE
- [ ] 404 Not Found → "Không tìm thấy dữ liệu"
- [ ] 409 Conflict (budget trùng) → message rõ ràng

**Commit Day 5:**
```bash
git commit -m "feat: Budget + Account pages integrated, loading/error states added to all pages"
```

---

## 📅 DAY 6 — Integration Test + Bug Fix + Sprint Review
**6 tiếng | Thứ Bảy**

### 🌅 Daily Standup
```
✅ Hôm qua: Toàn bộ FE đã kết nối BE thật
🎯 Hôm nay: Test toàn flow, fix bug, Sprint Review
🚧 Blocker: List tất cả bug/UI glitch ghi nhận từ Day 4-5
```

---

### ⏰ 09:00–11:30 | T19: Integration Test End-to-End (2.5h)

**Test flow hoàn chỉnh (chạy từ đầu đến cuối):**

```
Auth Flow:
1.  Mở http://localhost:5173                    ✅/❌
2.  Truy cập dashboard khi chưa login → redirect login ✅/❌
3.  Register tài khoản mới                      ✅/❌
4.  Login → redirect Dashboard                  ✅/❌
5.  F5 sau khi login → vẫn ở dashboard          ✅/❌

Account Flow:
6.  Tạo account "Ví tiền mặt" 5,000,000₫       ✅/❌
7.  Account xuất hiện trong dropdown form       ✅/❌

Transaction Flow:
8.  Thêm giao dịch: Thu - Lương - 15,000,000₫  ✅/❌
9.  Thêm giao dịch: Chi - Ăn uống - 200,000₫   ✅/❌
10. Thêm giao dịch: Chi - Đi lại - 50,000₫     ✅/❌
11. Transaction list hiển thị 3 giao dịch       ✅/❌
12. Filter type=EXPENSE → chỉ thấy 2 giao dịch ✅/❌
13. Xóa 1 giao dịch → biến mất khỏi list       ✅/❌

Dashboard Flow:
14. Dashboard: totalIncome=15,000,000₫          ✅/❌
15. Dashboard: totalExpense=250,000₫            ✅/❌
16. Donut chart hiển thị Ăn uống + Đi lại      ✅/❌
17. Trend chart có 6 tháng (tháng trống = 0)   ✅/❌
18. Đổi tháng → summary cập nhật đúng          ✅/❌

Budget Flow:
19. Tạo budget Ăn uống 500,000₫ tháng này      ✅/❌
20. Budget status: 200,000/500,000 = 40% (OK)  ✅/❌
21. Tạo budget Ăn uống lần 2 → lỗi 409 rõ ràng ✅/❌

Edge Cases:
22. Thêm transaction amount âm → validation FE/BE ✅/❌
23. Tháng không có data → empty state đẹp       ✅/❌
24. Logout → clear token → redirect login        ✅/❌
25. User B không thấy data User A (test isolation) ✅/❌
```

---

### ⏰ 11:30–13:30 | T20: Bug Fix (2h)

**Bugs thường gặp khi kết nối FE-BE:**

| Bug | Nguyên nhân | Fix |
|-----|------------|-----|
| Chart không render | Thiếu `ChartJS.register()` | Import đủ các module Chart.js |
| Số tiền hiển thị sai format | Không dùng `Intl.NumberFormat` | Tạo helper `formatVND()` dùng chung |
| Filter tháng lệch 1 tháng | Timezone issue với `new Date()` | Dùng string `YYYY-MM` trực tiếp |
| Form reset sau submit | state không clear | Reset formData trong `onSuccess` |
| Loading spinner không tắt khi lỗi | Thiếu `finally { setLoading(false) }` | Thêm finally block |
| Dropdown category rỗng | Load categories trước khi mount | Dùng `useEffect` với dependency rỗng |

---

### ⏰ 14:00–15:00 | T21: Sprint Review + Retro (1h)

**Deliverables Checklist:**

| # | Deliverable | Status |
|---|------------|--------|
| 1 | `GET /api/summary` trả đúng tổng thu/chi/balance | ⬜ |
| 2 | `GET /api/summary/trend` trả 6 tháng đủ | ⬜ |
| 3 | Budget CRUD API hoạt động | ⬜ |
| 4 | `GET /api/budgets/status` với OK/WARNING/EXCEEDED | ⬜ |
| 5 | Dashboard kết nối API thật, chart render đúng | ⬜ |
| 6 | Transaction list filter theo tháng/type | ⬜ |
| 7 | Form thêm giao dịch submit được, reload list | ⬜ |
| 8 | Budget page hiển thị progress bar đúng màu | ⬜ |
| 9 | Loading + error states toàn FE | ⬜ |
| 10 | Unit tests BE GREEN | ⬜ |

**Sprint Velocity:**
```
Task planned: 21 | Task done: ___ | %: ___
```

**Retrospective (3 câu):**
```
✅ Went WELL:    ...
⚠️ Improve:      ...
🚀 Next Sprint:  ...
```

**Final Commit:**
```bash
git commit -m "chore: integration bugs fixed, all flows tested, Sprint 3 complete"
git tag -a sprint-3 -m "Sprint 3: Personal Finance MVP - Backend + FE Integration"
git push origin main --tags
```

---

## 📊 Sprint 3 Summary

### Time Distribution
```
Day 1: Summary API + tests               → 6h
Day 2: Trend API + Budget CRUD           → 6h
Day 3: Budget Status + tests + Postman   → 6h  ← query JOIN phức tạp nhất
Day 4: CORS + Dashboard + Transaction    → 6h  ← nhiều file FE cần sửa
Day 5: Budget + Account + Error states   → 6h
Day 6: Integration test + bug + review   → 6h
──────────────────────────────────────────────
Total:                                    36h
```

### Nếu Bị Trễ — Cắt Theo Thứ Tự
```
Giữ:   Summary API + Budget Status + Dashboard kết nối BE
Giảm:  Trend chart (bỏ bar chart, chỉ giữ số liệu)
Dời:   Account management UI (dùng Swagger để quản lý tạm)
Bỏ:    Empty states đẹp (để text đơn giản)
```

---

*Sprint 3 Complete → Personal Finance MVP ✅ → Sprint 4: Family Group Management* 🚀
