package com.data.personalfinanceinsightai.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppTimeConfig {

    public static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Bean
    public Clock appClock() {
        return Clock.system(APP_ZONE);
    }
}
