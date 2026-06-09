# Service Architecture & Conventions (msfw) — TEMPLATE

> Portable starter for a new **msfw** microservice (`tech.vsf.ptnt.msfw.*`). Copy this file to the new
> repo's root as `ARCHITECTURE.md`, then replace `<org>`, `<service>`, `<feature>` placeholders and trim
> sections you don't need. The full reference lives in the `ecommerce-msfw` workspace `ARCHITECTURE.md`.

Architecture = **Hexagonal** (`domain → application → adapter`, deps point inward) ×
**package-by-feature** (vertical use-case slices) × **inbound/outbound** (adapter only).

---

## 1. Module layout

```
<service>/                  (pom packaging=pom)
├── domain/        → <service>-domain       (jar, pure Java; msfw domain-core only)
├── application/   → <service>-application   (jar; depends on domain)
└── adapter/       → <service>-service       (Spring Boot; depends on application)
```

Parent pom: Java 21, Spring Boot 4.0.6, Spring Cloud 2025.1.1, `msfw 1.0-SNAPSHOT`;
`maven-compiler-plugin` with `<parameters>true</parameters>` (**required** — msfw reflection + Spring MVC
need parameter names).

msfw deps by module: `domain` → `domain-core`; `application` → `domain-core` (+ `event-core`, `outbox`
if publishing); `adapter` → `spring-outbox` or `spring-adapter-core` (+ `event-consumption` if consuming).

> ⚠ **Lombok ↔ build JDK**: if the adapter uses Lombok, the Lombok version must support the JDK you
> *build* with (not just the Java 21 target). Old Lombok silently fails to generate constructors on
> newer JDKs. Use Lombok ≥ 1.18.46 AND declare it in `annotationProcessorPaths`:
> ```xml
> <plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-compiler-plugin</artifactId>
>   <configuration><parameters>true</parameters>
>     <annotationProcessorPaths>
>       <path><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId><version>1.18.46</version></path>
>     </annotationProcessorPaths>
>   </configuration></plugin>
> ```

---

## 2. Naming (apply at every layer, per feature slice `<feature>`)

| Kind | Pattern / base type |
|---|---|
| Aggregate root | `extends Aggregate<TId>` (set `this.id` in ctor; has `_id()`/`set_id` technical key) |
| Identity | `extends Identity<T>` (override `value()`, `equals`, `hashCode`) |
| Value object | `implements DomainValue` (prefer `record`) — msfw ships `Money/Currency/Quantity/DTime` |
| Domain event | `extends AbstractDomainEvent` (`super(DomainEventType.of("<Ctx>","<Name>"), occurredAt)`) |
| Factory | `implements Factory<T,P>`; publish via `DomainEventPublisher.publish(...)` |
| Use-case port / impl | verb interface (`CreateProduct`) / `…Uc` |
| Input / read model | `…Cmd` (record) / `…View` |
| Repository port | `… extends Repository<T>` |
| REST / boundary / DTO | `…Controller` / `…Facade` (`@Service`,`@Transactional`) / `…Request`,`…Response` |
| Persistence | `…Oa` (impl `Repository<T>`), `…Entity` (`@Entity`), `…JpaRepository extends JpaRepository` |
| Consumer | `…PipelineFactory extends FiveStepsPipelineFactory<Cmd>` |

Adapter package: `…adapter.<feature>.{inbound/{restful,messaging}, outbound/persistence}` +
`…adapter.configuration`.

---

## 3. Cookbook (essentials)

**Use-case + outbox publish:**
```java
public class CreateXUc implements CreateX {
    private final Repository<X> repo;
    public CreateXUc(Repository<X> repo) { this.repo = repo; }

    @Override
    @EventPublishHandler(eventProcessors = JsonEventStoreProcessor.class)   // outbox, NOT direct publish
    public void execute(XCmd cmd) {
        X x = new XFactory().create(cmd.toFactoryParams());   // factory publishes domain event
        repo.save(x);
    }
}
```

**Wiring (`AdapterConfiguration`):**
```java
@Configuration
@Import({ SpringCoreConfiguration.class, OutboxConfiguration.class })  // OutboxConfiguration only if publishing
@ComponentScan("vn.<org>.<service>.adapter")
@EntityScan("vn.<org>.<service>.adapter")            // org.springframework.boot.persistence.autoconfigure.EntityScan (SB4)
@EnableJpaRepositories("vn.<org>.<service>.adapter")
public class AdapterConfiguration {
    @Bean CreateX createX(Repository<X> repo) { return new CreateXUc(repo); }  // @Bean, not @Component
}
```

**Outbound adapter** = hand-written `…Oa implements Repository<X>` mapping domain↔entity (use the
aggregate `Memento` for reconstruction); `…JpaRepository` is an empty Spring Data interface.

**Consume event** = `…PipelineFactory extends FiveStepsPipelineFactory<Cmd>` overriding
`declareDomainEventType / declareEventDeserializer / declareEventDataDeserializer /
declareApplicationInputPreparer / declareApplicationTarget`.
> ⚠ msfw has **no default** `CloudEventDeserializer` / `EventDataDeserializer` — write them per schema.

---

## 4. Conventions that prevent real bugs

- **Pagination is 0-based**: `Pagination.offset() == page * size`. Controllers default `page=0` (not 1)
  and clamp negatives to 0; `defaultValue="1"` silently skips the first page. If the API is 1-based,
  convert at the facade (`new Pagination(page-1, size)`).
- **Error handling**: don't `try/catch → 500` in controllers; throw `DomainException`/
  `ResourceNotFoundException` (with `DomainErrorCode`) and let `GlobalExceptionHandler` map to HTTP.
- **Local run without infra**: provide a `standalone` profile (H2 + JSON outbox beans) — see msfw
  `sample-service` and the `catalog` standalone config.

---

## 5. Testing

- Build aggregates via their `Factory` (ctors are package-private); `DomainEventPublisher.clear()` after.
- Assert events via `DomainEventPublisher.getEvents()`; `clear()` in `@BeforeEach`/`@AfterEach`.
- Prefer hand fakes for `Repository<T>`/ports over Mockito (ByteBuddy lags new JDKs).
- Controllers: `MockMvcBuilders.standaloneSetup(controller)` + real facade + fake use-case (no `@SpringBootTest`).
- Cover: value-object validation, factory (create+event), each use-case (persist + publish/lookup +
  error branch), `…Oa` (save/find/paging), DTO mapping, controller (status + body + error mapping).

---

## 6. New-service checklist

1. 3 modules + parent pom (Java 21, SB 4.0.6, `<parameters>true</parameters>`, Lombok in `annotationProcessorPaths`).
2. domain: aggregate / identity / value / event (`DomainEventType`) / factory.
3. application: `Xxx` port + `…Uc`, `…Cmd`, repository port; publish → `@EventPublishHandler`.
4. adapter: `inbound/restful` + `Facade`; `outbound/persistence` `…Oa`+`…Entity`+`…JpaRepository`;
   `messaging` `…PipelineFactory` if consuming.
5. `AdapterConfiguration` imports `SpringCoreConfiguration` (+ `OutboxConfiguration` if publishing); use-cases as `@Bean`.
6. `bootstrap.yml` → config server; outbox migrations (`outbox_events`, `published_event_trackers`).
7. Tests per §5.
