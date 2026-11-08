package az.aqroiz.identity;
/**
 * İstifadəçi rolları. Bir istifadəçinin bir rolu var.
 * Bazada users.role sütununda mətn kimi saxlanılır (ck_users_role ilə yoxlanılır).
 */
public enum Role {

    /** Şirkət sahibi: hər şeyə icazə. */
    OWNER,

    /** Menecer: qəbul, partiya, yük, qiymətlər, ləğv. */
    MANAGER,

    /** Qəbulçu: sahədə qəbul, fermer qeydiyyatı. */
    RECEIVER,

    /** Mühasib: ödənişlər, hesabatlar, sənədlər. */
    ACCOUNTANT,

    /** Yalnız baxış. */
    VIEWER
}
