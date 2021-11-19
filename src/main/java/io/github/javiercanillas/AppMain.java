package io.github.javiercanillas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({ SpringConfiguration.class })
public class AppMain extends SpringBootServletInitializer {
    public static void main(final String[] args) {
        SpringApplication.run(AppMain.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(final SpringApplicationBuilder application) {
        return application.sources(AppMain.class);
    }
}
