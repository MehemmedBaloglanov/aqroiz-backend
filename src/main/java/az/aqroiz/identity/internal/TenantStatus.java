package az.aqroiz.identity.internal;
/**
 * Şirkətin statusu. Bazada tenants.status (ck_tenants_status).
 */
public enum TenantStatus {

    /** Şirkət sistemdən istifadə edir. */
    ACTIVE,

    /** Giriş dayandırılıb (məsələn, ödəniş edilməyib). Məlumatlar silinmir. */
    SUSPENDED
}
