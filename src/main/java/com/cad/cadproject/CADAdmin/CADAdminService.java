package com.cad.cadproject.CADAdmin;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

@Service
public class CADAdminService {
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final String UPLOAD_DIR = "C:\\cadProjectNew\\my-react-app\\public\\cadfiles";
    private final String TEMP_DIR = "C:\\cadProjectNew\\my-react-app\\public\\tempDXF";

    // 파일 업로드 관련 메서드들
    public String uploadDXFFile(MultipartFile file, String modelId) throws IOException {
        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists()) uploadDir.mkdirs();

        File dest = new File(uploadDir, file.getOriginalFilename());
        file.transferTo(dest);

        System.out.println("DXF 파일 저장 완료: " + dest.getAbsolutePath());
        return "DXF 업로드 성공";
    }

    public String uploadDWFFile(MultipartFile file, String modelId) throws IOException {
        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists()) uploadDir.mkdirs();

        File dest = new File(uploadDir, file.getOriginalFilename());
        file.transferTo(dest);

        System.out.println("DWF 파일 저장 완료: " + dest.getAbsolutePath());
        return "DWF 업로드 성공";
    }

    // DXF 변환 및 내용 반환
    public String convertAndGetDxfContent(String fileName) {
        try {
            File tempDir = new File(TEMP_DIR);
            if (!tempDir.exists()) {
                boolean created = tempDir.mkdirs();
                System.out.println("📁 tempDir 생성 결과: " + created);
            }

            String tempDxfName = fileName.replaceAll("(?i)\\.dwf$", ".dxf");
            File tempDxfFile = new File(tempDir, tempDxfName);
            
            System.out.println("📁 임시 DXF 파일 전체 경로: " + tempDxfFile.getAbsolutePath());
            System.out.println("📁 임시 DXF 파일 존재여부: " + tempDxfFile.exists());

            // 이미 존재하면 변환 없이 파일 내용 반환
            if (tempDxfFile.exists()) {
                String existingContent = handleExistingDxfFile(tempDxfFile);
                if (existingContent != null) {
                    return existingContent;
                }
            }

            // 없으면 변환 수행
            return performDwfToDxfConversion(fileName, tempDxfFile);

        } catch (Exception e) {
            System.err.println("❌ convertAndGetDxfContent 오류: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("변환 및 읽기 중 오류 발생: " + e.getMessage());
        }
    }

    // 기존 DXF 파일 처리
    private String handleExistingDxfFile(File tempDxfFile) {
        System.out.println("✅ 기존 변환 파일 발견: " + tempDxfFile.getAbsolutePath());
        
        // 파일 완성도 검증
        if (!waitForFileCompletion(tempDxfFile, 10)) {
            System.out.println("❌ 파일이 아직 완성되지 않음, 삭제 후 재변환");
            boolean deleted = tempDxfFile.delete();
            System.out.println("🗑️ 파일 삭제 결과: " + deleted);
            return null;
        }

        try {
            String dxfContent = Files.readString(tempDxfFile.toPath(), StandardCharsets.UTF_8);
            System.out.println("📏 읽어온 DXF 내용 길이: " + dxfContent.length());
            
            // DXF 형식 검증
            if (validateDxfFormat(dxfContent)) {
                System.out.println("✅ 올바른 DXF 형식 확인됨");
                return dxfContent;
            } else {
                System.out.println("❌ DXF 형식이 아님, 파일 삭제 후 재변환");
                boolean deleted = tempDxfFile.delete();
                System.out.println("🗑️ 파일 삭제 결과: " + deleted);
                return null;
            }
        } catch (Exception e) {
            System.out.println("❌ 파일 읽기 오류: " + e.getMessage());
            e.printStackTrace();
            boolean deleted = tempDxfFile.delete();
            System.out.println("🗑️ 문제 파일 삭제 결과: " + deleted);
            return null;
        }
    }

    // DWF to DXF 변환 수행
    private String performDwfToDxfConversion(String fileName, File tempDxfFile) throws Exception {
        File dwfFile = new File(UPLOAD_DIR, fileName);
        System.out.println("📁 원본 DWF 파일 경로: " + dwfFile.getAbsolutePath());
        System.out.println("📁 원본 DWF 파일 존재여부: " + dwfFile.exists());
        
        if (!dwfFile.exists()) {
            System.out.println("❌ DWF 파일이 존재하지 않습니다: " + dwfFile.getAbsolutePath());
            throw new RuntimeException("404:DWF 파일이 존재하지 않습니다: " + dwfFile.getAbsolutePath());
        }

        String abviewerExe = "C:\\PROGRA~1\\CADSOF~1\\ABVIEW~1\\ABViewer.exe";
        String command = abviewerExe
                + " /c dxf"
                + " dir=\"" + new File(TEMP_DIR).getAbsolutePath() + "\""
                + " \"" + dwfFile.getAbsolutePath() + "\"";

        System.out.println("🔧 실행할 명령어: " + command);
        
        Process process = new ProcessBuilder("cmd.exe", "/c", command).start();
        int exitCode = process.waitFor();
        
        // 변환 완료 대기 시간 증가
        System.out.println("⏳ 변환 완료 대기 시작...");
        Thread.sleep(1500); 

        // 파일 생성 확인 및 추가 대기
        int waitCount = 0;
        while (!tempDxfFile.exists() && waitCount < 10) {
            Thread.sleep(500);
            waitCount++;
            System.out.println("🔄 변환 완료 대기 중... " + (waitCount * 500) + "ms");
        }
        
        System.out.println("🔧 변환 프로세스 종료 코드: " + exitCode);
        System.out.println("📋 변환 후 DXF 파일 존재 여부: " + tempDxfFile.exists());

        if (!tempDxfFile.exists()) {
            System.out.println("❌ DWF→DXF 변환 실패. 변환된 파일이 생성되지 않았습니다.");
            throw new RuntimeException("DWF→DXF 변환 실패. 변환된 파일이 생성되지 않았습니다.");
        }

        // 변환 완료 후 파일 완성도 검증
        if (!waitForFileCompletion(tempDxfFile, 20)) {
            System.out.println("❌ 변환된 파일이 완성되지 않았습니다.");
            throw new RuntimeException("변환된 파일이 완성되지 않았습니다.");
        }

        // 변환 완료 후 파일 내용 반환
        System.out.println("✅ 변환 완료, 파일 읽기 시작");
        String dxfContent = Files.readString(tempDxfFile.toPath(), StandardCharsets.UTF_8);
        System.out.println("📏 변환된 DXF 내용 길이: " + dxfContent.length());
        
        return dxfContent;
    }

    // 변환된 파일 체크
    public Map<String, Object> checkConvertedFiles(String fileName) {
        File tempDir = new File(TEMP_DIR);
        System.out.println("🔍 변환된 파일 체크 시작: " + TEMP_DIR);
        
        if (!tempDir.exists()) {
            System.out.println("📁 TEMP_DIR이 존재하지 않음");
            return Map.of("hasFiles", false);
        }
        
        // fileName 파라미터가 있으면 특정 파일만 체크
        if (fileName != null && !fileName.isEmpty()) {
            return checkSpecificFile(tempDir, fileName);
        }
        
        // fileName 파라미터가 없으면 기존 로직 (가장 최근 파일)
        return checkLatestFile(tempDir);
    }

    // 특정 파일 체크
    private Map<String, Object> checkSpecificFile(File tempDir, String fileName) {
        String targetDxfName = fileName.replaceAll("(?i)\\.dwf$", ".dxf");
        File targetFile = new File(tempDir, targetDxfName);
        
        System.out.println("🎯 특정 파일 체크: " + targetDxfName);
        
        if (targetFile.exists()) {
            if (isFileCompletelyGenerated(targetFile)) {
                System.out.println("✅ 요청된 파일 완성됨: " + targetDxfName);
                return Map.of(
                    "hasFiles", true,
                    "fileName", targetFile.getName(),
                    "fileSize", targetFile.length()
                );
            } else {
                System.out.println("⏳ 요청된 파일이 아직 생성 중: " + targetDxfName);
                return Map.of("hasFiles", false, "generating", true);
            }
        } else {
            System.out.println("❌ 요청된 파일 없음: " + targetDxfName);
            return Map.of("hasFiles", false);
        }
    }

    // 최신 파일 체크
    private Map<String, Object> checkLatestFile(File tempDir) {
        File[] dxfFiles = tempDir.listFiles((dir, name) -> 
            name.toLowerCase().endsWith(".dxf"));
        
        if (dxfFiles != null && dxfFiles.length > 0) {
            // 가장 최근 파일 찾기
            File latestFile = null;
            long latestTime = 0;
            
            for (File file : dxfFiles) {
                if (file.lastModified() > latestTime) {
                    latestTime = file.lastModified();
                    latestFile = file;
                }
            }
            
            if (latestFile != null) {
                if (isFileCompletelyGenerated(latestFile)) {
                    System.out.println("✅ 완전한 변환 파일 발견: " + latestFile.getName());
                    return Map.of(
                        "hasFiles", true,
                        "fileName", latestFile.getName(),
                        "fileSize", latestFile.length()
                    );
                } else {
                    System.out.println("⏳ 파일이 아직 생성 중: " + latestFile.getName());
                    return Map.of("hasFiles", false, "generating", true);
                }
            }
        }
        
        System.out.println("❌ 변환된 파일 없음");
        return Map.of("hasFiles", false);
    }

    // DXF 형식 검증
    private boolean validateDxfFormat(String dxfContent) {
        boolean hasSECTION = dxfContent.contains("SECTION");
        boolean hasHEADER = dxfContent.contains("HEADER");
        boolean startsWithZero = dxfContent.startsWith("0");
        
        System.out.println("📋 DXF 형식 검증:");
        System.out.println("   - SECTION 포함: " + hasSECTION);
        System.out.println("   - HEADER 포함: " + hasHEADER);
        System.out.println("   - '0'으로 시작: " + startsWithZero);
        
        return hasSECTION || hasHEADER || startsWithZero;
    }

    // 파일 락 체크
    private boolean isFileLocked(File file) {
        try (FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.WRITE)) {
            return false; // 락이 없으면 쓰기 가능
        } catch (IOException e) {
            System.out.println("🔒 파일 락 감지: " + file.getName());
            return true; // 락이 걸려있으면 IOException 발생
        }
    }

    // 파일 크기 안정성 체크
    private boolean isFileSizeStable(File file) {
        try {
            long size1 = file.length();
            System.out.println("📏 첫 번째 크기 체크: " + size1 + " bytes");
            Thread.sleep(2000); // 2초 대기
            long size2 = file.length();
            System.out.println("📏 두 번째 크기 체크: " + size2 + " bytes");
            
            boolean isStable = (size1 == size2 && size1 > 0);
            System.out.println("📊 크기 안정성: " + isStable);
            return isStable;
        } catch (InterruptedException e) {
            System.out.println("❌ 크기 안정성 체크 중단됨");
            return false;
        }
    }

    // 파일 완성 대기
    private boolean waitForFileCompletion(File file, int maxAttempts) {
        System.out.println("⏳ 파일 완성 대기 시작: " + file.getName() + ", 최대 시도: " + maxAttempts);
        
        for (int i = 0; i < maxAttempts; i++) {
            System.out.println("🔄 완성도 체크 시도 " + (i + 1) + "/" + maxAttempts);
            
            if (isFileCompletelyGenerated(file) && 
                !isFileLocked(file) && 
                isFileSizeStable(file)) {
                System.out.println("✅ 파일 완성 확인됨: " + file.getName());
                return true;
            }
            
            try {
                Thread.sleep(2000); // 2초씩 대기
                System.out.println("⏳ 2초 대기 후 재시도...");
            } catch (InterruptedException e) {
                System.out.println("❌ 대기 중단됨");
                return false;
            }
        }
        
        System.out.println("❌ 파일 완성 대기 시간 초과: " + file.getName());
        return false;
    }

    // 파일 완성도 체크
    private boolean isFileCompletelyGenerated(File file) {
        try {
            System.out.println("🔍 파일 완성도 체크 시작: " + file.getName());
            
            // 1. 파일 크기가 너무 작으면 불완전
            if (file.length() < 500) {
                System.out.println("❌ 파일 크기가 너무 작음: " + file.length() + " bytes");
                return false;
            }
            
            // 2. 파일이 최근 수정되었는지 확인 (3초 이내 수정되면 아직 생성 중일 수 있음)
            long timeSinceModified = System.currentTimeMillis() - file.lastModified();
            if (timeSinceModified < 3000) {
                System.out.println("❌ 파일이 최근에 수정됨: " + timeSinceModified + "ms 전");
                return false;
            }
            
            // 3. DXF 파일 형식 검증
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            
            // 4. 파일 헤더 체크 (CADSoftTools 소프트웨어로 생성됨을 확인)
            if (!content.contains("CADSoftTools")) {
                System.out.println("⚠️ CADSoftTools로 생성되지 않은 파일");
                return false;
            }
            
            // 5. ENTITIES 섹션이 있는지 확인
            if (!content.contains("ENTITIES")) {
                System.out.println("⚠️ ENTITIES 섹션이 없음");
                return false;
            }
            
            // 6. 파일이 완전한 구조를 가지는지 확인
            String[] requiredSections = {"HEADER", "TABLES", "BLOCKS", "ENTITIES", "OBJECTS"};
            for (String section : requiredSections) {
                if (!content.contains(section)) {
                    System.out.println("⚠️ 필수 섹션 누락: " + section);
                    return false;
                }
            }
            
            // 7. DXF 파일이 EOF로 올바르게 끝나는지 확인
            if (!content.trim().endsWith("EOF")) {
                System.out.println("⚠️ DXF 파일이 EOF로 끝나지 않음");
                return false;
            }
            
            // 8. 기본 DXF 구조 확인
            boolean hasSection = content.contains("SECTION");
            boolean hasEndsec = content.contains("ENDSEC");
            boolean hasEof = content.contains("EOF");
            
            if (!hasSection || !hasEndsec || !hasEof) {
                System.out.println("⚠️ DXF 기본 구조 누락 - SECTION: " + hasSection + 
                                 ", ENDSEC: " + hasEndsec + ", EOF: " + hasEof);
                return false;
            }
            
            System.out.println("✅ 파일 완성도 검증 완료: " + file.getName());
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ 파일 완성도 체크 오류: " + e.getMessage());
            return false;
        }
    }

    public boolean cleanupTempDxfFile(String fileName) {
        try {
            File tempDir = new File(TEMP_DIR);
            String tempDxfName = fileName.replaceAll("(?i)\\.dwf$", ".dxf");
            File tempDxfFile = new File(tempDir, tempDxfName);
            
            if (tempDxfFile.exists()) {
                boolean deleted = tempDxfFile.delete();
                System.out.println("임시 파일 삭제: " + tempDxfName + " -> " + deleted);
                return deleted;
            }
            return false;
            
        } catch (Exception e) {
            System.err.println("임시 파일 삭제 오류: " + e.getMessage());
            return false;
        }
    }
}