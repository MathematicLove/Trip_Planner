package org.tripplanner.http;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Objects;

/**
 * Пропускает запрос, если ключ совпал
 *   • заголовок  X-API-KEY: <key>
 *   • либо query /path?key=<key>
 *
 * Сравнение без учёта регистра, пробелов и CR/LF.
 */
public class ApiKeyInterceptor implements HandlerInterceptor {

    /** «Правильный» ключ из ENV / application.properties */
    private final String expected;

    public ApiKeyInterceptor(String expectedApiKey) {
        this.expected = normalize(Objects.requireNonNull(expectedApiKey));
    }

    /** приводим к верхнему регистру, убираем пробелы и управляющие символы */
    private static String normalize(String s) {
        if (s == null) return null;
        return s.replaceAll("[\\s\r\n]+", "")  // пробел, таб, CR, LF
                .toUpperCase();
    }

    /* для разового дебага можно оставить печать */
    private static void dump(String label, String value) {
        System.out.printf("%s = [%s]%n", label, value);
    }

    @Override
    public boolean preHandle(HttpServletRequest req,
                             HttpServletResponse resp,
                             Object handler) throws IOException {

        String key = req.getHeader("X-API-KEY");
        if (key == null || key.isBlank()) {
            key = req.getParameter("key");     // fallback из ?key=
        }

        key = normalize(key);                  // привели к общему виду

        // ---- debug (можно убрать) ----
        dump("EXPECTED", expected);
        dump("FROM REQ", key);
        // ------------------------------

        if (expected.equals(key)) {            // полное совпадение
            return true;                       // → пропускаем
        }

        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        resp.getWriter().println("API key required");
        return false;
    }
}
