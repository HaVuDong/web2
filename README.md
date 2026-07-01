# TỔNG HỢP GIAO DIỆN LẬP TRÌNH ỨNG DỤNG (API) ĐÃ HOÀN THÀNH
**Máy chủ Hệ thống Quản Lý Trọ (Backend QL_Tro)**

- **Địa chỉ gốc cục bộ (Base URL local):** `http://localhost:8080`
- **Tiền tố API (API prefix):** `/api`
- **Xác thực (Auth):** Sử dụng Mã thông báo JWT (JWT Bearer)
- **Ngày tổng hợp:** 24/06/2026

## Tóm tắt Hạng mục

| Hạng mục | Tóm tắt |
| :--- | :--- |
| **Tổng số đường dẫn API** | 60 đường dẫn (endpoint) |
| **Các đường dẫn công khai chính** | `POST /api/auth/login`, `POST /api/webhooks/payos`, `GET /api/test/**` |
| **Các đường dẫn yêu cầu đăng nhập** | Toàn bộ các đường dẫn còn lại đều bắt buộc gửi kèm Mã thông báo ở Header: `Authorization: Bearer <token>` |
| **Nhóm nghiệp vụ đã hoàn thiện** | Xác thực, Người dùng, Nhà trọ, Phòng, Khách thuê, Hợp đồng, Giá dịch vụ, Chốt số điện nước, Hóa đơn, Thanh toán, Bảng thống kê, Yêu cầu bảo trì, Đồng bộ dữ liệu tức thời (Real-time) |
| **Chuẩn phản hồi (Response)** | Định dạng chuẩn: `ApiResponse { success, message, data, timestamp }` |

## Mục lục nhanh
1. Tổng quan kỹ thuật
2. Xác thực và chuẩn phản hồi
3. Thống kê đường dẫn theo nhóm
4. Danh sách đường dẫn đã hoàn thành
5. Luồng nghiệp vụ dành cho ứng dụng (Frontend)
6. Quy tắc nghiệp vụ quan trọng
7. Các giá trị cố định (Enum) của hệ thống
8. Danh sách kiểm tra khi tích hợp

---

## 1. Tổng quan kỹ thuật

| Thông tin | Giá trị |
| :--- | :--- |
| **Địa chỉ gốc cục bộ** | `http://localhost:8080` |
| **Tiền tố API chính** | `/api` |
| **Kiểm tra sức khỏe hệ thống** | `GET /actuator/health` |
| **Cơ chế xác thực** | Mã thông báo JWT (Bearer token) |
| **Cấu hình cho API cần đăng nhập** | Gửi kèm tiêu đề: `Authorization: Bearer <token>` và `Content-Type: application/json` |
| **Phản hồi chuẩn** | Tất cả kết quả trả về (dù thành công hay lỗi) đều được bọc trong đối tượng `ApiResponse` |
| **Đồng bộ thời gian thực** | Sử dụng kết nối liên tục (WebSocket) tại `/ws/realtime` (Hệ thống sẽ gửi sự kiện `GLOBAL_UPDATE` và `PAYMENT_UPDATED`) |

---

## 2. Xác thực và chuẩn phản hồi

### 2.1. Đường dẫn đăng nhập

- **Mục đích:** Đăng nhập và lấy Mã thông báo (Công khai)
- **Phương thức:** `POST`
- **Đường dẫn:** `/api/auth/login`

**Dữ liệu gửi lên (Request Body):**
```json
{
  "email": "owner@gmail.com",
  "password": "123456"
}
```

**Dữ liệu trả về (Response):**
```json
{
  "token": "<mã_jwt>",
  "type": "Bearer",
  "email": "owner@gmail.com",
  "name": "Chu Tro",
  "role": "OWNER"
}
```

### 2.2. Mẫu phản hồi chuẩn

**Thành công:**
```json
{
  "success": true,
  "message": "Thành công",
  "data": {},
  "timestamp": "2026-06-23T18:00:00"
}
```

**Lỗi:**
```json
{
  "success": false,
  "message": "Lỗi xác thực dữ liệu: {trường_bị_lỗi=thông_báo_lỗi}",
  "data": null,
  "timestamp": "2026-06-23T18:00:00"
}
```

### 2.3. Các mã trạng thái lỗi (HTTP Status code) chính

| Mã trạng thái | Khi nào sử dụng |
| :--- | :--- |
| **400 (Yêu cầu không hợp lệ)** | Dữ liệu gửi lên bị sai, không qua được kiểm tra xác thực, sai tham số, vi phạm quy tắc nghiệp vụ |
| **404 (Không tìm thấy)** | Không tìm thấy dữ liệu yêu cầu |
| **500 (Lỗi máy chủ)** | Xảy ra lỗi hệ thống ngoài dự kiến |

---

## 3. Thống kê đường dẫn API theo nhóm

| Nhóm chức năng | Số lượng | Phạm vi thực hiện |
| :--- | :--- | :--- |
| **Xác thực (Auth)** | 1 | Đăng nhập lấy mã thông báo |
| **Người dùng (User)** | 1 | Lấy thông tin người dùng hiện tại |
| **Nhà trọ (Property)** | 5 | Thêm, xem, sửa, xóa cơ sở nhà trọ |
| **Phòng (Room)** | 6 | Thêm, xem, sửa, xóa phòng, danh sách phòng theo cơ sở, đổi trạng thái phòng |
| **Khách thuê (Tenant)** | 6 | Thêm, xem, sửa, xóa khách thuê, tìm kiếm/lọc, danh sách khách theo phòng |
| **Hợp đồng (Contract)** | 6 | Thêm, xem, sửa hợp đồng, kết thúc, gia hạn |
| **Giá dịch vụ (Service Price)**| 2 | Xem/cập nhật giá điện, nước, wifi, rác, giữ xe |
| **Chốt số điện nước** | 6 | Thêm, xem, sửa, xóa chỉ số điện nước, xem lịch sử và chỉ số mới nhất |
| **Hóa đơn (Invoice)** | 9 | Tạo, cập nhật, hủy, đánh dấu đã thanh toán, xem hóa đơn theo phòng |
| **Thanh toán (Payment)** | 4 | Tạo đường dẫn thanh toán PayOS, nhận thông báo tự động (webhook), xem lịch sử thanh toán |
| **Bảng thống kê (Dashboard)** | 4 | Thống kê tổng quan, doanh thu, công nợ, tình trạng phòng |
| **Bảo trì (Maintenance)** | 6 | Thêm, xem, sửa, xóa yêu cầu bảo trì, cập nhật trạng thái xử lý |
| **Môi trường thử nghiệm** | 4 | Kiểm tra các mẫu phản hồi và bộ xử lý lỗi |

---

## 4. Danh sách API đã hoàn thành chi tiết

| Nhóm | Phương thức | Đường dẫn (Path) | Mục đích / Ghi chú |
| :--- | :--- | :--- | :--- |
| Xác thực | POST | `/api/auth/login` | Đăng nhập và lấy mã thông báo |
| Người dùng | GET | `/api/users/me` | Lấy thông tin tài khoản đang đăng nhập (ẩn mật khẩu) |
| Nhà trọ | GET | `/api/properties` | Lấy danh sách nhà trọ |
| Nhà trọ | POST | `/api/properties` | Tạo nhà trọ mới |
| Nhà trọ | GET | `/api/properties/{id}` | Xem chi tiết một nhà trọ |
| Nhà trọ | PUT | `/api/properties/{id}` | Cập nhật thông tin nhà trọ |
| Nhà trọ | DELETE | `/api/properties/{id}` | Xóa nhà trọ |
| Phòng | GET | `/api/properties/{propertyId}/rooms` | Lấy danh sách phòng thuộc một nhà trọ |
| Phòng | POST | `/api/properties/{propertyId}/rooms` | Tạo phòng mới trong nhà trọ |
| Phòng | GET | `/api/rooms/{id}` | Lấy chi tiết một phòng |
| Phòng | PUT | `/api/rooms/{id}` | Cập nhật thông tin phòng |
| Phòng | DELETE | `/api/rooms/{id}` | Xóa phòng |
| Phòng | PATCH | `/api/rooms/{id}/status` | Đổi trạng thái phòng |
| Khách thuê | GET | `/api/tenants?keyword=&status=` | Tìm kiếm và lọc danh sách khách thuê |
| Khách thuê | POST | `/api/tenants` | Thêm khách thuê mới |
| Khách thuê | GET | `/api/tenants/{id}` | Xem chi tiết khách thuê |
| Khách thuê | PUT | `/api/tenants/{id}` | Cập nhật thông tin khách |
| Khách thuê | DELETE | `/api/tenants/{id}` | Đánh dấu khách thuê đã rời đi |
| Khách thuê | GET | `/api/rooms/{roomId}/tenants` | Lấy danh sách khách thuê đang ở trong phòng |
| Hợp đồng | GET | `/api/contracts` | Lấy danh sách hợp đồng |
| Hợp đồng | POST | `/api/contracts` | Lập hợp đồng mới, tự động gắn khách vào phòng |
| Hợp đồng | GET | `/api/contracts/{id}` | Lấy chi tiết hợp đồng |
| Hợp đồng | PUT | `/api/contracts/{id}` | Cập nhật nội dung hợp đồng |
| Hợp đồng | PATCH | `/api/contracts/{id}/terminate` | Kết thúc hợp đồng (khách trả phòng) |
| Hợp đồng | PATCH | `/api/contracts/{id}/renew` | Gia hạn hợp đồng |
| Giá dịch vụ | GET | `/api/properties/{propertyId}/service-prices`| Xem bảng giá dịch vụ của nhà trọ |
| Giá dịch vụ | PUT | `/api/properties/{propertyId}/service-prices`| Cập nhật giá điện, nước, wifi, rác, xe |
| Số điện nước| GET | `/api/meter-readings` | Lấy danh sách toàn bộ chỉ số |
| Số điện nước| POST | `/api/meter-readings` | Tạo chốt số điện nước cho một phòng/tháng |
| Số điện nước| GET | `/api/rooms/{roomId}/meter-readings` | Xem lịch sử chốt số của một phòng |
| Số điện nước| GET | `/api/rooms/{roomId}/latest-meter-reading` | Lấy chỉ số chốt gần nhất của phòng |
| Số điện nước| PUT | `/api/meter-readings/{id}` | Cập nhật số điện nước |
| Số điện nước| DELETE | `/api/meter-readings/{id}` | Xóa bản ghi chốt số |
| Hóa đơn | GET | `/api/invoices` | Lấy danh sách toàn bộ hóa đơn |
| Hóa đơn | POST | `/api/invoices/generate` | Tạo hóa đơn cho một phòng/tháng |
| Hóa đơn | POST | `/api/invoices/generate-monthly` | Tạo hóa đơn hàng loạt cho cả nhà trọ/tháng |
| Hóa đơn | GET | `/api/invoices/{id}` | Xem chi tiết hóa đơn |
| Hóa đơn | PUT | `/api/invoices/{id}` | Cập nhật phụ phí, giảm giá, hạn chót, ghi chú |
| Hóa đơn | DELETE | `/api/invoices/{id}` | Xóa hóa đơn chưa thanh toán |
| Hóa đơn | PATCH | `/api/invoices/{id}/mark-paid` | Đánh dấu hóa đơn đã thanh toán bằng tay |
| Hóa đơn | PATCH | `/api/invoices/{id}/cancel` | Hủy bỏ hóa đơn chưa thanh toán |
| Hóa đơn | GET | `/api/rooms/{roomId}/invoices` | Lấy danh sách hóa đơn của một phòng |
| Thanh toán | POST | `/api/invoices/{invoiceId}/payment-link`| Tạo mới hoặc lấy lại đường dẫn thanh toán qua PayOS |
| Thanh toán | POST | `/api/webhooks/payos` | Điểm nhận thông báo công khai từ PayOS (có kiểm tra chữ ký xác thực) |
| Thanh toán | GET | `/api/payments/{id}` | Xem chi tiết giao dịch thanh toán |
| Thanh toán | GET | `/api/invoices/{invoiceId}/payments`| Lấy lịch sử giao dịch của một hóa đơn |
| Thống kê | GET | `/api/dashboard/summary` | Xem tổng quan phòng, doanh thu, công nợ, bảo trì |
| Thống kê | GET | `/api/dashboard/revenue?month=&year=`| Xem báo cáo doanh thu theo tháng/năm |
| Thống kê | GET | `/api/dashboard/debts` | Tổng công nợ và danh sách hóa đơn chưa thu |
| Thống kê | GET | `/api/dashboard/rooms-status` | Thống kê số lượng phòng trống/đang thuê |
| Bảo trì | GET | `/api/maintenance-requests?status=` | Xem và lọc yêu cầu bảo trì theo trạng thái |
| Bảo trì | POST | `/api/maintenance-requests` | Tạo yêu cầu bảo trì mới |
| Bảo trì | GET | `/api/maintenance-requests/{id}` | Lấy chi tiết một yêu cầu |
| Bảo trì | PUT | `/api/maintenance-requests/{id}` | Sửa nội dung hoặc mức độ ưu tiên của yêu cầu |
| Bảo trì | PATCH | `/api/maintenance-requests/{id}/status` | Đổi trạng thái xử lý bảo trì |
| Bảo trì | DELETE | `/api/maintenance-requests/{id}` | Xóa yêu cầu bảo trì |

---

## 5. Luồng nghiệp vụ ưu tiên cho Ứng dụng (Frontend)

| Bước | Quy trình | Nhóm API và thao tác chính |
| :--- | :--- | :--- |
| **1** | **Đăng nhập** | Gọi `POST /api/auth/login` -> lưu mã `data.token` -> tự động gắn `Authorization: Bearer` cho mọi API khác. |
| **2** | **Xem thống kê** | Gọi các API nhóm Thống kê (Summary, Revenue, Debts, Rooms-status) ngay sau khi đăng nhập. |
| **3** | **Quản lý cơ sở** | Thao tác Thêm/Sửa/Xóa nhà trọ (`/api/properties`) và quản lý phòng thuộc nhà trọ đó. |
| **4** | **Bảng giá thu phí** | Gọi `PUT /api/properties/{propertyId}/service-prices` để thiết lập giá. |
| **5** | **Quản lý khách** | Thao tác danh sách khách thuê (`/api/tenants`), xem khách theo từng phòng. |
| **6** | **Ký hợp đồng** | Lập hợp đồng mới (`POST /api/contracts`); kết thúc hoặc gia hạn hợp đồng khi hết hạn. |
| **7** | **Đầu tháng/Cuối tháng**| Chốt số điện nước, sau đó tạo hóa đơn lẻ hoặc tạo hàng loạt cho cả nhà trọ. |
| **8** | **Thu tiền** | Tạo đường dẫn thanh toán PayOS, hiển thị mã QR, tự động cập nhật trạng thái thông qua kết nối liên tục (WebSocket). |
| **9** | **Bảo trì sửa chữa** | Lập phiếu yêu cầu, cập nhật quá trình từ lúc Chờ xử lý -> Đang xử lý -> Hoàn thành. |

---

## 6. Quy tắc nghiệp vụ quan trọng

| Tính năng | Quy tắc ràng buộc |
| :--- | :--- |
| **Phòng** | Các trạng thái cho phép: Trống (`AVAILABLE`), Đang thuê (`OCCUPIED`), Đã đặt cọc (`RESERVED`), Đang sửa chữa (`MAINTENANCE`). Sẽ bị chặn xóa nếu phòng đang có dữ liệu ràng buộc. |
| **Khách thuê** | Khi xóa khách thuê, hệ thống không xóa vĩnh viễn mà chỉ đánh dấu trạng thái là Đã rời đi (`LEFT`). |
| **Hợp đồng** | Khi lập hợp đồng bắt buộc phải truyền mã phòng và danh sách khách. Hệ thống sẽ tự động cập nhật phòng thành Đang thuê (`OCCUPIED`) và gắn khách vào phòng đó. |
| **Giá dịch vụ** | Bắt buộc phải có đầy đủ các mục giá dịch vụ cơ bản và giá trị phải `>= 0`. |
| **Chốt điện nước** | Không được chốt trùng tháng/năm của cùng một phòng. Chỉ số mới nhập bắt buộc phải `>=` chỉ số cũ. Không cho phép sửa/xóa nếu hóa đơn của tháng đó đã được thanh toán. |
| **Hóa đơn** | Chỉ có thể tạo hóa đơn khi: phòng có hợp đồng còn hiệu lực, nhà trọ đã thiết lập giá, đã có chốt số điện nước của tháng, và chưa có hóa đơn nào trùng lặp. |
| **Thành tiền** | Tính theo công thức: Tiền phòng + Điện + Nước + Wifi + Rác + Gửi xe + Phụ phí - Tiền giảm giá. |
| **Thanh toán PayOS** | Hóa đơn đã thanh toán hoặc đã hủy sẽ không được phép tạo đường dẫn thanh toán mới. Nếu đang có đường dẫn chờ thanh toán, hệ thống sẽ trả lại đường dẫn cũ. |
| **Nhận thông báo PayOS** | Giao diện công khai nhưng hệ thống sẽ kiểm tra chữ ký bảo mật. Nếu giao dịch thành công (mã `00`), hệ thống sẽ tự đổi trạng thái thanh toán và hóa đơn thành Đã thanh toán (`PAID`). |
| **Thống kê** | Nếu gọi báo cáo doanh thu mà không truyền tháng/năm, máy chủ sẽ tự lấy dữ liệu của tháng hiện tại. |
| **Bảo trì** | Các trạng thái cho phép: Chờ xử lý (`PENDING`), Đang thực hiện (`IN_PROGRESS`), Hoàn tất (`DONE`), Đã hủy (`CANCELLED`). Độ ưu tiên gồm: Thấp (`LOW`), Trung bình (`MEDIUM`), Cao (`HIGH`). |

---

## 7. Các giá trị cố định (Enum) của hệ thống

| Tên nhóm | Các giá trị hợp lệ |
| :--- | :--- |
| **Vai trò (UserRole)** | Chủ nhà (`OWNER`) |
| **Tình trạng phòng (RoomStatus)** | `AVAILABLE`, `OCCUPIED`, `RESERVED`, `MAINTENANCE` |
| **Tình trạng khách (TenantStatus)** | Đang thuê (`ACTIVE`), Đã rời đi (`LEFT`) |
| **Trạng thái hợp đồng (ContractStatus)** | Đang hiệu lực (`ACTIVE`), Hết hạn (`EXPIRED`), Đã chấm dứt (`TERMINATED`), Chờ duyệt (`PENDING`) |
| **Trạng thái hóa đơn (InvoiceStatus)** | Chưa thanh toán (`UNPAID`), Đã thanh toán (`PAID`), Thanh toán một phần (`PARTIAL`), Quá hạn (`OVERDUE`), Đã hủy (`CANCELLED`) |
| **Đơn vị thanh toán (PaymentProvider)**| `PAYOS` |
| **Trạng thái giao dịch (PaymentStatus)**| Chờ thanh toán (`PENDING`), Thành công (`PAID`), Thất bại (`FAILED`), Đã hủy (`CANCELLED`) |
| **Độ ưu tiên bảo trì (MaintenancePriority)** | `LOW`, `MEDIUM`, `HIGH` |
| **Trạng thái bảo trì (MaintenanceStatus)** | `PENDING`, `IN_PROGRESS`, `DONE`, `CANCELLED` |

---

## 8. Danh sách kiểm tra dành cho đội ngũ phát triển giao diện (Frontend)

- [x] Thiết lập bộ xử lý gọi API dùng chung, tự động gắn tiêu đề xác thực (`Authorization Bearer`) ngay sau khi đăng nhập thành công.
- [x] Xử lý đồng bộ dữ liệu trả về theo chuẩn `ApiResponse`: luôn đọc các trường `success`, `message`, `data`, `timestamp`.
- [x] Với các lỗi hệ thống (400, 404, 500), ưu tiên hiển thị dòng thông báo lỗi (`message`) từ máy chủ trả về thay vì hiện thông báo lỗi chung chung.
- [x] Trước khi gửi yêu cầu Thêm/Sửa, ứng dụng cần tự kiểm tra dữ liệu đầu vào (ví dụ: số tiền/chỉ số không được âm).
- [x] Ứng dụng đã tích hợp công nghệ kết nối liên tục (WebSocket tại `/ws/realtime`) để tự động cập nhật trạng thái thanh toán PayOS mà không cần gọi API liên tục (không dùng cơ chế polling).
- [x] Đường dẫn nhận thông báo (Webhook) của PayOS chỉ dành cho máy chủ PayOS gọi, ứng dụng giao diện tuyệt đối không được gọi trực tiếp.
- [x] Các màn hình danh sách đã có đầy đủ bộ lọc tìm kiếm: lọc khách thuê theo từ khóa/trạng thái, lọc bảo trì theo trạng thái, lọc thống kê doanh thu theo tháng/năm.
- [x] Khi tính năng tạo hóa đơn hàng loạt được kích hoạt, ứng dụng sẽ đọc và hiển thị rõ thông tin chi tiết: số hóa đơn thành công, số phòng bị bỏ qua, và các lỗi phát sinh.
- [x] Không hiển thị các nút thao tác trái quy tắc: ẩn nút Sửa/Xóa số điện nước nếu hóa đơn đã được thanh toán, ẩn nút Xóa/Hủy hóa đơn đã thanh toán.
- [x] Đã xử lý bắt sự kiện cập nhật toàn cầu (`GLOBAL_UPDATE`) qua WebSocket để làm mới giao diện tại mọi màn hình khi có dữ liệu bị thay đổi từ nơi khác.

*(Tài liệu này dùng làm sổ tay tham chiếu chuẩn bằng tiếng Việt cho toàn bộ kiến trúc máy chủ và giao diện của Hệ thống Quản Lý Trọ)*
