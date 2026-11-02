package az.aqroiz.shared;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Vaxtla bağlı parametrlər (application.yaml → aqroiz.time).
 *
 * @param businessZone biznes tarixlərinin hesablandığı saat qurşağı (qəbul günü, qiymət günü)
 */

@ConfigurationProperties(prefix = "aqroiz.time")
public record TimeProperties(@DefaultValue("Asia/Baku") String businessZone) {
}
