package cl.duoc.dina_backend.security;

import cl.duoc.dina_backend.exception.ApiException;
import cl.duoc.dina_backend.model.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public WebConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/login", "/api/salud");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new UsuarioActualResolver());
        resolvers.add(new TokenActualResolver());
    }

    /** Entrega el usuario que dejó AuthInterceptor a los parámetros marcados con @UsuarioActual. */
    static class UsuarioActualResolver implements HandlerMethodArgumentResolver {
        @Override
        public boolean supportsParameter(MethodParameter p) {
            return p.hasParameterAnnotation(UsuarioActual.class) && p.getParameterType() == Usuario.class;
        }

        @Override
        public Object resolveArgument(MethodParameter p, ModelAndViewContainer mav,
                                      NativeWebRequest req, WebDataBinderFactory bf) {
            Object usuario = req.getNativeRequest(HttpServletRequest.class).getAttribute(AuthInterceptor.ATRIBUTO_USUARIO);
            if (usuario == null) throw ApiException.noAutenticado("Falta el token de sesión");
            return usuario;
        }
    }

    /** Entrega el token actual a un parámetro {@code @UsuarioActual String token} (para cerrar sesión). */
    static class TokenActualResolver implements HandlerMethodArgumentResolver {
        @Override
        public boolean supportsParameter(MethodParameter p) {
            return p.hasParameterAnnotation(UsuarioActual.class) && p.getParameterType() == String.class;
        }

        @Override
        public Object resolveArgument(MethodParameter p, ModelAndViewContainer mav,
                                      NativeWebRequest req, WebDataBinderFactory bf) {
            return req.getNativeRequest(HttpServletRequest.class).getAttribute(AuthInterceptor.ATRIBUTO_TOKEN);
        }
    }
}
