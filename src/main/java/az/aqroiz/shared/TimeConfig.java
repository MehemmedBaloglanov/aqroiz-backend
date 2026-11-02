package az.aqroiz.shared;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tətbiqin yeganə vaxt mənbəyi.
 * <p>
 * Qayda: kodda Instant.now(), LocalDate.now(), LocalDateTime.now() birbaşa çağırılmır.
 * Həmişə bu Clock inject olunur: clock.instant() — UTC an, LocalDate.now(clock) — Bakı günü.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TimeProperties.class)
public class TimeConfig {

    @Bean
    public Clock clock(TimeProperties properties){
        return Clock.system(ZoneId.of(properties.businessZone()));
    }
}
