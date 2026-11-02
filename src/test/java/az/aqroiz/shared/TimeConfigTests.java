package az.aqroiz.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class TimeConfigTests {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(TimeConfig.class);

    @Test
    void clockUsesBakuZoneByDefault() {
        runner.run(context ->
                assertThat(context.getBean(Clock.class).getZone()).isEqualTo(ZoneId.of("Asia/Baku")));
    }

    @Test
    void zoneCanBeChangedFromConfiguration() {
        runner.withPropertyValues("aqroiz.time.business-zone=Asia/Tashkent")
                .run(context ->
                        assertThat(context.getBean(Clock.class).getZone()).isEqualTo(ZoneId.of("Asia/Tashkent")));
    }

    @Test
    void invalidZonePreventsStartup() {
        runner.withPropertyValues("aqroiz.time.business-zone=Asia/Bakuu")
                .run(context -> assertThat(context).hasFailed());
    }

    /**
     * Niyə Bakı zonası vacibdir: UTC ilə hələ 1 noyabrdır, Bakıda isə artıq 2 noyabrdır.
     * Qəbz 2 noyabr qiyməti ilə hesablanmalıdır.
     */
    @Test
    void lateNightUtcIsAlreadyNextDayInBaku() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-01T21:30:00Z"), ZoneId.of("Asia/Baku"));

        assertThat(LocalDate.now(clock)).isEqualTo(LocalDate.of(2026, 11, 2));
    }
}