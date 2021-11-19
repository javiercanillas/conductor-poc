package io.github.javiercanillas.workers;

import com.netflix.conductor.client.spring.ConductorClientAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({ConductorClientAutoConfiguration.class})
public class SpringWorkersConfiguration {

}
