package com.cad.cadproject.CADAdmin;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;

@Service
public class CADAdminService {

    private final RestTemplate restTemplate = new RestTemplate();

    public String convertDwfToDxf(String filePath) {
        File dwfFile = new File(filePath);

        System.out.println("파일 절대 경로: " + dwfFile.getAbsolutePath());
        System.out.println("파일 존재 여부: " + dwfFile.exists());
        System.out.println("파일 읽기 가능 여부: " + dwfFile.canRead());
        System.out.println("파일 바이트 길이: " + (dwfFile.exists() ? dwfFile.length() : 0));

        if (!dwfFile.exists() || !dwfFile.canRead() || dwfFile.length() == 0) {
            System.out.println("문제 원인 1: 파일이 없거나 읽을 수 없거나 파일 크기가 0입니다.");
            throw new RuntimeException("DWF 파일을 읽을 수 없습니다.");
        }

        try {
            // Multipart 요청 준비
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(dwfFile) {
                @Override
                public String getFilename() {
                    return dwfFile.getName(); // 확실히 파일명 전달
                }
            });

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            System.out.println("GroupDocs API에 업로드 시도...");

            // GroupDocs API 호출
            String apiUrl = "https://api.groupdocs.app/conversion/upload";
            String response = restTemplate.postForObject(apiUrl, requestEntity, String.class);

            if (response == null || response.isEmpty()) {
                System.out.println("문제 원인 2: API 응답이 비어있습니다.");
            } else {
                System.out.println("GroupDocs API 응답: " + response);
            }

            return response;
        } catch (Exception e) {
            System.out.println("DWF -> DXF 변환 중 예외 발생");

            if (e.getMessage().contains("No files found in multipart request")) {
                System.out.println("문제 원인 3: Multipart 키 이름 또는 업로드 방식이 API 요구사항과 맞지 않습니다.");
            }

            e.printStackTrace();
            throw new RuntimeException("DWF -> DXF 변환 중 오류 발생", e);
        }
    }
}
