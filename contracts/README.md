# JSON Event Contracts

The committed wire envelopes of every cross-service JSON event — what Schema Registry does for
Avro, done with fixtures (msfw `JsonEventContract`, see msfw docs/USAGE.md §4).

- **Producers** own the files: each service's `*ContractTest` (in its domain module) serializes a
  representative real domain event through the production serializer and (re)writes
  `<Source>.<Type>.json` here. The envelope is deterministic — **a git diff on a fixture IS the
  contract-change signal**; review it like an API change.
- **Consumers** pin themselves to the files: each service's `*ContractBindingTest` (in its adapter
  module) binds its payload DTO from the producer's fixture with consumption-pipeline semantics
  and asserts the values. A producer rename/removal breaks the consumer's build, not its runtime.

| Fixture | Producer | Consumers binding it |
|---|---|---|
| `Catalog.ProductCreated.json` | catalog | inventory |
| `Order.OrderCompleted.json` | order | payment, inventory |
| `Order.OrderCancelled.json` | order | (inventory release / notification — bind when wired) |
| `Order.OrderPendingTimedOut.json` | order | order (self — delayed FR13 timer) |
| `Payment.PaymentReceived.json` | payment | order, notification |
| `Payment.PaymentFailed.json` | payment | order |
| `Payment.PayoutCompleted.json` | payment | (notification `payout_completed_merchant` — bind when wired) |

Changing a payload: change the domain event, run the producer's contract test, commit the new
fixture **together with** the consumer-side DTO/test updates the diff forces you to look at.

## Synchronous HTTP Contracts (`http/`)

The same mechanism for the **synchronous** cross-service calls Checkout makes — the REST stand-ins
for the SAD's gRPC calls (`Catalog.GetPrice`, `Inventory.ReserveStock`, `Order.CreatePendingOrder`,
`Payment.InitEscrow`). The committed fixture is the `CommonHttpResponse`-shaped **response** envelope
(the `data` payload is what matters; the `timestamp` is pinned so the fixture is deterministic).

- **Producers** own the files: each provider service's `*HttpContractTest` (in its adapter module)
  serializes a representative response `data` and (re)writes `http/<Bc>.<Rpc>.response.json`.
- **Consumer** pins itself: Checkout's `OutboundHttpContractBindingTest` binds each outbound client's
  wire DTO (`InventoryClientOa.ReserveResult`, `PaymentClientOa.EscrowBody`, `OrderClientOa.OrderIdBody`,
  `CatalogClientOa.PriceLine`) from the fixture and asserts the values — a silently renamed provider
  field breaks Checkout's build, not the running saga.

| Fixture | Producer | Consumer binding it |
|---|---|---|
| `http/Catalog.GetPrice.response.json` | catalog | checkout (`CatalogClientOa`) |
| `http/Inventory.ReserveStock.response.json` | inventory | checkout (`InventoryClientOa`) |
| `http/Order.CreatePendingOrder.response.json` | order | checkout (`OrderClientOa`) |
| `http/Payment.InitEscrow.response.json` | payment | checkout (`PaymentClientOa`) |

> Only endpoints with a response **body** are pinned; the bodiless edges (`releaseStock`,
> `cancelOrder`, reservation release) carry no `data` to bind. Request bodies are not yet pinned —
> the symmetric next step if request-shape drift becomes a concern. When the real gRPC transport
> lands, the `.proto` IDL becomes the contract and these fixtures retire.
