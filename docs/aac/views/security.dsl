# ==============================================================================
# VIEWPOINT: Security
# Concerns: trust boundary, điểm tiếp nhận lưu lượng (ingress), API service-to-service,
#           dữ liệu nhạy cảm (tiền/PII/secret), hệ thống ngoài.
# View: C4 L2 Container, dùng TAG để tô đậm mối quan tâm bảo mật:
#   - Ingress       (viền cam): endpoint công khai nhận lưu lượng người dùng/đối tác
#   - InternalApi   (viền xanh): endpoint /internal/* — chỉ gọi trong mesh (kỳ vọng mTLS)
#   - Sensitive     (viền đỏ):  store/adapter xử lý tiền, PII, secret
#   - External      (xám):      hệ thống bên thứ ba
#   - Standin       (mờ/nét đứt): adapter giả lập, KHÔNG dùng cho production
# Ghi chú as-built: mTLS STRICT + authz policy (Istio) là Phase D — hiện CHƯA bật.
# Webhook gateway đã xác minh HMAC + chống replay.
# ==============================================================================

container marketplaceSystem "SecurityBoundaries" "Khung nhìn bảo mật: trust boundary, ingress, S2S API, dữ liệu nhạy cảm. Xem màu viền theo tag." {
    include *
    autoLayout lr
}
