package az.aqroiz.shared.security;

/**
 * JWT-dəki öz claim-lərimizin adları.
 * Token yaradan (identity.TokenService) və oxuyan (CurrentUserProvider) eyni adları istifadə etsin deyə bir yerdədir.
 */
public final class JwtClaims {

    public static final String TENANT_ID = "tenantId";
    public static final String ROLE = "role";

    private JwtClaims() {
    }
}
