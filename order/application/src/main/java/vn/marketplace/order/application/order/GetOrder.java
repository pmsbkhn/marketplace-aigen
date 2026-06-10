package vn.marketplace.order.application.order;

/** Use-case port: read one order's detail, tenant-scoped to the caller. Read-only. */
public interface GetOrder {
    OrderView execute(GetOrderCmd cmd);
}
