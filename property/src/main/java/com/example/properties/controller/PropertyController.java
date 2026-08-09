package com.example.properties.controller;

import com.example.properties.property.ApplicationProperty;
import com.example.properties.property.DeveloperProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

@RestController
public class PropertyController {
    private final ApplicationProperty applicationProperty;
    private final DeveloperProperty developerProperty;

    @Autowired
    public PropertyController(ApplicationProperty applicationProperty, DeveloperProperty developerProperty) {
        this.applicationProperty = applicationProperty;
        this.developerProperty = developerProperty;
    }

    @GetMapping("/property")
    public Map<String, Object> index() {
        Map<String, Object> result = new HashMap<>();
        result.put("applicationProperty", applicationProperty);
        result.put("developerProperty", developerProperty);
        return result;
    }

    @GetMapping("/hello")
    public String sayHello(@RequestParam(required = false, name = "who") String who) {
        if (who == null || who.isBlank()) {
            who = "World";
        }
        return "Hello, " + who + "!";
    }
}
