package az.aqroiz.shared.error;

/**
 * Axtarılan obyekt tapılmayanda atılır. Cavab: 404 Not Found.
 * <p>
 * Qeyd: başqa tenant-ın obyektinə müraciət də 404 qaytarır (403 yox),
 * çünki obyektin mövcud olduğunu belə açıqlamamalıyıq.
 */

public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
