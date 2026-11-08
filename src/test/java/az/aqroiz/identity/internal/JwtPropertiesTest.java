package az.aqroiz.identity.internal;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class JwtPropertiesTest {

    @Test
    void shortSecretIsRejected() {
        assertThatThrownBy(() -> new JwtProperties("qisa-acar", Duration.ofHours(12), "aqroiz"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32 bayt");
    }

    @Test
    void zeroTtlIsRejected() {
        assertThatThrownBy(() -> new JwtProperties("0123456789-0123456789-0123456789-ab", Duration.ZERO, "aqroiz"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
