package az.aqroiz;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Modul sərhədlərini yoxlayır:
 * - modullar bir-birinin daxili (internal) siniflərinə müraciət etmir;
 * - modullar arasında dövri asılılıq yoxdur.
 */
public class ModularityTests {

    private final ApplicationModules modules = ApplicationModules.of(AqroizBackendApplication.class);

    @Test
    void moduleStructureIsValid(){
        modules.verify();
    }

    @Test
    void printModules(){
        modules.forEach(System.out::println);
    }
}
