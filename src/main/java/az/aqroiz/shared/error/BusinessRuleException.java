package az.aqroiz.shared.error;

import java.util.Objects;

/**
 * Biznes qaydası pozulanda atılır. Cavab: 422 Unprocessable Entity.
 * <p>
 * Nümunə: new BusinessRuleException("NET_NOT_POSITIVE", "Net çəki sıfırdan böyük olmalıdır.")
 *
 * @see az.aqroiz.shared.error.GlobalExceptionHandler
 */


public class BusinessRuleException extends RuntimeException{

    private final String code;

    public BusinessRuleException(String code, String message){
        super(message);
        this.code = Objects.requireNonNull(code,"code");
    }

    public String getCode(){
        return code;
    }
}
