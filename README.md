# tanh. — Ứng dụng mua sắm Android

Bài tập Lập trình di động — Java, XML, Material 3 và Firebase Firestore.

## Chức năng

- Xem sản phẩm theo danh mục và tìm tên sản phẩm có hoặc không dấu.
- Xem chi tiết sản phẩm và danh sách sản phẩm gợi ý.
- Chọn màu Đen/Trắng; quần áo size S/M, giày size 36/37.
- Thêm vào giỏ, tăng giảm số lượng, xóa và chọn sản phẩm cần mua.
- Nhập thông tin giao hàng, lưu yêu cầu đặt hàng COD chờ xác nhận vào Firestore.
- Cập nhật họ tên, email, số điện thoại trong Hồ sơ.

## Công nghệ

- Android Java và XML, Material Components.
- Android SDK 37; hỗ trợ từ Android API 24.
- Firebase Firestore và Firebase Authentication ẩn danh.
- Gradle Wrapper có sẵn trong repository.

## Chạy ứng dụng

1. Clone repository rồi mở thư mục project bằng Android Studio.
2. Cài Android SDK 37, dùng Gradle JDK tương thích với Android Gradle Plugin của project (có thể dùng JDK đi kèm Android Studio hiện tại).
3. Chờ Gradle Sync hoàn thành.
4. Chọn máy ảo hoặc thiết bị Android có Internet rồi bấm Run.

Build debug bằng terminal trên macOS/Linux:

```sh
./gradlew :app:assembleDebug
```

`local.properties`, cache, thư mục build và file ký ứng dụng không được đưa vào Git.

## Firebase và dữ liệu

Project dùng cấu hình Firebase Android tại `app/google-services.json`. Khi chuyển sang Firebase project khác, đăng ký ứng dụng với applicationId `com.example.a24162007_tuan1` và thay file cấu hình này.

- Bật Cloud Firestore và phương thức đăng nhập Anonymous trong Firebase Authentication.
- `products/{productId}` gồm `id` (integer), `name`, `description`, `image_name` (string) và `price` (integer, đơn vị VND).
- `image_name` khớp tên ảnh trong `app/src/main/res/drawable`.
- `carts/{uid}/items/{variantId}` lưu sản phẩm, màu, size và số lượng.
- `users/{uid}` lưu hồ sơ; `users/{uid}/orders/{orderId}` lưu yêu cầu đặt hàng.
- Firestore Rules và dữ liệu trên Firebase Console không tự được sao lưu bằng thao tác đẩy mã nguồn lên GitHub.

## Phạm vi hiện tại

Tên đăng nhập trên giao diện được lưu cục bộ, chưa phải tài khoản email/mật khẩu Firebase. Hồ sơ và giỏ hàng gắn với UID Firebase ẩn danh trên thiết bị. Ảnh đại diện được lưu tham chiếu trên thiết bị.

Màu và size là tùy chọn phục vụ bài tập; ảnh sản phẩm chưa thay đổi theo màu. Đơn COD được lưu với trạng thái `pending_confirmation`, chưa tích hợp cổng thanh toán online, quản lý tồn kho hay hệ thống giao vận. Cần xác minh đơn hàng và giá ở máy chủ tin cậy trước khi triển khai bán hàng công khai.
