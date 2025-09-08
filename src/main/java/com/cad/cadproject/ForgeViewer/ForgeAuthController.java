package com.cad.cadproject.ForgeViewer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/api/forge")
public class ForgeAuthController {

    @Value("${forge.client.id}")
    private String clientId;

    @Value("${forge.client.secret}")
    private String clientSecret;

    private final String tokenUrl = "https://developer.api.autodesk.com/authentication/v2/token";

    @GetMapping("/token")
    public ResponseEntity<Map<String, Object>> getAccessToken() {
        RestTemplate restTemplate = new RestTemplate();

        // 요청 파라미터
        Map<String, String> params = new HashMap<>();
        params.put("client_id", clientId);
        params.put("client_secret", clientSecret);
        params.put("grant_type", "client_credentials");
        params.put("scope", "data:read data:write bucket:create bucket:read");

        // HTTP 요청 헤더
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // Body 변환
        StringBuilder body = new StringBuilder();
        params.forEach((key, value) -> body.append(key).append("=").append(value).append("&"));
        if (body.length() > 0) body.setLength(body.length() - 1); // 마지막 & 제거

        HttpEntity<String> request = new HttpEntity<>(body.toString(), headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> result = response.getBody();
                if (result != null && result.containsKey("access_token")) {
                    System.out.println("Server 토큰 생성 완료 : " + result.get("access_token"));
                }
                return ResponseEntity.ok(result);
            } else {
                System.out.println("❌ 토큰 요청 실패 : " + response.getStatusCode());
                return ResponseEntity.status(response.getStatusCode()).body(null);
            }
        } catch (Exception e) {
            System.out.println("❌ 토큰 요청 중 예외 발생: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
