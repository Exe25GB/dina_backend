package cl.duoc.dina_backend.security;

import cl.duoc.dina_backend.exception.ApiException;
import cl.duoc.dina_backend.model.Usuario;
import cl.duoc.dina_backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Revisa el token de cada petición a /api/** (salvo login y salud).
 * La app debe enviar el header: Authorization: Bearer <token>
 * Las rutas /api/admin/** además exigen rol ADMIN.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    static final String ATRIBUTO_USUARIO = "dina.usuario";
    static final String ATRIBUTO_TOKEN = "dina.token";
    private static final String PREFIJO = "Bearer ";

    private final AuthService authService;

    public AuthInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(PREFIJO) || header.length() == PREFIJO.length()) {
            throw ApiException.noAutenticado("Falta el token de sesión");
        }
        String token = header.substring(PREFIJO.length()).trim();
        Usuario usuario = authService.usuarioPorToken(token);

        if (request.getRequestURI().startsWith("/api/admin/") && !usuario.esAdmin()) {
            throw ApiException.prohibido("Solo un administrador puede hacer esto");
        }

        request.setAttribute(ATRIBUTO_USUARIO, usuario);
        request.setAttribute(ATRIBUTO_TOKEN, token);
        return true;
    }
}
