package org.tripplanner;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.tripplanner.config.AppConfig;

/** Точка входа: Spring-MVC + embedded Jetty. */
public class Main {

    public static void main(String[] args) throws Exception {

        /* Web-контекст (refresh вызовет сам DispatcherServlet) */
        AnnotationConfigWebApplicationContext ctx =
                new AnnotationConfigWebApplicationContext();
        ctx.register(AppConfig.class);

        DispatcherServlet dispatcher = new DispatcherServlet(ctx);
        ServletHolder     holder     = new ServletHolder(dispatcher);

        ServletContextHandler handler =
                new ServletContextHandler(ServletContextHandler.SESSIONS);
        handler.setContextPath("/");
        handler.addServlet(holder, "/*");

        Server jetty = new Server(8080);
        jetty.setHandler(handler);
        jetty.start();

        System.out.println("🚀 Trip-Planner HTTP на :8080");
        Thread.currentThread().join();
    }
}


/*
* # 0. Остановить всё, чтобы начать “чисто”
docker compose down     # удаляет старые контейнеры, но не образы

# 1. Полностью пересобрать fat-JAR локально
./gradlew clean shadowJar

# 2. Пересобрать образ *app* и поднять весь стек
docker compose up --build -d

# 3. Убедиться, что контейнер жив и healthy
docker compose ps

# 4. Показать последние строки лога и найти ключ
docker compose logs --tail=50 app

* */