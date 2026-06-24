# Boarding House API README

Tai lieu nay liet ke cac API hien co cua backend `QL_Tro`, duoc chia theo luong su dung cho frontend app quan ly nha tro.

## Tong Quan

- Base URL local: `http://localhost:8080`
- API prefix chinh: `/api`
- Health check: `GET /actuator/health`
- Auth: JWT Bearer token.
- Header cho API can dang nhap:

```http
Authorization: Bearer <token>
Content-Type: application/json
```

Tat ca response thanh cong duoc boc trong `ApiResponse`:

```json
{
  "success": true,
  "message": "Success",
  "data": {},
  "timestamp": "2026-06-23T18:00:00"
}
```

Response loi cung dung `ApiResponse`, voi `success=false` va `data=null`:

```json
{
  "success": false,
  "message": "Validation failed: {field=message}",
  "data": null,
  "timestamp": "2026-06-23T18:00:00"
}
```

Status code loi chinh:

| Status | Khi nao |
| --- | --- |
| `400` | Body sai, validation fail, enum/query param sai, business rule sai |
| `404` | Khong tim thay resource |
| `500` | Loi khong du kien |

Endpoint public:

| Method | Path | Ghi chu |
| --- | --- | --- |
| `POST` | `/api/auth/login` | Dang nhap lay JWT |
| `POST` | `/api/webhooks/payos` | PayOS goi webhook, frontend khong nen goi truc tiep |
| `GET` | `/api/test/**` | API test exception/dev |
| `GET` | `/actuator/**` | Health/actuator |

Tat ca endpoint con lai can `Authorization: Bearer <token>`.

## Tom Tat Endpoint

| Nhom | Method | Path |
| --- | --- | --- |
| Auth | `POST` | `/api/auth/login` |
| User | `GET` | `/api/users/me` |
| Property | `GET` | `/api/properties` |
| Property | `POST` | `/api/properties` |
| Property | `GET` | `/api/properties/{id}` |
| Property | `PUT` | `/api/properties/{id}` |
| Property | `DELETE` | `/api/properties/{id}` |
| Room | `GET` | `/api/properties/{propertyId}/rooms` |
| Room | `POST` | `/api/properties/{propertyId}/rooms` |
| Room | `GET` | `/api/rooms/{id}` |
| Room | `PUT` | `/api/rooms/{id}` |
| Room | `DELETE` | `/api/rooms/{id}` |
| Room | `PATCH` | `/api/rooms/{id}/status` |
| Tenant | `GET` | `/api/tenants?keyword=&status=` |
| Tenant | `POST` | `/api/tenants` |
| Tenant | `GET` | `/api/tenants/{id}` |
| Tenant | `PUT` | `/api/tenants/{id}` |
| Tenant | `DELETE` | `/api/tenants/{id}` |
| Tenant | `GET` | `/api/rooms/{roomId}/tenants` |
| Contract | `GET` | `/api/contracts` |
| Contract | `POST` | `/api/contracts` |
| Contract | `GET` | `/api/contracts/{id}` |
| Contract | `PUT` | `/api/contracts/{id}` |
| Contract | `PATCH` | `/api/contracts/{id}/terminate` |
| Contract | `PATCH` | `/api/contracts/{id}/renew` |
| Service Price | `GET` | `/api/properties/{propertyId}/service-prices` |
| Service Price | `PUT` | `/api/properties/{propertyId}/service-prices` |
| Meter Reading | `GET` | `/api/meter-readings` |
| Meter Reading | `POST` | `/api/meter-readings` |
| Meter Reading | `GET` | `/api/rooms/{roomId}/meter-readings` |
| Meter Reading | `GET` | `/api/rooms/{roomId}/latest-meter-reading` |
| Meter Reading | `PUT` | `/api/meter-readings/{id}` |
| Meter Reading | `DELETE` | `/api/meter-readings/{id}` |
| Invoice | `GET` | `/api/invoices` |
| Invoice | `POST` | `/api/invoices/generate` |
| Invoice | `POST` | `/api/invoices/generate-monthly` |
| Invoice | `GET` | `/api/invoices/{id}` |
| Invoice | `PUT` | `/api/invoices/{id}` |
| Invoice | `DELETE` | `/api/invoices/{id}` |
| Invoice | `PATCH` | `/api/invoices/{id}/mark-paid` |
| Invoice | `PATCH` | `/api/invoices/{id}/cancel` |
| Invoice | `GET` | `/api/rooms/{roomId}/invoices` |
| Payment | `POST` | `/api/invoices/{invoiceId}/payment-link` |
| Payment | `POST` | `/api/webhooks/payos` |
| Payment | `GET` | `/api/payments/{id}` |
| Payment | `GET` | `/api/invoices/{invoiceId}/payments` |
| Dashboard | `GET` | `/api/dashboard/summary` |
| Dashboard | `GET` | `/api/dashboard/revenue?month=&year=` |
| Dashboard | `GET` | `/api/dashboard/debts` |
| Dashboard | `GET` | `/api/dashboard/rooms-status` |
| Maintenance | `GET` | `/api/maintenance-requests?status=` |
| Maintenance | `POST` | `/api/maintenance-requests` |
| Maintenance | `GET` | `/api/maintenance-requests/{id}` |
| Maintenance | `PUT` | `/api/maintenance-requests/{id}` |
| Maintenance | `PATCH` | `/api/maintenance-requests/{id}/status` |
| Maintenance | `DELETE` | `/api/maintenance-requests/{id}` |
| Dev/Test | `GET` | `/api/test/success` |
| Dev/Test | `GET` | `/api/test/not-found` |
| Dev/Test | `GET` | `/api/test/bad-request` |
| Dev/Test | `GET` | `/api/test/internal-error` |

## Luong 1: Dang Nhap Va Lay Thong Tin User

Frontend can dang nhap truoc, luu `data.token`, sau do gan vao header `Authorization`.

### POST `/api/auth/login`

Auth: public.

Request:

```json
{
  "email": "owner@gmail.com",
  "password": "123456"
}
```

Response `data`:

```json
{
  "token": "<jwt>",
  "type": "Bearer",
  "email": "owner@gmail.com",
  "name": "Chu Tro",
  "role": "OWNER"
}
```

### GET `/api/users/me`

Auth: required.

Response `data` la user hien tai. Field `passwordHash` duoc set `null` truoc khi tra ve.

## Luong 2: Tao Nha Tro Va Phong

Thu tu frontend nen di:

1. Tao property.
2. Tao room trong property.
3. Cap nhat gia dich vu cho property.
4. Cap nhat trang thai phong neu can.

### GET `/api/properties`

Auth: required.

Lay danh sach nha tro.

### POST `/api/properties`

Auth: required.

Request:

```json
{
  "name": "Nha tro A",
  "address": "123 Nguyen Trai, Ha Noi",
  "description": "Gan truong dai hoc"
}
```

Field:

| Field | Bat buoc | Ghi chu |
| --- | --- | --- |
| `name` | Co | Ten nha tro |
| `address` | Co | Dia chi |
| `description` | Khong | Mo ta |

### GET `/api/properties/{id}`

Auth: required.

Lay chi tiet property.

### PUT `/api/properties/{id}`

Auth: required.

Body giong `POST /api/properties`.

### DELETE `/api/properties/{id}`

Auth: required.

Xoa property. Nen xoa/cap nhat cac room lien quan truoc neu service bao loi rang property con phong.

### GET `/api/properties/{propertyId}/rooms`

Auth: required.

Lay danh sach phong cua mot property.

### POST `/api/properties/{propertyId}/rooms`

Auth: required.

Request:

```json
{
  "roomNumber": "A101",
  "floor": 1,
  "area": 25.5,
  "baseRent": 2500000,
  "deposit": 2500000,
  "maxTenants": 2,
  "status": "AVAILABLE",
  "note": "Phong co ban cong"
}
```

Field:

| Field | Bat buoc | Ghi chu |
| --- | --- | --- |
| `roomNumber` | Co | Ma/so phong |
| `floor` | Khong | `>= 0` |
| `area` | Khong | `>= 0` |
| `baseRent` | Khong | `>= 0` |
| `deposit` | Khong | `>= 0` |
| `maxTenants` | Khong | `> 0` |
| `status` | Khong | `AVAILABLE`, `OCCUPIED`, `RESERVED`, `MAINTENANCE` |
| `note` | Khong | Ghi chu |

### GET `/api/rooms/{id}`

Auth: required.

Lay chi tiet phong.

### PUT `/api/rooms/{id}`

Auth: required.

Body giong `POST /api/properties/{propertyId}/rooms`, nhung khong can `propertyId` trong body.

### PATCH `/api/rooms/{id}/status`

Auth: required.

Request:

```json
{
  "status": "MAINTENANCE"
}
```

### DELETE `/api/rooms/{id}`

Auth: required.

Xoa phong. Neu phong dang co hop dong/du lieu lien quan, service co the chan xoa.

## Luong 3: Quan Ly Khach Thue Va Hop Dong

Thu tu frontend nen di:

1. Tao tenant.
2. Chon room `AVAILABLE`.
3. Tao contract voi `roomId` va danh sach `tenantIds`.
4. Backend cap nhat phong thanh `OCCUPIED` va tenant gan vao phong.
5. Khi ket thuc, goi terminate contract.

### GET `/api/tenants?keyword=&status=`

Auth: required.

Query param:

| Param | Bat buoc | Ghi chu |
| --- | --- | --- |
| `keyword` | Khong | Tim theo tu khoa |
| `status` | Khong | `ACTIVE` hoac `LEFT` |

Vi du:

```http
GET /api/tenants?keyword=nguyen&status=ACTIVE
```

### POST `/api/tenants`

Auth: required.

Request:

```json
{
  "fullName": "Nguyen Van A",
  "phone": "0909123456",
  "email": "a@example.com",
  "identityNumber": "001234567890",
  "dateOfBirth": "2001-01-15",
  "permanentAddress": "Ha Noi",
  "currentRoomId": null,
  "status": "ACTIVE",
  "note": "Khach moi"
}
```

Field:

| Field | Bat buoc | Ghi chu |
| --- | --- | --- |
| `fullName` | Co | Ho ten |
| `phone` | Co | So dien thoai |
| `email` | Khong | Phai dung format email neu co |
| `identityNumber` | Khong | CCCD/CMND |
| `dateOfBirth` | Khong | Format `yyyy-MM-dd` |
| `permanentAddress` | Khong | Dia chi thuong tru |
| `currentRoomId` | Khong | Co the de `null`, contract se gan phong sau |
| `status` | Khong | `ACTIVE`, `LEFT` |
| `note` | Khong | Ghi chu |

### GET `/api/tenants/{id}`

Auth: required.

Lay chi tiet tenant.

### PUT `/api/tenants/{id}`

Auth: required.

Body giong `POST /api/tenants`.

### DELETE `/api/tenants/{id}`

Auth: required.

API nay khong hard delete ngay, ma danh dau tenant thanh `LEFT`.

### GET `/api/rooms/{roomId}/tenants`

Auth: required.

Lay danh sach tenant dang gan voi phong.

### GET `/api/contracts`

Auth: required.

Lay danh sach hop dong.

### POST `/api/contracts`

Auth: required.

Request:

```json
{
  "roomId": "<roomId>",
  "tenantIds": ["<tenantId1>", "<tenantId2>"],
  "startDate": "2026-06-01",
  "endDate": "2027-06-01",
  "monthlyRent": 2500000,
  "deposit": 2500000,
  "paymentDueDay": 5,
  "note": "Thanh toan truoc ngay 5 hang thang"
}
```

Field:

| Field | Bat buoc | Ghi chu |
| --- | --- | --- |
| `roomId` | Co | Phong can thue |
| `tenantIds` | Co | List tenant, khong duoc rong |
| `startDate` | Co | Format `yyyy-MM-dd` |
| `endDate` | Co | Format `yyyy-MM-dd` |
| `monthlyRent` | Co | `>= 0` |
| `deposit` | Co | `>= 0` |
| `paymentDueDay` | Co | Tu `1` den `31` |
| `note` | Khong | Ghi chu |

### GET `/api/contracts/{id}`

Auth: required.

Lay chi tiet hop dong.

### PUT `/api/contracts/{id}`

Auth: required.

Request:

```json
{
  "startDate": "2026-06-01",
  "endDate": "2027-06-01",
  "monthlyRent": 2600000,
  "deposit": 2500000,
  "paymentDueDay": 5,
  "note": "Gia moi"
}
```

Tat ca field trong request update hop dong hien dang duoc validate bat buoc, tru `note`.

### PATCH `/api/contracts/{id}/terminate`

Auth: required.

Request co the bo trong. Neu gui body:

```json
{
  "roomStatus": "AVAILABLE",
  "note": "Khach tra phong"
}
```

`roomStatus` nen la `AVAILABLE` hoac `MAINTENANCE` tuy theo tinh trang sau khi tra phong.

### PATCH `/api/contracts/{id}/renew`

Auth: required.

Request:

```json
{
  "newEndDate": "2028-06-01",
  "monthlyRent": 2700000,
  "deposit": 2500000,
  "paymentDueDay": 5,
  "note": "Gia han hop dong"
}
```

Field `newEndDate` bat buoc. Cac field tien/ngay thanh toan co the bo qua de giu gia tri cu.

## Luong 4: Cau Hinh Gia Dich Vu Va Ghi So Dien Nuoc

Hoa don chi tao duoc khi da co:

1. Room co active contract.
2. Service price cua property.
3. Meter reading dung thang/nam.
4. Chua co invoice trung room + month + year.

### GET `/api/properties/{propertyId}/service-prices`

Auth: required.

Lay cau hinh gia dich vu cua property.

### PUT `/api/properties/{propertyId}/service-prices`

Auth: required.

Request:

```json
{
  "electricityPrice": 4000,
  "waterPrice": 20000,
  "wifiFee": 100000,
  "garbageFee": 30000,
  "parkingFee": 150000
}
```

Tat ca field bat buoc va phai `>= 0`.

### GET `/api/meter-readings`

Auth: required.

Lay tat ca chi so dien nuoc.

### POST `/api/meter-readings`

Auth: required.

Request:

```json
{
  "roomId": "<roomId>",
  "month": 6,
  "year": 2026,
  "electricityOld": 100,
  "electricityNew": 180,
  "waterOld": 20,
  "waterNew": 28,
  "note": "Ghi so thang 6"
}
```

Rule:

- `month` tu `1` den `12`.
- `year` tu `2000` den `2100`.
- Chi so moi phai lon hon hoac bang chi so cu.
- Khong duoc tao trung `roomId + month + year`.

### GET `/api/rooms/{roomId}/meter-readings`

Auth: required.

Lay lich su chi so cua phong, sap xep moi truoc.

### GET `/api/rooms/{roomId}/latest-meter-reading`

Auth: required.

Lay chi so moi nhat cua phong.

### PUT `/api/meter-readings/{id}`

Auth: required.

Body giong `POST /api/meter-readings` nhung khong co `roomId`.

Khong the sua neu invoice cua room + month + year da `PAID`.

### DELETE `/api/meter-readings/{id}`

Auth: required.

Khong the xoa neu invoice cua room + month + year da `PAID`.

## Luong 5: Tao Hoa Don

Thu tu frontend nen di:

1. Lay room dang `OCCUPIED`.
2. Dam bao service price da cau hinh.
3. Tao meter reading cho thang.
4. Goi generate invoice.
5. Tao payment link neu khach thanh toan online.

### GET `/api/invoices`

Auth: required.

Lay tat ca hoa don.

### POST `/api/invoices/generate`

Auth: required.

Tao hoa don cho mot phong/thang.

Request:

```json
{
  "roomId": "<roomId>",
  "month": 6,
  "year": 2026,
  "otherFees": 50000,
  "discountAmount": 0,
  "note": "Hoa don thang 6"
}
```

Rule:

- Can active contract cua phong.
- Can service price cua property.
- Can meter reading cung thang/nam.
- Khong duoc tao trung invoice cho cung room + month + year.
- `totalAmount = rent + electricity + water + wifi + garbage + parking + otherFees - discountAmount`.
- `dueDate` lay tu `paymentDueDay` cua contract.

### POST `/api/invoices/generate-monthly`

Auth: required.

Tao hoa don hang loat cho cac phong `OCCUPIED` trong mot property.

Request:

```json
{
  "propertyId": "<propertyId>",
  "month": 6,
  "year": 2026
}
```

Response `data`:

```json
{
  "createdInvoices": [],
  "skippedRooms": [],
  "errors": []
}
```

### GET `/api/invoices/{id}`

Auth: required.

Lay chi tiet hoa don.

### PUT `/api/invoices/{id}`

Auth: required.

Request:

```json
{
  "otherFees": 100000,
  "discountAmount": 50000,
  "dueDate": "2026-06-05",
  "note": "Dieu chinh phi phu"
}
```

Chi sua duoc invoice chua `PAID` va chua `CANCELLED`.

### DELETE `/api/invoices/{id}`

Auth: required.

Xoa invoice. Invoice da `PAID` khong duoc xoa.

### PATCH `/api/invoices/{id}/mark-paid`

Auth: required.

Danh dau invoice da thanh toan thu cong. Luong PayOS nen de webhook cap nhat `PAID`.

### PATCH `/api/invoices/{id}/cancel`

Auth: required.

Huy invoice. Invoice da `PAID` khong duoc huy.

### GET `/api/rooms/{roomId}/invoices`

Auth: required.

Lay danh sach hoa don cua mot phong.

## Luong 6: Thanh Toan PayOS

Thu tu frontend nen di:

1. Tao invoice.
2. Goi `POST /api/invoices/{invoiceId}/payment-link`.
3. Mo `checkoutUrl` hoac hien `qrCode`.
4. PayOS goi webhook ve backend.
5. Backend phat su kien WebSocket `PAYMENT_UPDATED` de frontend cap nhat trang thai ngay.
6. Khi WebSocket ket noi lai, frontend goi lai API invoice/dashboard de dong bo cac su kien co the bi bo lo.

### POST `/api/invoices/{invoiceId}/payment-link`

Auth: required.

Khong can body.

Response `data`:

```json
{
  "paymentId": "<paymentId>",
  "invoiceId": "<invoiceId>",
  "orderCode": 1781695257807,
  "amount": 3035000,
  "status": "PENDING",
  "checkoutUrl": "https://pay.payos.vn/...",
  "qrCode": "000201..."
}
```

Rule:

- Invoice da `PAID` khong tao link moi.
- Invoice da `CANCELLED` khong tao payment.
- Invoice `totalAmount` phai lon hon `0`.
- Neu invoice da co payment `PENDING`, API tra lai payment link cu thay vi tao moi.

### POST `/api/webhooks/payos`

Auth: public, nhung co verify signature.

API nay de PayOS goi ve backend, frontend khong nen goi truc tiep tru khi test local co signature dung.

Request PayOS gui ve co dang:

```json
{
  "code": "00",
  "desc": "success",
  "success": true,
  "data": {
    "orderCode": 1781695257807,
    "amount": 3035000,
    "code": "00",
    "desc": "success",
    "reference": "PAYOS_TRANSACTION_ID"
  },
  "signature": "<payos_signature>"
}
```

Rule:

- Sai signature tra loi.
- Sai amount so voi payment tra loi.
- Success code `00` se cap nhat payment thanh `PAID` va invoice thanh `PAID`.
- Webhook lap lai voi payment da `PAID` se tra lai payment hien tai.
- Webhook bi huy/that bai se cap nhat payment thanh `CANCELLED` hoac `FAILED`.

### GET `/api/payments/{id}`

Auth: required.

Lay chi tiet payment.

### GET `/api/invoices/{invoiceId}/payments`

Auth: required.

Lay danh sach payment cua invoice.

### WebSocket `/ws/realtime`

Dung cho Expo Web, Android va iOS nhan trang thai thanh toan theo thoi gian thuc.

Frontend ket noi bang:

```text
ws://localhost:8080/ws/realtime
```

Khi backend dung HTTPS, su dung `wss://`.

Ngay sau khi ket noi, frontend phai gui JWT:

```json
{
  "type": "AUTH",
  "token": "<jwt>"
}
```

Backend chi dang ky session sau khi JWT hop le va tra:

```json
{
  "type": "AUTHENTICATED"
}
```

Sau khi PayOS webhook da xac thuc va luu payment/invoice, backend phat:

```json
{
  "type": "PAYMENT_UPDATED",
  "occurredAt": "2026-06-24T12:00:00",
  "data": {
    "paymentId": "<paymentId>",
    "invoiceId": "<invoiceId>",
    "paymentStatus": "PAID",
    "invoiceStatus": "PAID",
    "paidAt": "2026-06-24T12:00:00"
  }
}
```

`paymentStatus` co the la `PAID`, `FAILED` hoac `CANCELLED`. Webhook sai chu ky khong cap nhat database va khong phat su kien. Origin WebSocket phai khop `CORS_ALLOWED_ORIGIN_PATTERNS`.

## Luong 7: Dashboard Va Bao Cao

Frontend dashboard co the goi cac API nay sau khi dang nhap.

### GET `/api/dashboard/summary`

Auth: required.

Response `data` gom:

```json
{
  "totalRooms": 10,
  "occupiedRooms": 6,
  "availableRooms": 3,
  "maintenanceRooms": 1,
  "monthlyExpectedRevenue": 30000000,
  "monthlyPaidRevenue": 20000000,
  "monthlyUnpaidRevenue": 10000000,
  "unpaidInvoices": 4,
  "pendingMaintenanceRequests": 2
}
```

### GET `/api/dashboard/revenue?month=6&year=2026`

Auth: required.

`month` va `year` optional. Neu khong gui, service dung ky hien tai.

Response `data` gom:

```json
{
  "month": 6,
  "year": 2026,
  "expectedRevenue": 30000000,
  "paidRevenue": 20000000,
  "unpaidRevenue": 10000000,
  "invoiceCount": 10,
  "paidInvoices": 6,
  "unpaidInvoices": 4
}
```

### GET `/api/dashboard/debts`

Auth: required.

Response `data` gom tong cong no va danh sach invoice chua thanh toan:

```json
{
  "totalDebt": 10000000,
  "debtInvoiceCount": 4,
  "invoices": []
}
```

### GET `/api/dashboard/rooms-status`

Auth: required.

Response `data` gom:

```json
{
  "totalRooms": 10,
  "availableRooms": 3,
  "occupiedRooms": 6,
  "reservedRooms": 0,
  "maintenanceRooms": 1
}
```

## Luong 8: Bao Tri/Sua Chua

Thu tu frontend nen di:

1. Tao maintenance request cho room, co the gan tenant.
2. Loc theo status tren man hinh danh sach.
3. Cap nhat noi dung/priority neu can.
4. Cap nhat status den `DONE` khi hoan thanh.

### GET `/api/maintenance-requests?status=`

Auth: required.

Query param:

| Param | Bat buoc | Ghi chu |
| --- | --- | --- |
| `status` | Khong | `PENDING`, `IN_PROGRESS`, `DONE`, `CANCELLED` |

### POST `/api/maintenance-requests`

Auth: required.

Request:

```json
{
  "roomId": "<roomId>",
  "tenantId": "<tenantId>",
  "title": "Sua den phong A101",
  "description": "Den phong tam bi hong",
  "priority": "MEDIUM"
}
```

Field:

| Field | Bat buoc | Ghi chu |
| --- | --- | --- |
| `roomId` | Co | Phong can bao tri |
| `tenantId` | Khong | Tenant bao loi |
| `title` | Co | Tieu de |
| `description` | Khong | Mo ta |
| `priority` | Co | `LOW`, `MEDIUM`, `HIGH` |

### GET `/api/maintenance-requests/{id}`

Auth: required.

Lay chi tiet yeu cau bao tri.

### PUT `/api/maintenance-requests/{id}`

Auth: required.

Request:

```json
{
  "tenantId": "<tenantId>",
  "title": "Sua den phong A101",
  "description": "Can thay bong moi",
  "priority": "HIGH"
}
```

### PATCH `/api/maintenance-requests/{id}/status`

Auth: required.

Request:

```json
{
  "status": "IN_PROGRESS"
}
```

### DELETE `/api/maintenance-requests/{id}`

Auth: required.

Xoa request bao tri.

## Luong 9: Dev/Test Endpoints

Dung de test wrapper response va exception handler.

| Method | Path | Ket qua |
| --- | --- | --- |
| `GET` | `/api/test/success` | Tra success response |
| `GET` | `/api/test/not-found` | Nem `ResourceNotFoundException` |
| `GET` | `/api/test/bad-request` | Nem `BadRequestException` |
| `GET` | `/api/test/internal-error` | Nem runtime exception |

## Enums

| Enum | Gia tri |
| --- | --- |
| `UserRole` | `OWNER` |
| `RoomStatus` | `AVAILABLE`, `OCCUPIED`, `RESERVED`, `MAINTENANCE` |
| `TenantStatus` | `ACTIVE`, `LEFT` |
| `ContractStatus` | `ACTIVE`, `EXPIRED`, `TERMINATED`, `PENDING` |
| `InvoiceStatus` | `UNPAID`, `PAID`, `PARTIAL`, `OVERDUE`, `CANCELLED` |
| `PaymentProvider` | `PAYOS` |
| `PaymentStatus` | `PENDING`, `PAID`, `FAILED`, `CANCELLED` |
| `MaintenancePriority` | `LOW`, `MEDIUM`, `HIGH` |
| `MaintenanceStatus` | `PENDING`, `IN_PROGRESS`, `DONE`, `CANCELLED` |

## Luong Frontend De Xay App Quan Ly Tro

Luong toi thieu de frontend tich hop het backend:

1. Dang nhap: `POST /api/auth/login`, luu token.
2. Tai dashboard: goi `/api/dashboard/summary`, `/api/dashboard/revenue`, `/api/dashboard/debts`, `/api/dashboard/rooms-status`.
3. Quan ly co so: CRUD `/api/properties`, CRUD room theo property.
4. Cau hinh thu phi: `PUT /api/properties/{propertyId}/service-prices`.
5. Quan ly khach: CRUD `/api/tenants`, xem tenant theo room.
6. Lap hop dong: `POST /api/contracts`, terminate/renew khi can.
7. Hang thang: tao meter reading, tao invoice don le hoac hang loat.
8. Thu tien: tao PayOS payment link, hien `checkoutUrl`/`qrCode`, nhan `PAYMENT_UPDATED` qua WebSocket.
9. Bao tri: tao request, loc theo status, cap nhat trang thai xu ly.
