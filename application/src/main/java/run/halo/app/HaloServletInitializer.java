package run.halo.app;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Bridge for WAR-style deployment. It is never used when running the executable JAR
 * (or WAR) directly with {@code java -jar}.
 *
 * <p>NOTE: Halo is a reactive (WebFlux) application. Spring Boot does not support
 * deploying WebFlux apps into external servlet containers; this class exists only to
 * keep the WAR artifact standard-compliant for operations that require a .war form.
 * Run the packaged WAR directly with {@code java -jar} instead.
 *
 * @see Application
 */
public class HaloServletInitializer extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(Application.class);
    }
}
