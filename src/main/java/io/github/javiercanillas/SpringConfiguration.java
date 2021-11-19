package io.github.javiercanillas;

import com.netflix.conductor.client.http.WorkflowClient;
import com.netflix.conductor.client.spring.ClientProperties;
import io.github.javiercanillas.workers.SpringWorkersConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan
@EnableAutoConfiguration
@Import({SpringWorkersConfiguration.class})
@EnableConfigurationProperties(ClientProperties.class)
public class SpringConfiguration {

    @Bean
    public WorkflowClient workflowClient(ClientProperties clientProperties) {
        final var workflowClient = new WorkflowClient();
        workflowClient.setRootURI(clientProperties.getRootUri());
        return workflowClient;
    }
}
