package vn.marketplace.checkout.k8s;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import vn.marketplace.checkout.adapter.configuration.AdapterConfiguration;

/**
 * In-cluster entry point — boots Checkout (stateless saga, in-memory session) in the mesh, no config
 * server. Sibling service URLs come from {@code application-k8s.properties} (in-cluster Service DNS).
 * Same {@link AdapterConfiguration} as production; Checkout has no DB, so the JPA autoconfigs that
 * spring-adapter-core drags in are excluded. Run with {@code SPRING_PROFILES_ACTIVE=k8s}.
 */
@SpringBootApplication(excludeName = {
        "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
        "org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration",
        "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
        "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration"})
@Import(AdapterConfiguration.class)
public class CheckoutK8sApplication {
    public static void main(String[] args) {
        System.setProperty("spring.cloud.bootstrap.name", "bootstrap-k8s");
        SpringApplication.run(CheckoutK8sApplication.class, args);
    }
}
