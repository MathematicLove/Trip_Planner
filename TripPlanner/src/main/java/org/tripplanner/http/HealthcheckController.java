package org.tripplanner.http;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class HealthcheckController {

    private final List<String> authors;

    public HealthcheckController(
            @Value("#{'${project.authors}'.split(',')}") List<String> authors
    ) {
        this.authors = authors;
    }

    @GetMapping("/healthcheck")
    public Map<String, Object> health() {
        return Map.of(
                "status",  "ok",
                "authors", authors
        );
    }
}
