package com.cad.cadproject.ForgeViewer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/forge/bucket")
public class ForgeBucketController {

    @Value("${forge.client.id}")
    private String clientId;

    @Value("${forge.client.secret}")
    private String clientSecret;

    private final String tokenUrl = "https://developer.api.autodesk.com/authentication/v2/token";
    private final String bucketsUrl = "https://developer.api.autodesk.com/oss/v2/buckets";

    // 1️⃣ Forge 토큰 발급
    private String getAccessToken() {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body = "client_id=" + clientId +
                      "&client_secret=" + clientSecret +
                      "&grant_type=client_credentials" +
                      "&scope=data:read data:write bucket:create bucket:read";

        HttpEntity<String> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.exchange(tokenUrl, HttpMethod.POST, request, Map.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return (String) response.getBody().get("access_token");
        }
        throw new RuntimeException("❌ Forge 토큰 발급 실패");
    }

    // 2️⃣ 토큰 JSON으로 반환
    @GetMapping("/token")
    public ResponseEntity<Map<String, Object>> getToken() {
        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", getAccessToken());
        return ResponseEntity.ok(result);
    }

    // 3️⃣ Bucket 생성 or 기존 버킷 사용 + JSON 반환
    @PostMapping
    public ResponseEntity<Map<String, Object>> createBucket(@RequestParam String bucketKey) {
        RestTemplate restTemplate = new RestTemplate();
        String accessToken = getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("bucketKey", bucketKey);
        bodyMap.put("policyKey", "persistent"); // ✅ persistent로 변경
        bodyMap.put("region", "US");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(bodyMap, headers);
        Map<String, Object> result = new HashMap<>();

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    bucketsUrl,
                    HttpMethod.POST,
                    request,
                    String.class
            );
            System.out.println("✅ Server에서 버킷 생성 : " + bucketKey);
            result.put("status", response.getStatusCodeValue());
            result.put("message", "Bucket 생성 성공");
            result.put("bucketKey", bucketKey);
            result.put("data", response.getBody());
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.CONFLICT) { // 이미 존재
                System.out.println("⚠ Server에서 버킷 생성 : 이미 존재 -> " + bucketKey);
                result.put("status", 200);
                result.put("message", "버킷 이미 존재");
                result.put("bucketKey", bucketKey);
            } else {
                System.out.println("❌ Server에서 버킷 생성 실패 : " + e.getResponseBodyAsString());
                result.put("status", e.getStatusCode().value());
                result.put("message", "Bucket 생성 실패");
                result.put("error", e.getResponseBodyAsString());
            }
        } catch (Exception e) {
            System.out.println("❌ Server에서 버킷 생성 실패 : " + e.getMessage());
            result.put("status", 500);
            result.put("message", "Bucket 생성 실패");
            result.put("error", e.getMessage());
        }

        return ResponseEntity.ok(result);
    }
}
