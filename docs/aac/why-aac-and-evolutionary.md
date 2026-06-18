# Vì sao AaC + Evolutionary Architecture — bài toán kiểm soát hệ thống

Ghi chú ngắn: AaC và Evolutionary Architecture giải quyết việc **kiểm soát hệ thống** thế
nào. Đây là phần "tại sao" ở tầng tổng; chi tiết cơ chế xem [training/](training/00-README.md)
và [adr/](adr/README.md).

## Vấn đề (quan trọng giảm dần)

1. **Trôi tài liệu (docs ↔ code lệch nhau).** Mất nguồn sự thật để kiểm soát → mọi quyết
   định dựa trên bản đồ sai.
2. **Xói mòn kiến trúc.** Từng commit lén phá layering/dependency; hệ thống thoái hoá âm
   thầm, ngày càng khó đổi.
3. **Vỡ tích hợp ở biên service.** Đổi event/API làm hỏng consumer — rủi ro lớn nhất của
   hệ phân tán.
4. **Không đo được "độ khoẻ" kiến trúc.** Không biết đang vi phạm gì, không có phán quyết
   pass/fail → không quản trị được.
5. **Mất dấu vết quyết định ("vì sao thế này").** Tranh luận lặp lại; quyết định sau mâu
   thuẫn quyết định trước.
6. **Stakeholder thiếu góc nhìn phù hợp.** PO/dev/SRE/security nhìn cùng một mớ → hiểu sai,
   quyết định sai.
7. **Tri thức nằm trong đầu người.** Onboarding chậm; mất người là mất hiểu biết hệ thống.

## Giải pháp (giải vấn đề nào)

- **AaC: một model — nhiều view** (Structurizr/C4). → Giải **#6** (mỗi stakeholder một
  viewpoint nhất quán, không mâu thuẫn) và **#7** (đọc text + render, onboarding nhanh).
  Nền tảng cho mọi thứ còn lại.
- **AaC versioned + CI `validate/export`** (`.github/workflows/aac.yml`). → Giải **#1**:
  docs là code, review qua PR, sai cú pháp/view là đỏ → tài liệu không trôi *về mặt chính nó*.
- **Fitness functions** (ArchUnit/ea-archrules, `*/architecture/FitnessFunctionsTest.java`).
  → Giải **#2**: luật cấu trúc (domain-pure, publish-via-outbox, naming…) thành test; phá
  luật là **build đỏ** → chống xói mòn tự động, liên tục.
- **Contract tests ở biên** (event `contracts/*.json` + producer/consumer test; REST khi
  cần — xem [ADR-0004](adr/0004-rest-standins-for-sync-integration.md)). → Giải **#3**:
  producer/consumer bị trói vào hợp đồng; đổi phá vỡ là đỏ trước khi merge.
- **Governance plane / EA scorecard verdict**
  ([ADR-0001](adr/0001-observability-governance-plane-split.md)). → Giải **#4**: biến tuân
  thủ thành **phán quyết đo được** (pass/warn/fail/waived), tách khỏi metric vận hành.
- **ADR** (MADR, versioned — [adr/](adr/README.md)). → Giải **#5**: mỗi quyết định + lý do
  được lưu, đánh số, `Supersedes` khi đổi → dấu vết bất biến.
- **Quy trình same-PR + CODEOWNERS + branch protection**
  ([training Ch.9](training/09-quy-trinh-va-governance.md)). → Khép vòng **#1–#3**: đổi code
  thì đổi docs cùng PR, đúng role duyệt đúng vùng.

## Ý cốt lõi

> **AaC** giữ cho bức tranh kiến trúc *trung thực và đa góc nhìn*; **Evolutionary
> Architecture** (fitness functions + contract tests + verdict) giữ cho *code không trượt
> khỏi bức tranh đó theo thời gian*. AaC **mô tả**, EA **cưỡng chế** — hai nửa của cùng một
> bài toán kiểm soát: **luôn biết hệ thống đang thế nào, và an toàn để tiếp tục thay đổi nó.**
