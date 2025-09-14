package com.cad.cadproject.CADAdmin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cad")
public class CADAdminController {

    private final String UPLOAD_DIR = "C:\\cadProjectNew\\my-react-app\\public\\cadfiles";
    private final String TEMP_DIR = "C:\\cadProjectNew\\my-react-app\\public\\tempDXF";

    @Autowired
    private CADAdminService cadAdminService;

    @PostMapping("/uploadDXF")
    public ResponseEntity<?> uploadDXF(@RequestParam("file") MultipartFile file,
                                       @RequestParam("modelId") String modelId) {
        try {
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) uploadDir.mkdirs();

            File dest = new File(uploadDir, file.getOriginalFilename());
            file.transferTo(dest);

            System.out.println("DXF 파일 저장 완료: " + dest.getAbsolutePath());
            return ResponseEntity.ok("DXF 업로드 성공");

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("DXF 업로드 실패: " + e.getMessage());
        }
    }

    @PostMapping("/uploadDWF")
    public ResponseEntity<?> uploadDWF(@RequestParam("file") MultipartFile file,
                                       @RequestParam("modelId") String modelId) {
        try {
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) uploadDir.mkdirs();

            File dest = new File(uploadDir, file.getOriginalFilename());
            file.transferTo(dest);

            System.out.println("DWF 파일 저장 완료: " + dest.getAbsolutePath());
            return ResponseEntity.ok("DWF 업로드 성공");

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("DWF 업로드 실패: " + e.getMessage());
        }
    }

    @GetMapping("/convertDwfToDxf")
    public ResponseEntity<?> convertDwfToDxf(@RequestParam("fileName") String fileName) {
        try {
            File dwfFile = new File(UPLOAD_DIR, fileName);
            if (!dwfFile.exists()) {
                System.out.println("📄 DWF 파일 없음: " + dwfFile.getAbsolutePath());
                return ResponseEntity.status(404)
                    .body("DWF 파일이 존재하지 않습니다: " + fileName);
            }

            File tempDir = new File(TEMP_DIR);
            if (!tempDir.exists()) tempDir.mkdirs();

            String tempDxfName = fileName.replaceAll("(?i)\\.dwf$", ".dxf");
            File tempDxfFile = new File(tempDir, tempDxfName);

            System.out.println("🔹 변환될 DXF 파일 경로: " + tempDxfFile.getAbsolutePath());

            // ABViewer 실행 경로 (8.3 단축명 사용 - 공백 문제 해결)
            String abviewerExe = "C:\\PROGRA~1\\CADSOF~1\\ABVIEW~1\\ABViewer.exe";

            // 실행할 전체 명령어 문자열
            String command = abviewerExe
                    + " /c dxf"
                    + " dir=\"" + tempDir.getAbsolutePath() + "\""
                    + " \"" + dwfFile.getAbsolutePath() + "\"";

            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", command);
            pb.redirectErrorStream(true);

            System.out.println("💻 CMD를 통한 ABViewer 실행 시작...");
            System.out.println("🔧 실행 명령어: " + command);

            Process process = pb.start();

            // CLI 출력 실시간 확인
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "MS949"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("💬 CMD 출력: " + line);
                }
            }

            int exitCode = process.waitFor();
            System.out.println("✅ CMD 명령어 종료, exit code: " + exitCode);

            // DXF 파일이 생성되었는지 확인 (약간의 지연 후)
            Thread.sleep(1000);

            if (exitCode != 0 || !tempDxfFile.exists()) {
                // tempDir에서 생성된 DXF 파일 찾기
                File[] dxfFiles = tempDir.listFiles((dir, name) ->
                        name.toLowerCase().endsWith(".dxf"));

                if (dxfFiles != null && dxfFiles.length > 0) {
                    tempDxfFile = dxfFiles[0]; // 첫 번째 DXF 사용
                    tempDxfName = tempDxfFile.getName();
                    System.out.println("🔍 발견된 DXF 파일: " + tempDxfFile.getAbsolutePath());
                } else {
                    String msg = "DWF→DXF 변환 실패, exit code: " + exitCode;
                    System.out.println(msg);
                    return ResponseEntity.status(500).body(msg);
                }
            }

            System.out.println("✅ DXF 생성 완료: " + tempDxfFile.getAbsolutePath());
            InputStreamResource resource = new InputStreamResource(new FileInputStream(tempDxfFile));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + tempDxfName)
                    .contentLength(tempDxfFile.length())
                    .contentType(MediaType.parseMediaType("application/dxf"))
                    .body(resource);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("변환 중 오류 발생: " + e.getMessage());
        }
    }

    @GetMapping(value = "/convertAndGetDxf", produces = "text/plain;charset=UTF-8")
    public String convertAndGetDxf(@RequestParam("fileName") String fileName, HttpServletResponse response) {
        System.out.println("=== 서버 파일 경로 디버깅 시작 ===");
        System.out.println("🔥 convertAndGetDxf 호출됨");
        System.out.println("📂 받은 fileName 파라미터: '" + fileName + "'");
        
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
                System.out.println("✅ 기존 변환 파일 발견: " + tempDxfFile.getAbsolutePath());
                
                // 파일 완성도 검증
                if (!waitForFileCompletion(tempDxfFile, 10)) {
                    System.out.println("❌ 파일이 아직 완성되지 않음, 삭제 후 재변환");
                    boolean deleted = tempDxfFile.delete();
                    System.out.println("🗑️ 파일 삭제 결과: " + deleted);
                } else {
                    try {
                        String dxfContent = Files.readString(tempDxfFile.toPath(), StandardCharsets.UTF_8);
                        System.out.println("📏 읽어온 DXF 내용 길이: " + dxfContent.length());
                        
                        // DXF 형식 검증
                        boolean hasSECTION = dxfContent.contains("SECTION");
                        boolean hasHEADER = dxfContent.contains("HEADER");
                        boolean startsWithZero = dxfContent.startsWith("0");
                        
                        System.out.println("📋 DXF 형식 검증:");
                        System.out.println("   - SECTION 포함: " + hasSECTION);
                        System.out.println("   - HEADER 포함: " + hasHEADER);
                        System.out.println("   - '0'으로 시작: " + startsWithZero);
                        
                        if (hasSECTION || hasHEADER || startsWithZero) {
                            System.out.println("✅ 올바른 DXF 형식 확인됨");
                            
                            response.setContentType("text/plain; charset=UTF-8");
                            response.setHeader("Cache-Control", "no-cache");
                            response.setStatus(200);
                            
                            System.out.println("📤 Raw String 응답 생성 완료");
                            return dxfContent;
                        } else {
                            System.out.println("❌ DXF 형식이 아님, 파일 삭제 후 재변환");
                            boolean deleted = tempDxfFile.delete();
                            System.out.println("🗑️ 파일 삭제 결과: " + deleted);
                        }
                    } catch (Exception e) {
                        System.out.println("❌ 파일 읽기 오류: " + e.getMessage());
                        e.printStackTrace();
                        boolean deleted = tempDxfFile.delete();
                        System.out.println("🗑️ 문제 파일 삭제 결과: " + deleted);
                    }
                }
            }

            // 없으면 변환 수행
            File dwfFile = new File(UPLOAD_DIR, fileName);
            System.out.println("📁 원본 DWF 파일 경로: " + dwfFile.getAbsolutePath());
            System.out.println("📁 원본 DWF 파일 존재여부: " + dwfFile.exists());
            
            if (!dwfFile.exists()) {
                System.out.println("❌ DWF 파일이 존재하지 않습니다: " + dwfFile.getAbsolutePath());
                response.setStatus(404);
                response.setContentType("text/plain; charset=UTF-8");
                return "DWF 파일이 존재하지 않습니다: " + dwfFile.getAbsolutePath();
            }

            String abviewerExe = "C:\\PROGRA~1\\CADSOF~1\\ABVIEW~1\\ABViewer.exe";
            String command = abviewerExe
                    + " /c dxf"
                    + " dir=\"" + tempDir.getAbsolutePath() + "\""
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
                response.setStatus(500);
                response.setContentType("text/plain; charset=UTF-8");
                return "DWF→DXF 변환 실패. 변환된 파일이 생성되지 않았습니다.";
            }

            // 변환 완료 후 파일 완성도 검증
            if (!waitForFileCompletion(tempDxfFile, 20)) {
                System.out.println("❌ 변환된 파일이 완성되지 않았습니다.");
                response.setStatus(500);
                response.setContentType("text/plain; charset=UTF-8");
                return "변환된 파일이 완성되지 않았습니다.";
            }

            // 변환 완료 후 파일 내용 반환
            System.out.println("✅ 변환 완료, 파일 읽기 시작");
            String dxfContent = Files.readString(tempDxfFile.toPath(), StandardCharsets.UTF_8);
            System.out.println("📏 변환된 DXF 내용 길이: " + dxfContent.length());
            
            response.setContentType("text/plain; charset=UTF-8");
            response.setHeader("Cache-Control", "no-cache");
            response.setStatus(200);
            
            System.out.println("📤 Raw String 응답 생성 완료");
            return dxfContent;

        } catch (Exception e) {
            System.err.println("❌ convertAndGetDxf 오류: " + e.getMessage());
            e.printStackTrace();
            response.setStatus(500);
            response.setContentType("text/plain; charset=UTF-8");
            return "변환 및 읽기 중 오류 발생: " + e.getMessage();
        }
    }

    @GetMapping("/checkConvertedFiles")
    public ResponseEntity<?> checkConvertedFiles() {
        try {
            File tempDir = new File(TEMP_DIR);
            System.out.println("🔍 변환된 파일 체크 시작: " + TEMP_DIR);
            
            if (!tempDir.exists()) {
                System.out.println("📁 TEMP_DIR이 존재하지 않음");
                return ResponseEntity.ok().body(Map.of("hasFiles", false));
            }
            
            // .dxf 파일들 찾기
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
                    // 파일 완성도 검증
                    if (isFileCompletelyGenerated(latestFile)) {
                        System.out.println("✅ 완전한 변환 파일 발견: " + latestFile.getName());
                        return ResponseEntity.ok().body(Map.of(
                            "hasFiles", true,
                            "fileName", latestFile.getName(),
                            "fileSize", latestFile.length()
                        ));
                    } else {
                        System.out.println("⏳ 파일이 아직 생성 중: " + latestFile.getName());
                        return ResponseEntity.ok().body(Map.of("hasFiles", false, "generating", true));
                    }
                }
            }
            
            System.out.println("❌ 변환된 파일 없음");
            return ResponseEntity.ok().body(Map.of("hasFiles", false));
            
        } catch (Exception e) {
            System.err.println("❌ 파일 체크 오류: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("파일 체크 중 오류 발생: " + e.getMessage());
        }
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

    private boolean isFileCompletelyGenerated(File file) {
        try {
            System.out.println("🔍 파일 완성도 체크 시작: " + file.getName());
            
            // 1. 파일 크기가 너무 작으면 불완전
            if (file.length() < 500) {
                System.out.println("❌ 파일 크기가 너무 작음: " + file.length() + " bytes");
                return false;
            }
            
            // 2. 파일이 최근 수정되었는지 확인 (15초 이내 수정되면 아직 생성 중일 수 있음)
            long timeSinceModified = System.currentTimeMillis() - file.lastModified();
            if (timeSinceModified < 5000) {
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
}