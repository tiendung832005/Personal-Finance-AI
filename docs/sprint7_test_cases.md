# Sprint 7 Test Plan: AI Finance Insights

Tài liệu này hướng dẫn các bước kiểm thử chức năng AI phân tích tài chính đã triển khai trong Sprint 7.

## 1. Kiểm thử Monthly AI Insights
- **TC-INS-01: Lần đầu sinh Insight**
  - **Dữ liệu**: User có ít nhất 1-2 giao dịch chi tiêu trong tháng.
  - **Thực hiện**: Vào tab Dashboard hoặc AI Insight.
  - **Kết quả mong đợi**: AI mất vài giây để phân tích, sau đó hiển thị đoạn nhận xét 3-5 câu bằng tiếng Việt chuyên nghiệp.
- **TC-INS-02: Kiểm tra Caching**
  - **Thực hiện**: F5 trang hoặc chuyển sang tab khác rồi quay lại.
  - **Kết quả mong đợi**: Dữ liệu hiển thị ngay lập tức (isFromCache = true), backend không gọi lại Gemini API (kiểm tra log console).
- **TC-INS-03: Regenerate Insight**
  - **Thực hiện**: Nhấn nút "Làm mới từ AI".
  - **Kết quả mong đợi**: Cache cũ bị xóa, AI sinh ra lời khuyên mới nhất.

## 2. Kiểm thử Phát hiện chi tiêu bất thường (Anomaly)
- **TC-ANO-01: Unusual Amount (Quy tắc 3x)**
  - **Chuẩn bị**: Trong 3 tháng trước, trung bình chi Ăn uống là 100k/bữa.
  - **Thực hiện**: Tạo 1 giao dịch mới mục Ăn uống giá 500k.
  - **Kết quả mong đợi**: Xuất hiện cảnh báo màu đỏ/cam ở Dashboard, AI giải thích: "Bữa ăn này cao hơn nhiều so với trung bình, hãy cân nhắc...".
- **TC-ANO-02: New Category**
  - **Thực hiện**: Chi tiêu vào một mục (ví dụ: Đầu tư) mà 3 tháng qua chưa bao giờ dùng.
  - **Kết quả mong đợi**: Hệ thống gắn cờ mục lạ, giúp người dùng nhận ra các khoản chi phát sinh ngoài ý muốn.
- **TC-ANO-03: Dismiss Anomaly**
  - **Thực hiện**: Nhấn nút "Bỏ qua" tại một cảnh báo.
  - **Kết quả mong đợi**: Cảnh báo biến mất, không còn làm phiền người dùng ở Dashboard.

## 3. Kiểm thử Financial Health Score
- **TC-HEA-01: Tính điểm Tiết kiệm**
  - **Kịch bản**: Thu nhập 20 triệu, chi tiêu 10 triệu (Tiết kiệm 50%).
  - **Kết quả mong đợi**: Điểm Savings Score đạt tối đa (50/50).
- **TC-HEA-02: Điểm Ngân sách**
  - **Thực hiện**: Đặt ngân sách 1 triệu cho Mua sắm, chi tiêu thực tế lên 1.2 triệu.
  - **Kết quả mong đợi**: Điểm Budget Score bị trừ, hiển thị breakdown đỏ/vàng.
- **TC-HEA-03: Đối chiếu tổng quát**
  - **Thực hiện**: Kiểm tra điểm Overall và nhãn đánh giá (Xuất sắc/Khá/Cần cải thiện) khớp với dải điểm 0-100.

## 4. Kiểm thử Insight Gia đình
- **TC-FAM-01: Phân tích nhóm**
  - **Thực hiện**: Vào một Family Group, xem Insight chung.
  - **Kết quả mong đợi**: Nhận xét AI tập trung vào "Chúng ta", "Gia đình bạn", và tính toán trên tổng chi tiêu chung thay vì cá nhân.

---
*Ghi chú: Đảm bảo đã chạy Migration V21-V23 trước khi thực hiện test.*
