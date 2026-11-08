package az.aqroiz.shared.error;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import az.aqroiz.shared.TimeConfig;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Xəta formatını yoxlayır. Bazanı qaldırmır: yalnız web qatı (@WebMvcTest).
 * Test üçün xüsusi controller istifadə olunur, çünki hələ real endpoint yoxdur.
 */
@WebMvcTest
@Import({TimeConfig.class, GlobalExceptionHandlerTests.TestController.class})
@WithMockUser
class GlobalExceptionHandlerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void notFoundReturns404() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.title").value("Tapılmadı"))
                .andExpect(jsonPath("$.detail").value("Fermer tapılmadı"))
                .andExpect(jsonPath("$.instance").value("/test/not-found"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void businessRuleReturns422WithItsOwnCode() throws Exception {
        mockMvc.perform(get("/test/business"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("NET_NOT_POSITIVE"))
                .andExpect(jsonPath("$.detail").value("Net çəki sıfırdan böyük olmalıdır."));
    }

    @Test
    void validationReturns400WithFieldErrorsInAzerbaijani() throws Exception {
        mockMvc.perform(post("/test/validation").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "", "amount": -5}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.length()").value(2))
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("name", "amount")))
                .andExpect(jsonPath("$.errors[*].message",
                        containsInAnyOrder("boş ola bilməz", "sıfırdan böyük olmalıdır")));
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/test/validation").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void wrongPathVariableTypeReturns400() throws Exception {
        mockMvc.perform(get("/test/number/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TYPE_MISMATCH"));
    }

    @Test
    void unexpectedErrorReturns500WithoutInternalDetails() throws Exception {
        mockMvc.perform(get("/test/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.detail", not(containsString("gizli"))));
    }

    @Test
    void unsupportedMethodGetsCodeAndTimestamp() throws Exception {
        mockMvc.perform(post("/test/not-found").with(csrf()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("HTTP_405"))
                .andExpect(jsonPath("$.title").value("Metod dəstəklənmir"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @RestController
    @RequestMapping("/test")
    static class TestController {

        record Payload(@NotBlank String name, @NotNull @Positive BigDecimal amount) {
        }

        @GetMapping("/not-found")
        void notFound() {
            throw new NotFoundException("Fermer tapılmadı");
        }

        @GetMapping("/business")
        void business() {
            throw new BusinessRuleException("NET_NOT_POSITIVE", "Net çəki sıfırdan böyük olmalıdır.");
        }

        @PostMapping("/validation")
        void validation(@Valid @RequestBody Payload payload) {
        }

        @GetMapping("/number/{value}")
        void number(@PathVariable int value) {
        }

        @GetMapping("/boom")
        void boom() {
            throw new IllegalStateException("gizli daxili detal");
        }
    }
}