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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
        System.out.println("📂 fileName 길이: " + (fileName != null ? fileName.length() : "null"));
        System.out.println("📂 fileName 타입: " + (fileName != null ? fileName.getClass().getSimpleName() : "null"));
        
        try {
            File tempDir = new File(TEMP_DIR);
            System.out.println("📁 TEMP_DIR 상수: " + TEMP_DIR);
            System.out.println("📁 tempDir 절대경로: " + tempDir.getAbsolutePath());
            System.out.println("📁 tempDir 존재여부: " + tempDir.exists());
            
            if (!tempDir.exists()) {
                System.out.println("📁 tempDir 생성 시도");
                boolean created = tempDir.mkdirs();
                System.out.println("📁 tempDir 생성 결과: " + created);
            }

            String tempDxfName = fileName.replaceAll("(?i)\\.dwf$", ".dxf");
            System.out.println("📁 변환된 DXF 파일명: '" + tempDxfName + "'");
            
            File tempDxfFile = new File(tempDir, tempDxfName);
            System.out.println("📁 임시 DXF 파일 전체 경로: " + tempDxfFile.getAbsolutePath());
            System.out.println("📁 임시 DXF 파일 존재여부: " + tempDxfFile.exists());
            
            if (tempDxfFile.exists()) {
                System.out.println("📁 파일 크기: " + tempDxfFile.length() + " bytes");
                System.out.println("📁 파일 읽기 가능: " + tempDxfFile.canRead());
            }

            // 이미 존재하면 변환 없이 파일 내용 반환
            if (tempDxfFile.exists()) {
                System.out.println("✅ 기존 변환 파일 발견: " + tempDxfFile.getAbsolutePath());
                System.out.println("📏 파일 크기: " + tempDxfFile.length() + " bytes");
                
                try {
                    String dxfContent = Files.readString(tempDxfFile.toPath(), StandardCharsets.UTF_8);
                    System.out.println("📏 읽어온 DXF 내용 길이: " + dxfContent.length());
                    System.out.println("📄 DXF 내용 시작 100자: " + dxfContent.substring(0, Math.min(100, dxfContent.length())));
                    
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
                        System.out.println("📤 Raw String 응답 전송 시작 - 데이터 길이: " + dxfContent.length());
                        
                        response.setContentType("text/plain; charset=UTF-8");
                        response.setHeader("Cache-Control", "no-cache");
                        response.setStatus(200);
                        
                        System.out.println("📤 Raw String 응답 생성 완료");
                        System.out.println("=== 서버 디버깅 끝 (기존 파일 반환) ===");
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

            // 없으면 변환 수행
            File dwfFile = new File(UPLOAD_DIR, fileName);
            System.out.println("📁 UPLOAD_DIR 상수: " + UPLOAD_DIR);
            System.out.println("📁 원본 DWF 파일 경로: " + dwfFile.getAbsolutePath());
            System.out.println("📁 원본 DWF 파일 존재여부: " + dwfFile.exists());
            
            if (dwfFile.exists()) {
                System.out.println("📁 원본 DWF 파일 크기: " + dwfFile.length() + " bytes");
                System.out.println("📁 원본 DWF 파일 읽기 가능: " + dwfFile.canRead());
            }
            
            if (!dwfFile.exists()) {
                System.out.println("❌ DWF 파일이 존재하지 않습니다: " + dwfFile.getAbsolutePath());
                System.out.println("📤 404 응답 전송 시작");
                response.setStatus(404);
                response.setContentType("text/plain; charset=UTF-8");
                System.out.println("📤 404 Raw String 응답 생성 완료");
                System.out.println("=== 서버 디버깅 끝 (파일 없음) ===");
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

            if (tempDxfFile.exists()) {
                System.out.println("📁 변환 후 파일 크기: " + tempDxfFile.length() + " bytes");
            }

            if (!tempDxfFile.exists()) {
                System.out.println("❌ DWF→DXF 변환 실패. 변환된 파일이 생성되지 않았습니다.");
                System.out.println("📤 500 응답 전송 시작");
                response.setStatus(500);
                response.setContentType("text/plain; charset=UTF-8");
                System.out.println("📤 500 Raw String 응답 생성 완료");
                System.out.println("=== 서버 디버깅 끝 (변환 실패) ===");
                return "DWF→DXF 변환 실패. 변환된 파일이 생성되지 않았습니다.";
            }

            // 변환 완료 후 파일 내용 반환 - Raw String 응답
            System.out.println("✅ 변환 완료, 파일 읽기 시작");
            String dxfContent = Files.readString(tempDxfFile.toPath(), StandardCharsets.UTF_8);
            System.out.println("📏 변환된 DXF 내용 길이: " + dxfContent.length());
            System.out.println("📄 DXF 내용 시작 100자: " + dxfContent.substring(0, Math.min(100, dxfContent.length())));
            
            System.out.println("📤 Raw String 응답 전송 시작 - 데이터 길이: " + dxfContent.length());
            
            response.setContentType("text/plain; charset=UTF-8");
            response.setHeader("Cache-Control", "no-cache");
            response.setStatus(200);
            
            System.out.println("📤 Raw String 응답 생성 완료");
            System.out.println("=== 서버 디버깅 끝 (변환 성공) ===");
            return dxfContent;

        } catch (Exception e) {
            System.err.println("❌ convertAndGetDxf 오류: " + e.getMessage());
            e.printStackTrace();
            System.out.println("📤 예외 500 응답 전송 시작");
            response.setStatus(500);
            response.setContentType("text/plain; charset=UTF-8");
            System.out.println("📤 예외 500 Raw String 응답 생성 완료");
            System.out.println("=== 서버 디버깅 끝 (예외 발생) ===");
            return "변환 및 읽기 중 오류 발생: " + e.getMessage();
        }
    }

}
