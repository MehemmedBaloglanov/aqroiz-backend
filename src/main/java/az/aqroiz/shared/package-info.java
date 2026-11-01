@ApplicationModule(type = ApplicationModule.Type.OPEN)
package az.aqroiz.shared;

/**
 * Bütün modulların istifadə etdiyi ümumi infrastruktur:
 * xəta formatı, vaxt (Clock), biznes istisnaları və s.
 * <p>
 * Bu modulda biznes məntiqi olmur.
 */

import org.springframework.modulith.ApplicationModule;