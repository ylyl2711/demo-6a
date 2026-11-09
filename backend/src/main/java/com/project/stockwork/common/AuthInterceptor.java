package com.project.stockwork.common;

import com.project.stockwork.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    private static final String[] WHITE_LIST = {
            "/api/auth/login",
            "/uploads/",
            "/error",
            "/favicon.ico"
    };

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String uri = request.getRequestURI();
        for (String white : WHITE_LIST) {
            if (uri.startsWith(white)) {
                return true;
            }
        }
        if (uri.startsWith("/doc.html") || uri.startsWith("/webjars/")
                || uri.startsWith("/swagger-resources") || uri.startsWith("/v2/api-docs")) {
            return true;
        }
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        String header = request.getHeader("Authorization");
        if (header == null || header.trim().isEmpty()) {
            throw new BizException(401, "未登录或登录已失效");
        }
        String token = header.startsWith("Bearer ") ? header.substring(7) : header;
        LoginUser user = jwtUtil.parseToken(token);
        if (user == null) {
            throw new BizException(401, "登录凭证无效，请重新登录");
        }
        user.setIp(clientIp(request));
        UserContext.set(user);

        HandlerMethod method = (HandlerMethod) handler;
        RequireRole requireRole = method.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = method.getBeanType().getAnnotation(RequireRole.class);
        }
        if (requireRole != null && !Arrays.asList(requireRole.value()).contains(user.getRoleCode())) {
            throw new BizException(403, "当前角色无权访问该功能");
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty()) {
            return ip.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
