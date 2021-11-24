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

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

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

    @Bean(name = "backgroundExecutor")
    public ExecutorService backgroundExecutor() {
        return new ThreadPoolExecutor(10, 50, 1, TimeUnit.MINUTES, new LinkedBlockingQueue<>(),
                new ThreadFactory() {
                    final AtomicLong count = new AtomicLong(1L);
                    @Override
                    public Thread newThread(Runnable r) {
                        var thread = Executors.defaultThreadFactory().newThread(r);
                        thread.setName(String.format(Locale.ROOT, "consumer-%2d", count.getAndIncrement()));
                        thread.setUncaughtExceptionHandler(Thread.getDefaultUncaughtExceptionHandler());
                        return thread;
                    }
                });
    }
}
