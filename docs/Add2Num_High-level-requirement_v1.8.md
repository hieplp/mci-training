# High level requirement — A2N: Develop a simple function

| | |
|---|---|
| **Process** | Recruitment |
| **Document code** | Add2Num |
| **Document name** | Project Add 2 numbers |
| **Create Date** | 1-Oct-17 |
| **Creator** | Thach.Le |
| **Update Date** | 11-Mar-26 |
| **Updater** | Thach.Le |

## 1. Introduction

Tình huống đặt ra là bạn được giao việc viết hàm cài đặt thuật toán cộng 2 số lớn (được biểu diễn dưới dạng chuỗi) với thuật toán như học sinh Tiểu học đã làm.

Hàm này sẽ được đóng gói để bàn giao cho một nhóm khác làm giao diện (hoặc ứng dụng dạng console) để họ gọi hàm của bạn trong dự án lớn hơn.

## 2. Input

- Yêu cầu trong mục 1.
- Các kiến thức, kỹ năng, tài liệu mà bạn đã học.

## 3. Preparation

- Bạn tự chuẩn bị công cụ và tài liệu lập trình tương ứng với Ngôn ngữ lập trình mà bạn chọn như GO, C, C++, Java, C#, Dart, Javascript, Typescript, Python, PHP,…
- Giấy và bút nếu cần và bạn thấy hữu ích.

## 4. Request

Viết phần lõi (core) trong 1 lớp riêng **"MyBigNumber"**, method **`String sum(String stn1, String stn2)`** để cài đặt thuật toán cộng 2 số giống như các học sinh Tiểu học thực hiện như sau:

- Duyệt đồng thời chuỗi `stn1`, `stn2` từ phải sang trái, lấy ra từng kí tự (character), chuyển thành kí số (digit).
- Cộng từng kí số.
- Ghi nhận lại lịch sử phép toán vừa thực hiện (Ưu tiên dùng **LOGGING**. Không biết logging thì dùng PRINT cũng tạm chấp nhận).
- …

Ví dụ lệnh `sum("1234", "897")` sẽ được thực hiện như sau:

- **Bước 1:** Lấy 4 cộng với 7 được 11. Lưu 1 vào kết quả và nhớ 1.
- **Bước 2:** Lấy 3 cộng với 9 được 12. Cộng tiếp với nhớ 1 được 13. Lưu 3 vào kết quả được kết quả mới là `"31"`. Ghi nhớ 1.
- Lặp lại các bước trên (nhớ lại học sinh lớp 3 đã cộng như thế nào thì lập trình tương tự như vậy)
- …

**Giả định:** giá trị tham số truyền vào hàm là đúng, chỉ chứa các kí số hợp lệ, không có kí tự nào khác. Vì vậy chưa cần xử lý lỗi dữ liệu.

## 5. Output — Sản phẩm nộp lại

- Project đã làm lưu trên GIT Server để chế độ công khai. Nộp lại link của GIT repository của sản phẩm.
- GIT server có thể dùng GITHUB.com, GITLAB.com hoặc máy chủ nào đó mà bạn đã quen.
- Hãy đặt phiên bản mà bạn hoàn thành để đánh giá là **0.0.1**: Có thể dùng **tag** hoặc **branch**.
- Project có thực hiện Unit Testing để kiểm thử source code chính ở trên nếu có. Trường hợp source code thực hiện Unit Testing được lưu chung trong project ở mục trên thì bỏ qua yêu cầu này.
  - Source code để trên GIT đủ để người khác lấy về và thực hiện lại được như tác giả đã làm.
  - Có hướng dẫn chi tiết trong file README.md để người khác biết cách biên dịch, chạy các Test Case (nếu có viết Unit Testing) như tác giả đã thực hiện.
  - Khuyến khích viết code Unit Testing trong project khác, hoặc thư mục khác với project/thư mục của mã nguồn chính.
- Sau khi làm xong hãy đóng vai trò là một người khác để clone source code về thư mục qui ước như dưới đây:

  Ví dụ dự án của bạn có URL trên github.com là `https://github.com/youraccount/projectname` thì hãy clone về thư mục có đường dẫn như sau:

  ```
  D:\Projects\github.com\youraccount\projectname
  ```

  **Giải thích:**

  - `D:\Projects` là thư mục do bạn chọn, có thể thay đổi ổ đĩa phù hợp với máy bạn. Dùng MacOS, Linux thì có thể thư mục là `~/Projects`
  - `github.com` là tên thư mục tương ứng với lại trang web của GIT Server. Tùy web site github.com, gitlab.com thì hãy tạo tương ứng.
  - `youraccount` là tên tài khoản của bạn. Hãy xem kỹ đường link của dự án để tạo cho đúng.
  - `projectname` là tên của dự án của bạn. Hãy xem kỹ đường link của dự án để tạo cho đúng.

  Ví dụ: Dự án `https://github.com/thachln/Hawkeye` sẽ được clone về thư mục `D:\Projects\github.com\thachln\Hawkeye` (nếu dùng Windows), `~/Projects/github.com/thachln/Hawkeye` (nếu dùng MacOS/Linux; `~` là thư mục các nhân của người dùng)
