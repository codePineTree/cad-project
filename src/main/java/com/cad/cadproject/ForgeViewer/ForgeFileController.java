package com.cad.cadproject.ForgeViewer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/api/forge/file")
public class ForgeFileController {

    @Value("${forge.client.id}")
    private String clientId;

    @Value("${forge.client.secret}")
    private String clientSecret;

    // Data Management API 사용 (OSS v2 대신)
    private final String hubsUrl = "https://developer.api.autodesk.com/project/v1/hubs";
    private final String projectsUrl = "https://developer.api.autodesk.com/project/v1/hubs/%s/projects";
    private final String foldersUrl = "https://developer.api.autodesk.com/data/v1/projects/%s/folders";
    private final String storageUrl = "https://developer.api.autodesk.com/data/v1/projects/%s/storage";
    private final String itemsUrl = "https://developer.api.autodesk.com/data/v1/projects/%s/items";
    private final String versionsUrl = "https://developer.api.autodesk.com/data/v1/projects/%s/versions";
    private final String translateUrl = "https://developer.api.autodesk.com/modelderivative/v2/designdata/job";

    // -------------------- Forge Access Token --------------------
    private String getAccessToken() {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String body = "client_id=" + clientId +
                    "&client_secret=" + clientSecret +
                    "&grant_type=client_credentials" +
                    "&scope=data:read data:write data:create bucket:create bucket:read";

            HttpEntity<String> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://developer.api.autodesk.com/authentication/v2/token",
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String token = (String) response.getBody().get("access_token");
                System.out.println("✅ 토큰 생성 완료");
                return token;
            } else {
                throw new RuntimeException("Forge 토큰 발급 실패! HTTP Status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.out.println("❌ 토큰 발급 실패: " + e.getMessage());
            throw new RuntimeException("Forge 토큰 발급 실패! " + e.getMessage(), e);
        }
    }

    // -------------------- 임시 스토리지 생성 (Data Management API) --------------------
    private String createStorage(String projectId, String fileName, String accessToken) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> payload = new HashMap<>();
            Map<String, Object> data = new HashMap<>();
            data.put("type", "objects");
            
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("name", fileName);
            data.put("attributes", attributes);
            
            Map<String, Object> relationships = new HashMap<>();
            Map<String, Object> target = new HashMap<>();
            Map<String, Object> targetData = new HashMap<>();
            targetData.put("type", "folders");
            targetData.put("id", projectId); // 임시로 프로젝트 ID 사용
            target.put("data", targetData);
            relationships.put("target", target);
            data.put("relationships", relationships);
            
            payload.put("data", data);

            System.out.println("📤 스토리지 생성 요청: " + payload);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            String url = String.format(storageUrl, projectId);
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> responseBody = mapper.readValue(response.getBody(), Map.class);
            
            Map<String, Object> responseData = (Map<String, Object>) responseBody.get("data");
            String storageId = (String) responseData.get("id");
            
            System.out.println("✅ 스토리지 생성 완료: " + storageId);
            return storageId;
            
        } catch (Exception e) {
            System.out.println("❌ 스토리지 생성 실패: " + e.getMessage());
            throw new RuntimeException("스토리지 생성 실패! " + e.getMessage(), e);
        }
    }

    // -------------------- 파일 업로드 (Data Management API) --------------------
    private void uploadToStorage(String storageId, File file, String accessToken) {
        try {
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            
            // 스토리지 ID에서 실제 업로드 URL 추출
            String uploadUrl = storageId; // 스토리지 ID가 실제로는 업로드 URL

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

            HttpEntity<byte[]> request = new HttpEntity<>(fileBytes, headers);
            ResponseEntity<String> response = restTemplate.exchange(uploadUrl, HttpMethod.PUT, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("✅ 파일 업로드 성공: " + file.getName());
            } else {
                throw new RuntimeException("파일 업로드 실패: HTTP " + response.getStatusCode());
            }

        } catch (Exception e) {
            System.out.println("❌ 파일 업로드 실패: " + e.getMessage());
            throw new RuntimeException("파일 업로드 실패! " + e.getMessage(), e);
        }
    }

    // -------------------- 아이템 및 버전 생성 --------------------
    private String createItemAndVersion(String projectId, String fileName, String storageId, String accessToken) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> payload = new HashMap<>();
            
            List<Map<String, Object>> included = new ArrayList<>();
            
            // Version 데이터
            Map<String, Object> versionData = new HashMap<>();
            versionData.put("type", "versions");
            versionData.put("id", "1");
            
            Map<String, Object> versionAttributes = new HashMap<>();
            versionAttributes.put("name", fileName);
            versionData.put("attributes", versionAttributes);
            
            Map<String, Object> versionRelationships = new HashMap<>();
            Map<String, Object> storage = new HashMap<>();
            Map<String, Object> storageData = new HashMap<>();
            storageData.put("type", "objects");
            storageData.put("id", storageId);
            storage.put("data", storageData);
            versionRelationships.put("storage", storage);
            versionData.put("relationships", versionRelationships);
            
            included.add(versionData);
            
            // Item 데이터
            Map<String, Object> data = new HashMap<>();
            data.put("type", "items");
            
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("displayName", fileName);
            Map<String, String> extension = new HashMap<>();
            extension.put("type", "items:autodesk.core:File");
            extension.put("version", "1.0");
            attributes.put("extension", extension);
            data.put("attributes", attributes);
            
            Map<String, Object> relationships = new HashMap<>();
            Map<String, Object> tip = new HashMap<>();
            Map<String, Object> tipData = new HashMap<>();
            tipData.put("type", "versions");
            tipData.put("id", "1");
            tip.put("data", tipData);
            relationships.put("tip", tip);
            
            Map<String, Object> parent = new HashMap<>();
            Map<String, Object> parentData = new HashMap<>();
            parentData.put("type", "folders");
            parentData.put("id", projectId); // 임시 폴더 ID
            parent.put("data", parentData);
            relationships.put("parent", parent);
            
            data.put("relationships", relationships);
            
            payload.put("data", data);
            payload.put("included", included);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            String url = String.format(itemsUrl, projectId);
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> responseBody = mapper.readValue(response.getBody(), Map.class);
            
            // URN 생성을 위해 버전 ID 반환
            List<Map<String, Object>> includedResponse = (List<Map<String, Object>>) responseBody.get("included");
            if (includedResponse != null && !includedResponse.isEmpty()) {
                String versionId = (String) includedResponse.get(0).get("id");
                System.out.println("✅ 아이템 및 버전 생성 완료: " + versionId);
                return versionId;
            }
            
            throw new RuntimeException("버전 ID를 찾을 수 없습니다");
            
        } catch (Exception e) {
            System.out.println("❌ 아이템 생성 실패: " + e.getMessage());
            throw new RuntimeException("아이템 생성 실패! " + e.getMessage(), e);
        }
    }

    // -------------------- Model Derivative 변환 --------------------
    private String translateFile(String versionId, String accessToken) {
        try {
            String urnBase64 = Base64.getUrlEncoder().encodeToString(versionId.getBytes(StandardCharsets.UTF_8));

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            Map<String, Object> payload = new HashMap<>();
            Map<String, String> input = new HashMap<>();
            input.put("urn", urnBase64);
            payload.put("input", input);

            Map<String, Object> output = new HashMap<>();
            List<Map<String, Object>> formats = new ArrayList<>();
            Map<String, Object> svfFormat = new HashMap<>();
            svfFormat.put("type", "svf2");
            svfFormat.put("views", Arrays.asList("2d","3d"));
            formats.add(svfFormat);
            output.put("formats", formats);
            payload.put("output", output);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(translateUrl, request, String.class);

            ObjectMapper mapper = new ObjectMapper();
            Map<String,Object> responseBody = mapper.readValue(response.getBody(), Map.class);
            System.out.println("✅ 변환 요청 완료: " + responseBody);

            return urnBase64;
        } catch (Exception e) {
            System.out.println("❌ 파일 변환 실패: " + e.getMessage());
            throw new RuntimeException("파일 변환 실패! " + e.getMessage(), e);
        }
    }

    // -------------------- 간단한 방법: 임시 프로젝트 사용 --------------------
    @GetMapping("/auto-urn-simple")
    public Map<String, Object> getAutoUrnSimple(@RequestParam(defaultValue = "C:/CADSAMPLE/racadvancedsampleproject.rvt") String filePath) {
        Map<String, Object> result = new HashMap<>();
        
        System.out.println("❌ 죄송합니다. OSS API가 완전히 차단되었습니다.");
        System.out.println("💡 해결방법:");
        System.out.println("1. Autodesk Construction Cloud (ACC) 또는 BIM 360 계정 필요");
        System.out.println("2. Data Management API 사용 (프로젝트 기반)");
        System.out.println("3. 또는 다른 3D 뷰어 라이브러리 사용 고려");
        
        result.put("status", 503);
        result.put("message", "OSS API 완전 차단됨 (2024.12.10부터)");
        result.put("solution", "Autodesk Construction Cloud 계정 필요하거나 다른 3D 뷰어 사용");
        result.put("error", "Legacy endpoint is deprecated - All OSS endpoints blocked for new apps");
        
        return result;
    }

    // -------------------- 자동 업로드 + URN 반환 (Data Management API 필요) --------------------
    @GetMapping("/auto-urn")
    public Map<String, Object> getAutoUrn(@RequestParam(defaultValue = "C:/CADSAMPLE/racadvancedsampleproject.rvt") String filePath) {
        Map<String, Object> result = new HashMap<>();

        try {
            File file = new File(filePath);
            if (!file.exists()) {
                result.put("status", 404);
                result.put("message", "파일을 찾을 수 없습니다: " + filePath);
                return result;
            }

            System.out.println("⚠️  Data Management API 사용 시도 (실험적)");
            System.out.println("📋 주의: ACC/BIM 360 프로젝트 ID가 필요합니다");

            // 임시 프로젝트 ID (실제로는 ACC/BIM360에서 가져와야 함)
            String tempProjectId = "temp-project-id";
            
            String accessToken = getAccessToken();
            
            // 이 부분은 실제 프로젝트 없이는 작동하지 않습니다
            result.put("status", 501);
            result.put("message", "Data Management API는 실제 ACC/BIM 360 프로젝트가 필요합니다");
            result.put("requirement", "Autodesk Construction Cloud 또는 BIM 360 계정 필요");
            result.put("alternative", "Three.js, Babylon.js 등 다른 3D 뷰어 고려해보세요");

        } catch (Exception e) {
            System.out.println("💥 Data Management API 실패: " + e.getMessage());
            result.put("status", 500);
            result.put("message", "Data Management API 실패");
            result.put("error", e.getMessage());
        }

        return result;
    }
}