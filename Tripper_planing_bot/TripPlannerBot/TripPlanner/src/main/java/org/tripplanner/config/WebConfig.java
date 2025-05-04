package org.tripplanner.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.tripplanner.http.ApiKeyInterceptor;

/**
 * MVC-конфигурация (без Spring-Boot).
 *
 * ▸ Регистрирует перехватчик API-ключа
 * ▸ Настраивает JSP-view-resolver  (/WEB-INF/ui/*.jsp)
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /** значение admin.api.key из application.properties  */
    private final String adminKey;

    public WebConfig(@Value("${admin.api.key}") String adminKey) {
        this.adminKey = adminKey;
    }

    /* ------------------------------------------------------------------ */
    /*  View-resolver:  "/WEB-INF/ui/<view>.jsp"                          */
    /* ------------------------------------------------------------------ */
    @Bean
    public ViewResolver jspResolver() {
        InternalResourceViewResolver vr = new InternalResourceViewResolver();
        vr.setPrefix("/WEB-INF/ui/");       // где лежат JSP
        vr.setSuffix(".jsp");               // шаблон имени
        vr.setOrder(0);                     // приоритет
        vr.setContentType("text/html; charset=UTF-8");
        return vr;
    }

    /* ------------------------------------------------------------------ */
    /*  Перехватчик для /admin/** и /users/**                             */
    /* ------------------------------------------------------------------ */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new ApiKeyInterceptor(adminKey))
                .addPathPatterns("/admin/**", "/users/**");   // 🔒 защищённые URL
    }

    @Override
    public void configureViewResolvers(ViewResolverRegistry registry) {
    registry.jsp("/WEB-INF/ui/", ".jsp");
}
}
