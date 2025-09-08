package com.cad.cadproject.test;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class TestController3 {

    // 간단한 GET API
    @GetMapping("/api/test3")
    public Map<String, String> testApi() {
        Map<String, String> result = new HashMap<>();
        result.put("message", "Spring Boot TestController3 API 호출 성공!");
        return result;
    }
}
