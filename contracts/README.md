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

Changing a payload: change the domain event, run the producer's contract test, commit the new
fixture **together with** the consumer-side DTO/test updates the diff forces you to look at.
