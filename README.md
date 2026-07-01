# TỔNG HỢP API - Backend QL_Tro (Boarding House API)

**Base URL local:** `http://localhost:8080`  
**API prefix:** `/api`  
**Auth:** JWT Bearer  
**Ngày tổng hợp:** 24/06/2026

## Tóm tắt Hạng mục

| Hạng mục | Tóm tắt |
| :--- | :--- |
| **Tổng số endpoint trong `/api`** | 60 endpoint |
| **Endpoint public chính** | `POST /api/auth/login`, `POST /api/webhooks/payos`, `GET /api/test/**` |
| **Endpoint cần đăng nhập** | Toàn bộ endpoint còn lại dùng `Authorization: Bearer <token>` |
| **Nhóm nghiệp vụ đã có** | Auth, User, Property, Room, Tenant, Contract, Service Price, Meter Reading, Invoice, Payment, Dashboard, Maintenance, Real-time |
| **Chuẩn response** | `ApiResponse { success, message, data, timestamp }` |

## Mục lục nhanh
1. Tổng quan kỹ thuật
2. Xác thực và chuẩn response
3. Thống kê endpoint theo nhóm
4. Danh sách endpoint đã hoàn thành
5. Luồng nghiệp vụ frontend nên tích hợp
6. Quy tắc nghiệp vụ quan trọng
7. Enum hệ thống
8. Checklist tích hợp frontend

---

## 1. Tổng quan kỹ thuật

| Thông tin | Giá trị |
| :--- | :--- |
| **Base URL local** | `http://localhost:8080` |
| **API prefix chính** | `/api` |
| **Health check** | `GET /actuator/health` |
| **Cơ chế xác thực** | JWT Bearer token |
| **Header cho API cần đăng nhập** | `Authorization: Bearer <token>` và `Content-Type: application/json` |
| **Response chuẩn** | Tất cả response thành công/lỗi đều bọc trong `ApiResponse` |
| **Đồng bộ thời gian thực** | WebSocket tại `/ws/realtime` (bắn sự kiện `GLOBAL_UPDATE` và `PAYMENT_UPDATED`) |

---

## 2. Xác thực và chuẩn response

### 2.1. Endpoint đăng nhập

- **Mục đích:** Đăng nhập và lấy JWT (Public)
- **Method:** `POST`
- **Path:** `/api/auth/login`

**Request Body:**
```json
{
  "email": "owner@gmail.com",
  "password": "123456"
}
```

**Response:**
```json
{
  "token": "<jwt>",
  "type": "Bearer",
  "email": "owner@gmail.com",
  "name": "Chu Tro",
  "role": "OWNER"
}
```

### 2.2. Mẫu response chuẩn

**Thành công (Success):**
```json
{
  "success": true,
  "message": "Success",
  "data": {},
  "timestamp": "2026-06-23T18:00:00"
}
```

**Lỗi (Error):**
```json
{
  "success": false,
  "message": "Validation failed: {field=message}",
  "data": null,
  "timestamp": "2026-06-23T18:00:00"
}
```

### 2.3. Status code lỗi chính

| Status | Khi nào dùng |
| :--- | :--- |
| **400** | Body sai, validation fail, enum/query param sai, business rule sai |
| **404** | Không tìm thấy resource |
| **500** | Lỗi không dự kiến |

---

## 3. Thống kê endpoint theo nhóm

| Nhóm | Số endpoint | Phạm vi |
| :--- | :--- | :--- |
| **Auth** | 1 | Đăng nhập lấy token |
| **User** | 1 | Lấy thông tin user hiện tại |
| **Property** | 5 | CRUD nhà trọ/cơ sở |
| **Room** | 6 | CRUD phòng, danh sách phòng theo property, đổi trạng thái |
| **Tenant** | 6 | CRUD khách thuê, tìm kiếm/lọc, danh sách tenant theo phòng |
| **Contract** | 6 | CRUD hợp đồng, terminate, renew |
| **Service Price** | 2 | Lấy/cập nhật giá điện, nước, wifi, rác, giữ xe |
| **Meter Reading** | 6 | CRUD chỉ số điện nước, lịch sử và chỉ số mới nhất |
| **Invoice** | 9 | Tạo, cập nhật, hủy, đánh dấu thanh toán, invoice theo phòng |
| **Payment** | 4 | Tạo payment link PayOS, webhook, xem payment |
| **Dashboard** | 4 | Summary, revenue, debts, room status |
| **Maintenance** | 6 | CRUD yêu cầu bảo trì, cập nhật trạng thái |
| **Dev/Test** | 4 | Test wrapper response và exception handler |

---

## 4. Danh sách endpoint đã hoàn thành

| Nhóm | Method | Path | Mục đích/Ghi chú |
| :--- | :--- | :--- | :--- |
| Auth | POST | `/api/auth/login` | Đăng nhập và lấy JWT |
| User | GET | `/api/users/me` | Lấy user hiện tại, passwordHash trả về null |
| Property | GET | `/api/properties` | Lấy danh sách nhà trọ |
| Property | POST | `/api/properties` | Tạo nhà trọ |
| Property | GET | `/api/properties/{id}` | Lấy chi tiết nhà trọ |
| Property | PUT | `/api/properties/{id}` | Cập nhật nhà trọ |
| Property | DELETE | `/api/properties/{id}` | Xóa nhà trọ |
| Room | GET | `/api/properties/{propertyId}/rooms` | Lấy danh sách phòng theo property |
| Room | POST | `/api/properties/{propertyId}/rooms` | Tạo phòng trong property |
| Room | GET | `/api/rooms/{id}` | Lấy chi tiết phòng |
| Room | PUT | `/api/rooms/{id}` | Cập nhật phòng |
| Room | DELETE | `/api/rooms/{id}` | Xóa phòng |
| Room | PATCH | `/api/rooms/{id}/status` | Cập nhật trạng thái phòng |
| Tenant | GET | `/api/tenants?keyword=&status=` | Tìm kiếm/lọc khách thuê |
| Tenant | POST | `/api/tenants` | Tạo khách thuê |
| Tenant | GET | `/api/tenants/{id}` | Lấy chi tiết khách thuê |
| Tenant | PUT | `/api/tenants/{id}` | Cập nhật khách thuê |
| Tenant | DELETE | `/api/tenants/{id}` | Đánh dấu khách thuê LEFT |
| Tenant | GET | `/api/rooms/{roomId}/tenants` | Lấy khách thuê đang gắn với phòng |
| Contract | GET | `/api/contracts` | Lấy danh sách hợp đồng |
| Contract | POST | `/api/contracts` | Tạo hợp đồng, gắn tenant vào phòng |
| Contract | GET | `/api/contracts/{id}` | Lấy chi tiết hợp đồng |
| Contract | PUT | `/api/contracts/{id}` | Cập nhật hợp đồng |
| Contract | PATCH | `/api/contracts/{id}/terminate` | Kết thúc hợp đồng |
| Contract | PATCH | `/api/contracts/{id}/renew` | Gia hạn hợp đồng |
| Service Price | GET | `/api/properties/{propertyId}/service-prices` | Lấy cấu hình giá dịch vụ |
| Service Price | PUT | `/api/properties/{propertyId}/service-prices` | Cập nhật giá điện, nước, wifi, rác, giữ xe |
| Meter Reading | GET | `/api/meter-readings` | Lấy tất cả chỉ số điện nước |
| Meter Reading | POST | `/api/meter-readings` | Tạo chỉ số điện nước cho phòng/tháng |
| Meter Reading | GET | `/api/rooms/{roomId}/meter-readings` | Lấy lịch sử chỉ số điện nước của phòng |
| Meter Reading | GET | `/api/rooms/{roomId}/latest-meter-reading` | Lấy chỉ số mới nhất của phòng |
| Meter Reading | PUT | `/api/meter-readings/{id}` | Cập nhật chỉ số điện nước |
| Meter Reading | DELETE | `/api/meter-readings/{id}` | Xóa chỉ số điện nước |
| Invoice | GET | `/api/invoices` | Lấy tất cả hóa đơn |
| Invoice | POST | `/api/invoices/generate` | Tạo hóa đơn cho một phòng/tháng |
| Invoice | POST | `/api/invoices/generate-monthly` | Tạo hóa đơn hàng loạt theo property/tháng |
| Invoice | GET | `/api/invoices/{id}` | Lấy chi tiết hóa đơn |
| Invoice | PUT | `/api/invoices/{id}` | Cập nhật phí phụ, giảm giá, hạn thanh toán, ghi chú |
| Invoice | DELETE | `/api/invoices/{id}` | Xóa hóa đơn chưa PAID |
| Invoice | PATCH | `/api/invoices/{id}/mark-paid` | Đánh dấu hóa đơn đã thanh toán thủ công |
| Invoice | PATCH | `/api/invoices/{id}/cancel` | Hủy hóa đơn chưa PAID |
| Invoice | GET | `/api/rooms/{roomId}/invoices` | Lấy danh sách hóa đơn của một phòng |
| Payment | POST | `/api/invoices/{invoiceId}/payment-link` | Tạo hoặc lấy lại link thanh toán PayOS PENDING |
| Payment | POST | `/api/webhooks/payos` | Webhook PayOS public có verify signature |
| Payment | GET | `/api/payments/{id}` | Lấy chi tiết payment |
| Payment | GET | `/api/invoices/{invoiceId}/payments` | Lấy danh sách payment của invoice |
| Dashboard | GET | `/api/dashboard/summary` | Tổng quan phòng, doanh thu, công nợ, bảo trì |
| Dashboard | GET | `/api/dashboard/revenue?month=&year=` | Báo cáo doanh thu theo tháng/năm |
| Dashboard | GET | `/api/dashboard/debts` | Tổng công nợ và invoice chưa thanh toán |
| Dashboard | GET | `/api/dashboard/rooms-status` | Thống kê trạng thái phòng |
| Maintenance | GET | `/api/maintenance-requests?status=` | Lấy/lọc yêu cầu bảo trì theo status |
| Maintenance | POST | `/api/maintenance-requests` | Tạo yêu cầu bảo trì |
| Maintenance | GET | `/api/maintenance-requests/{id}` | Lấy chi tiết yêu cầu bảo trì |
| Maintenance | PUT | `/api/maintenance-requests/{id}` | Cập nhật nội dung/priority yêu cầu bảo trì |
| Maintenance | PATCH | `/api/maintenance-requests/{id}/status` | Cập nhật trạng thái xử lý bảo trì |
| Maintenance | DELETE | `/api/maintenance-requests/{id}` | Xóa yêu cầu bảo trì |

---

## 5. Luồng nghiệp vụ frontend nên tích hợp

| Bước | Luồng | API/Thao tác chính |
| :--- | :--- | :--- |
| **1** | **Đăng nhập** | `POST /api/auth/login` -> lưu `data.token` -> gắn `Authorization: Bearer` cho các API cần đăng nhập. |
| **2** | **Dashboard** | Gọi `/dashboard/summary`, `/dashboard/revenue`, `/dashboard/debts`, `/dashboard/rooms-status` sau khi đăng nhập. |
| **3** | **Quản lý cơ sở** | CRUD `/api/properties`, CRUD room theo property. |
| **4** | **Cấu hình thu phí** | `PUT /api/properties/{propertyId}/service-prices`. |
| **5** | **Quản lý khách thuê** | CRUD `/api/tenants`, xem tenant theo phòng bằng `/api/rooms/{roomId}/tenants`. |
| **6** | **Lập hợp đồng** | `POST /api/contracts` với `roomId` + `tenantIds` rỗng; terminate/renew khi trả phòng hoặc gia hạn. |
| **7** | **Hàng tháng** | Tạo meter reading, tạo invoice đơn lẻ hoặc `generate-monthly`. |
| **8** | **Thu tiền** | Tạo PayOS payment link, hiển thị `checkoutUrl`/`qrCode`, poll invoice/payment hoặc nghe WebSocket để cập nhật. |
| **9** | **Bảo trì** | Tạo request, lọc theo status, cập nhật trạng thái `PENDING` -> `IN_PROGRESS` -> `DONE`/`CANCELLED`. |

---

## 6. Quy tắc nghiệp vụ quan trọng

| Module | Rule |
| :--- | :--- |
| **Room** | Trạng thái hợp lệ: `AVAILABLE`, `OCCUPIED`, `RESERVED`, `MAINTENANCE`. Xóa phòng có thể bị chặn nếu có hợp đồng/dữ liệu liên quan. |
| **Tenant** | DELETE tenant không hard delete ngay mà đánh dấu status `LEFT`. |
| **Contract** | Tạo contract cần `roomId` và `tenantIds` không rỗng. Backend tự động cập nhật phòng thành `OCCUPIED` và gắn tenant vào phòng. |
| **Service Price** | Tất cả field giá dịch vụ bắt buộc và `>= 0`. |
| **Meter Reading** | Không tạo trùng `roomId` + `month` + `year`. Chỉ số mới phải `>=` chỉ số cũ. Không sửa/xóa nếu invoice cùng kỳ đã `PAID`. |
| **Invoice** | Chỉ tạo invoice khi room có active contract, property có service price, có meter reading đúng kỳ và chưa có invoice trùng. |
| **Invoice** | `totalAmount = rent + electricity + water + wifi + garbage + parking + otherFees - discountAmount`. |
| **Payment PayOS** | Invoice `PAID`/`CANCELLED` không tạo payment link mới. Nếu đã có payment `PENDING` thì trả lại link cũ. |
| **Webhook PayOS** | Webhook public nhưng phải verify signature; success code `00` cập nhật payment `PAID` và invoice `PAID`. |
| **Dashboard** | Dùng để render màn tổng quan sau login; revenue có `month`/`year` optional, nếu bỏ trống backend dùng kỳ hiện tại. |
| **Maintenance** | Status hợp lệ: `PENDING`, `IN_PROGRESS`, `DONE`, `CANCELLED`; priority: `LOW`, `MEDIUM`, `HIGH`. |

---

## 7. Enum hệ thống

| Enum | Giá trị |
| :--- | :--- |
| **UserRole** | `OWNER` |
| **RoomStatus** | `AVAILABLE`, `OCCUPIED`, `RESERVED`, `MAINTENANCE` |
| **TenantStatus** | `ACTIVE`, `LEFT` |
| **ContractStatus** | `ACTIVE`, `EXPIRED`, `TERMINATED`, `PENDING` |
| **InvoiceStatus** | `UNPAID`, `PAID`, `PARTIAL`, `OVERDUE`, `CANCELLED` |
| **PaymentProvider** | `PAYOS` |
| **PaymentStatus** | `PENDING`, `PAID`, `FAILED`, `CANCELLED` |
| **MaintenancePriority** | `LOW`, `MEDIUM`, `HIGH` |
| **MaintenanceStatus** | `PENDING`, `IN_PROGRESS`, `DONE`, `CANCELLED` |

---

## 8. Checklist tích hợp frontend

- [x] Tạo service HTTP dùng chung, tự động gắn `Authorization Bearer` sau khi login.
- [x] Chuẩn hóa parser response theo `ApiResponse`: luôn đọc `success`, `message`, `data`, `timestamp`.
- [x] Với lỗi 400/404/500, hiển thị message từ backend thay vì chỉ hiện lỗi chung chung.
- [x] Form create/update cần validate frontend theo enum và rule số tiền/chỉ số trước khi gọi API.
- [x] Invoice/payment cần có cơ chế nghe Real-time qua WebSocket (`/ws/realtime`) thay vì poll liên tục để cập nhật trạng thái PayOS.
- [x] Webhook `/api/webhooks/payos` không gọi trực tiếp từ frontend trong production (do server PayOS tự gọi).
- [x] Các màn danh sách nên có filter/search: tenants theo `keyword`/`status`, maintenance theo `status`, dashboard revenue theo `month`/`year`.
- [x] Khi tạo invoice hàng tháng, đọc `createdInvoices`, `skippedRooms`, `errors` để báo kết quả rõ cho người dùng.
- [x] Không hiển thị nút thao tác không hợp lệ: sửa/xóa meter reading khi invoice đã `PAID`, xóa/hủy invoice đã `PAID`, tạo payment cho invoice `CANCELLED`.
- [x] Đã tích hợp Realtime `GLOBAL_UPDATE` để tự động làm mới giao diện mọi màn hình khi có dữ liệu bị thay đổi từ client khác.

---

## 9. Ghi chú dùng nhanh cho frontend

**Thứ tự tích hợp tối thiểu được khuyến nghị:**
`Auth` -> `Dashboard` -> `Property/Room` -> `Service Price` -> `Tenant` -> `Contract` -> `Meter Reading` -> `Invoice` -> `Payment` -> `Maintenance`.

*(Tài liệu này dùng làm sổ tay bàn giao và tham chiếu cho toàn bộ API đã phát triển trong hệ thống Quản Lý Trọ)*
