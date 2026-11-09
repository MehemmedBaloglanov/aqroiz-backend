package az.aqroiz.catalog;
/**
 * Məhsulun sortu. Bazada product_prices.grade (ck_product_prices_grade).
 * Qəbz modulu (procurement) da istifadə edir.
 */
public enum Grade {

    /** Birinci sort: ixrac keyfiyyəti. */
    I,

    /** İkinci sort. */
    II,

    /** Üçüncü sort: emal üçün. */
    III
}
