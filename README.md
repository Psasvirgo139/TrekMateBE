# 🏔️ TrekMate Backend

> **REST API for TrekMate – Trekking Tour Guide Platform**

TrekMate là nền tảng hướng dẫn du lịch trekking, cung cấp RESTful API để quản lý tour, lịch khởi hành, đặt chỗ, hướng dẫn viên, thiết bị thuê và nhiều tính năng khác.

---

## 📋 Mục lục

- [Công nghệ sử dụng](#-công-nghệ-sử-dụng)
- [Kiến trúc dự án](#-kiến-trúc-dự-án)
- [Yêu cầu hệ thống](#-yêu-cầu-hệ-thống)
- [Cài đặt & Chạy dự án](#-cài-đặt--chạy-dự-án)
- [Cấu hình môi trường](#-cấu-hình-môi-trường)
- [Cơ sở dữ liệu](#-cơ-sở-dữ-liệu)
- [API Documentation](#-api-documentation)
- [Tính năng chính](#-tính-năng-chính)
- [Cấu trúc thư mục](#-cấu-trúc-thư-mục)

---

## 🛠️ Công nghệ sử dụng

| Công nghệ | Phiên bản | Mô tả |
|---|---|---|
| **Java** | 21 | Ngôn ngữ lập trình chính |
| **Spring Boot** | 3.3.5 | Framework backend |
| **Spring Security** | - | Xác thực & phân quyền |
| **Spring Data JPA** | - | ORM & truy vấn database |
| **PostgreSQL** | - | Cơ sở dữ liệu quan hệ |
| **Hibernate** | - | JPA implementation |
| **Lombok** | - | Giảm boilerplate code |
| **MapStruct** | 1.6.2 | Ánh xạ DTO ↔ Entity |
| **Springdoc OpenAPI** | 2.6.0 | Swagger UI tự động |
| **HikariCP** | - | Connection pooling |
| **Maven** | - | Build & dependency management |

---

## 🏗️ Kiến trúc dự án

Dự án theo kiến trúc **Layered Architecture** chuẩn Spring Boot:

```
Client (Mobile/Web App)
        │
        ▼
  Controller Layer   ← Nhận HTTP request, trả về response
        │
        ▼
  Service Layer      ← Business logic
        │
        ▼
  Repository Layer   ← Truy vấn database (Spring Data JPA)
        │
        ▼
  PostgreSQL Database
```

**Các thành phần bổ trợ:**
- **DTO**: Chuyển đổi dữ liệu giữa các tầng
- **Mapper (MapStruct)**: Ánh xạ Entity ↔ DTO tự động
- **Security**: Bảo vệ các endpoint với Spring Security
- **Exception Handler**: Xử lý lỗi tập trung
- **Config**: Cấu hình OpenAPI, CORS, JPA Auditing...

---

## 💻 Yêu cầu hệ thống

- **Java 21** trở lên
- **Maven 3.8+**
- **PostgreSQL 14+**
- (Tuỳ chọn) **Docker** để chạy database dễ hơn

---

## 🚀 Cài đặt & Chạy dự án

### 1. Clone repository

```bash
git clone https://github.com/Psasvirgo139/TrekMateBE.git
cd TrekMateBE
```

### 2. Tạo database PostgreSQL

```sql
CREATE DATABASE trekmate_db;
```

Hoặc dùng Docker:

```bash
docker run -d \
  --name trekmate-postgres \
  -e POSTGRES_DB=trekmate_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=your_password \
  -p 5432:5432 \
  postgres:16
```

### 3. Cấu hình môi trường

Sao chép file cấu hình mẫu:

```bash
cp .env.example .env
```

Chỉnh sửa file `.env` với thông tin database của bạn (xem [Cấu hình môi trường](#-cấu-hình-môi-trường)).

### 4. Chạy ứng dụng

**Chế độ Development (mặc định):**

```bash
mvn spring-boot:run
```

**Hoặc build và chạy JAR:**

```bash
mvn clean package -DskipTests
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

Ứng dụng sẽ khởi động tại: `http://localhost:8080/api`

---

## ⚙️ Cấu hình môi trường

### File `.env` (từ `.env.example`)

```env
# Database
DB_URL=jdbc:postgresql://localhost:5432/trekmate_db
DB_USERNAME=postgres
DB_PASSWORD=your_password_here

# JWT (sẽ dùng sau)
# JWT_SECRET=your_jwt_secret_key_at_least_256_bits
# JWT_EXPIRATION=86400000
```

### Profiles Spring Boot

| Profile | Dùng khi | File config |
|---|---|---|
| `dev` (mặc định) | Phát triển local | `application-dev.yml` |
| `prod` | Deploy production | `application-prod.yml` |

Để đổi profile, sửa trong `application.yml`:
```yaml
spring:
  profiles:
    active: prod  # hoặc dev
```

Hoặc truyền qua command line:
```bash
java -jar app.jar --spring.profiles.active=prod
```

---

## 🗄️ Cơ sở dữ liệu

### Sơ đồ Entity chính

```
User ──────────────────────────────────────────────┐
 │                                                  │
 │ (đặt tour)                                       │ (viết review)
 ▼                                                  ▼
Booking ──────────────── TourDeparture ─────── Review
 │                              │
 │ (thuê thiết bị)              │ (thuê guide)
 ▼                              ▼
EquipmentRental            DepartureGuide
 │                              │
 ▼                              ▼
Equipment                    Guide
 │
 ▼
EquipmentCategory

Tour ──────────────────────────────────────────────────┐
 ├─── TourImage                                        │
 ├─── TourWaypoint ──── ItineraryWaypoint              │
 ├─── TourDailyItinerary                               │
 └─── TourDeparture ────────────────────────────────────┘
                   └─── DepartureWeatherDaily
```

### Các Entity chính

| Entity | Mô tả |
|---|---|
| `User` | Người dùng hệ thống |
| `Role` / `UserRole` | Phân quyền người dùng |
| `Tour` | Thông tin tour trekking |
| `TourDeparture` | Lịch khởi hành cụ thể của một tour |
| `TourWaypoint` | Các điểm waypoint trên lộ trình |
| `TourDailyItinerary` | Lịch trình theo từng ngày |
| `TourImage` | Hình ảnh của tour |
| `Booking` | Đặt chỗ của khách hàng |
| `Customer` | Thông tin hồ sơ khách hàng |
| `Guide` | Hướng dẫn viên |
| `DepartureGuide` | Phân công HDV cho lịch khởi hành |
| `GuideRating` | Đánh giá hướng dẫn viên |
| `Equipment` | Thiết bị trekking có thể thuê |
| `EquipmentCategory` | Danh mục thiết bị |
| `EquipmentRental` | Chi tiết thuê thiết bị trong booking |
| `Review` | Đánh giá & nhận xét của khách hàng |
| `ReviewHelpful` | Đánh dấu review hữu ích |
| `Payment` | Thông tin thanh toán |
| `Notification` | Thông báo cho người dùng |
| `Wishlist` | Danh sách yêu thích của khách hàng |
| `DepartureWeatherDaily` | Dữ liệu thời tiết theo ngày cho lịch khởi hành |
| `ItineraryWaypoint` | Waypoint theo ngày của lộ trình |

### Import dữ liệu mẫu

Trong thư mục gốc có sẵn các file SQL để import:

```bash
# Schema + dữ liệu chính
psql -U postgres -d trekmate_db -f database_22_5.sql

# Các bản vá/sửa đổi
psql -U postgres -d trekmate_db -f database_fixes.sql

# Dữ liệu waypoints
psql -U postgres -d trekmate_db -f database_waypoints.sql
```

> **Lưu ý:** `DataInitializer.java` cũng tự động seed dữ liệu mẫu khi khởi động ứng dụng ở chế độ `dev`.

---

## 📖 API Documentation

Sau khi khởi động ứng dụng, truy cập Swagger UI tại:

```
http://localhost:8080/api/swagger-ui.html
```

Hoặc xem OpenAPI JSON spec:

```
http://localhost:8080/api/v3/api-docs
```

### Thông tin API

| Thuộc tính | Giá trị |
|---|---|
| **Base URL** | `http://localhost:8080/api` |
| **API Version** | `v1.0.0` |
| **Format** | JSON |
| **Timezone** | `Asia/Ho_Chi_Minh` |
| **Date Format** | `yyyy-MM-dd HH:mm:ss` |

---

## ✨ Tính năng chính

### 🗺️ Quản lý Tour
- Tạo, cập nhật, xoá tour trekking
- Quản lý lộ trình (waypoints) với toạ độ GPS
- Lịch trình theo từng ngày (daily itinerary)
- Thư viện ảnh tour
- Đa cấp độ khó: `EASY`, `MODERATE`, `HARD`, `EXPERT`
- Trạng thái tour: `DRAFT` → `PUBLISHED` → `ARCHIVED`

### 📅 Lịch Khởi hành (Departure)
- Quản lý các chuyến đi cụ thể theo ngày
- Theo dõi slot đã đặt / còn trống
- Thông tin thời tiết dự báo theo ngày
- Log sự cố và lệch lộ trình trong chuyến đi
- Hỗ trợ **join tour** (ghép đoàn)

### 🎫 Đặt chỗ (Booking)
- Đặt tour cho nhiều người tham gia
- Thuê thiết bị trekking kèm booking
- Mã booking duy nhất (booking code)
- Trạng thái: `PENDING` → `CONFIRMED` → `COMPLETED` / `CANCELLED`
- Snapshot giá tại thời điểm đặt

### 🧭 Hướng dẫn viên (Guide)
- Hồ sơ hướng dẫn viên
- Phân công guide cho từng lịch khởi hành
- Hệ thống đánh giá guide

### 🎒 Thiết bị thuê (Equipment)
- Danh mục thiết bị theo category
- Theo dõi tình trạng thiết bị
- Thuê thiết bị kèm theo booking

### ⭐ Đánh giá & Review
- Khách hàng để lại review sau chuyến đi
- Đánh dấu review hữu ích
- Tính điểm trung bình tự động cho tour

### 🔔 Thông báo (Notification)
- Gửi thông báo đến người dùng
- Theo dõi trạng thái đã đọc

### ❤️ Wishlist
- Lưu tour yêu thích

---

## 📁 Cấu trúc thư mục

```
backend/
├── src/
│   └── main/
│       ├── java/com/trekmate/backend/
│       │   ├── TrekmateApplication.java       # Entry point
│       │   ├── config/
│       │   │   ├── AppConfig.java             # Cấu hình chung (CORS, Auditing...)
│       │   │   ├── DataInitializer.java       # Seed dữ liệu mẫu
│       │   │   └── OpenApiConfig.java         # Cấu hình Swagger/OpenAPI
│       │   ├── controller/
│       │   │   └── HomeController.java        # API endpoint
│       │   ├── dto/                           # Data Transfer Objects
│       │   ├── exception/                     # Custom exceptions & handler
│       │   ├── listener/                      # JPA Entity listeners
│       │   ├── mapper/                        # MapStruct mappers
│       │   ├── model/
│       │   │   ├── enums/                     # Enum types
│       │   │   ├── embeddable/                # JPA Embeddable classes
│       │   │   ├── BaseEntity.java            # Base entity (id, timestamps)
│       │   │   ├── Tour.java
│       │   │   ├── TourDeparture.java
│       │   │   ├── Booking.java
│       │   │   └── ...                        # Các entity khác
│       │   ├── repository/                    # Spring Data JPA Repositories
│       │   ├── security/
│       │   │   └── SecurityConfig.java        # Cấu hình Spring Security
│       │   ├── service/
│       │   │   ├── HomeService.java
│       │   │   └── impl/                      # Service implementations
│       │   └── utils/                         # Utility classes
│       └── resources/
│           ├── application.yml                # Cấu hình chính
│           ├── application-dev.yml            # Cấu hình môi trường dev
│           ├── application-prod.yml           # Cấu hình môi trường prod
│           └── db/                            # Migration scripts
├── database_22_5.sql                          # Dữ liệu mẫu chính
├── database_fixes.sql                         # Bản vá database
├── database_waypoints.sql                     # Dữ liệu waypoints
├── .env.example                               # Mẫu biến môi trường
├── .gitignore
└── pom.xml                                    # Maven dependencies
```

---

## 🔧 Cấu hình HikariCP (Connection Pool)

| Tham số | Giá trị | Mô tả |
|---|---|---|
| `minimum-idle` | 5 | Số connection tối thiểu |
| `maximum-pool-size` | 20 | Số connection tối đa |
| `idle-timeout` | 300,000 ms (5 phút) | Timeout khi connection nhàn rỗi |
| `connection-timeout` | 20,000 ms (20 giây) | Timeout khi lấy connection |
| `max-lifetime` | 1,200,000 ms (20 phút) | Thời gian sống tối đa của connection |

---

## 📝 Ghi chú phát triển

- **DDL Auto**: Đang dùng `update` ở dev — chuyển sang `validate` hoặc `none` ở production
- **JSONB fields**: Một số trường (`highlights`, `includes`, `participants_info`, `incident_log`...) dùng kiểu `jsonb` của PostgreSQL để lưu dữ liệu linh hoạt
- **UUID**: Tất cả entity dùng `UUID` làm primary key
- **Auditing**: `BaseEntity` tự động ghi `createdAt` và `updatedAt` qua JPA Auditing

---

## 📬 Liên hệ

**TrekMate Team** – support@trekmate.com
