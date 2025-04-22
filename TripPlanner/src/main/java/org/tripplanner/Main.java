package org.tripplanner;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.tripplanner.config.AppConfig;

public class Main {
    public static void main(String[] args) {
        // Initialize Spring 6 application context with our AppConfig
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(AppConfig.class)) {

            System.out.println("🚀 Trip Planner Bot is up and running!");

            // Keep the main thread alive so that:
            //  - @Scheduled tasks (reminders) continue to fire
            //  - TelegramBotService polling stays active
            Thread.currentThread().join();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Application interrupted, shutting down.");
        }
    }
}
