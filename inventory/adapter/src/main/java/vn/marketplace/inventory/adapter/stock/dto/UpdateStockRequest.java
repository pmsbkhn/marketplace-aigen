package vn.marketplace.inventory.adapter.stock.dto;

/** Body for PUT /v1/merchant/stock/{sku}: the new absolute available quantity (≥ 0). */
public record UpdateStockRequest(int available) {
}
