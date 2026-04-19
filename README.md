# BookShopOnline - Hệ thống quản lý cửa hàng sách

BookShopOnline là một ứng dụng thương mại điện tử chuyên biệt dành cho nhà sách. Ứng dụng hỗ trợ quản lý kho, giỏ hàng, giao dịch, thanh toán qua cổng VNPAY và cung cấp tính năng tương tác khách hàng thời gian thực (real-time chat) thông qua WebSocket.

#Các tính năng chính (Features)

- **Quản lý Kho sách (Inventory)**: Lưu trữ thông tin sách, theo dõi số lượng tồn kho và lịch sử nhập/xuất.
- **Giỏ hàng & Thanh toán (Cart & Checkout)**: Quản lý giỏ hàng của người dùng, thanh toán trực tuyến bảo mật thông qua VNPAY.
- **Tương tác Thời gian thực (Real-time Chat)**: Kênh chat cộng đồng sử dụng WebSocket (STOMP/SockJS).
- **Import/Export Excel**: Hỗ trợ admin xuất và nhập danh sách sản phẩm nhanh chóng bằng Apache POI.
- **Phân quyền bảo mật (Security)**: Sử dụng Spring Security để kiểm soát quyền hạn (ADMIN / USER) và bảo vệ dữ liệu.
- **API**: Cung cấp các RESTful API chuẩn mực.

#Công nghệ sử dụng (Tech Stack)

- **Backend**: Java 21, Spring Boot 3.x, Spring Data JPA, Spring Security, Spring WebSocket.
- **Database**: MySQL.
- **Cổng thanh toán**: VNPAY.
- **Frontend**: HTML/CSS, JavaScript, Bootstrap 5, Thymeleaf.
- **Build tool**: Maven.

#Hướng dẫn cài đặt và chạy dự án (How to run)

#Yêu cầu hệ thống (Prerequisites)
1. **JDK 21** trở lên.
2. **MySQL Server** (đang chạy ở port mặc định 3306).
3. *(Tùy chọn)* RabbitMQ nếu muốn thiết lập Message Broker nâng cao cho WebSocket.

#Các bước cài đặt

**Bước 1: Clone dự án**
```bash
git clone <đường-dẫn-repo-của-bạn>
cd BookShopOnline
```

**Bước 2: Cài đặt Database**
1. Mở MySQL Console hoặc công cụ quản lý DB (như phpMyAdmin, DBeaver, MySQL Workbench).
2. Tạo một database mới tên là `bookstore`:
```sql
CREATE DATABASE bookstore CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

**Bước 3: Cấu hình kết nối Database**
Mở file `src/main/resources/application.properties` và chỉnh sửa lại tài khoản/mật khẩu MySQL cho phù hợp với máy của bạn:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/bookstore
spring.datasource.username=root
spring.datasource.password=mat_khau_cua_ban
```
*(Lưu ý: Bảng và dữ liệu khởi tạo Role sẽ được Hibernate tự động tạo (auto-ddl) khi dự án chạy lần đầu).*

**Bước 4: Build và chạy dự án**

Dự án đã tích hợp sẵn Maven Wrapper, bạn không cần cài Maven thủ công.

- **Trên Windows:**
```cmd
.\mvnw.cmd clean compile
.\mvnw.cmd spring-boot:run
```

- **Trên macOS/Linux:**
```bash
./mvnw clean compile
./mvnw spring-boot:run
```

**Bước 5: Truy cập ứng dụng**
Sau khi ứng dụng khởi động thành công, mở trình duyệt và truy cập:
👉 **[http://localhost:8081](http://localhost:8081)**

---

#Kiểm thử và Đăng nhập
- Ở lần chạy đầu tiên, hệ thống sẽ tự động tạo các Role cần thiết (`ADMIN`, `USER`). 
- Bạn có thể vào trang `/login` để tạo tài khoản mới và dùng tài khoản đó trải nghiệm các chức năng User.
- Để vào trang quản trị viên, hãy set cứng quyền Admin cho tài khoản của bạn trực tiếp trong Database (bảng User-Role) nếu chưa có giao diện gán quyền.

## 🐛 Khắc phục sự cố thường gặp
- **Lỗi `Could not obtain connection`**: Do MySQL chưa chạy hoặc sai thông tin đăng nhập trong `application.properties`.
- **Thiếu BOM chặn việc compile file Java**: Dự án hiện đã được làm sạch ký tự BOM ẩn, nên bạn hoàn toàn yên tâm build.
