package az.aqroiz.shared.security;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Controller metodlarında CurrentUser parametrini avtomatik doldurur:
 * <pre>
 * {@code @GetMapping List<...> list(CurrentUser currentUser) { ... } }
 * </pre>
 */
@Configuration(proxyBeanMethods = false)
class CurrentUserWebConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        // Bean inject etmirik, birbaşa yaradırıq: @WebMvcTest testləri bu konfiqurasiyanı yükləyir,
        // amma CurrentUserProvider bean-ini yükləmir — inject etsəydik, həmin testlərin konteksti qalxmazdı.
        resolvers.add(new CurrentUserArgumentResolver(new CurrentUserProvider()));
    }

    private record CurrentUserArgumentResolver(CurrentUserProvider provider) implements HandlerMethodArgumentResolver {

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return CurrentUser.class.equals(parameter.getParameterType());
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
            return provider.get();
        }
    }
}
