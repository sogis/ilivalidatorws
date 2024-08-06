package ch.so.agi.ilivalidator.cleaner;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class CleanerAsyncConfiguration {
    
    @Bean(name = "cleanerAsyncTaskExecutor")
    ThreadPoolTaskExecutor asyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2); // Set the initial number of threads in the pool
        executor.setMaxPoolSize(5); // Set the maximum number of threads in the pool
        executor.setQueueCapacity(10); // Set the queue capacity for holding pending tasks
        executor.setThreadNamePrefix("CleanerAsyncTask-"); // Set a prefix for thread names
        executor.initialize();
        return executor;
    }
}
