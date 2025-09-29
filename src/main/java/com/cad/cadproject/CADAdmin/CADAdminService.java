package com.cad.cadproject.CADAdmin;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

// Aspose CAD import
import com.aspose.cad.Image;
import com.aspose.cad.fileformats.cad.CadImage;
import com.aspose.cad.fileformats.cad.cadobjects.*;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CADAdminService {
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final String UPLOAD_DIR = "C:\\cadProjectNew\\my-react-app\\public\\cadfiles";
    private final String TEMP_DIR = "C:\\cadProjectNew\\my-react-app\\public\\tempDXF";

    // ==================== 기존 파일 업로드 관련 메서드들 ====================
    
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

    // ==================== 기존 DXF 변환 및 내용 반환 ====================
    
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

            if (tempDxfFile.exists()) {
                String existingContent = handleExistingDxfFile(tempDxfFile);
                if (existingContent != null) {
                    return existingContent;
                }
            }

            return performDwfToDxfConversion(fileName, tempDxfFile);

        } catch (Exception e) {
            System.err.println("❌ convertAndGetDxfContent 오류: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("변환 및 읽기 중 오류 발생: " + e.getMessage());
        }
    }

    private String handleExistingDxfFile(File tempDxfFile) {
        System.out.println("✅ 기존 변환 파일 발견: " + tempDxfFile.getAbsolutePath());
        
        if (!waitForFileCompletion(tempDxfFile, 10)) {
            System.out.println("❌ 파일이 아직 완성되지 않음, 삭제 후 재변환");
            boolean deleted = tempDxfFile.delete();
            System.out.println("🗑️ 파일 삭제 결과: " + deleted);
            return null;
        }

        try {
            String dxfContent = Files.readString(tempDxfFile.toPath(), StandardCharsets.UTF_8);
            System.out.println("📏 읽어온 DXF 내용 길이: " + dxfContent.length());
            
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
        
        System.out.println("⏳ 변환 완료 대기 시작...");
        Thread.sleep(1500); 

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

        if (!waitForFileCompletion(tempDxfFile, 20)) {
            System.out.println("❌ 변환된 파일이 완성되지 않았습니다.");
            throw new RuntimeException("변환된 파일이 완성되지 않았습니다.");
        }

        System.out.println("✅ 변환 완료, 파일 읽기 시작");
        String dxfContent = Files.readString(tempDxfFile.toPath(), StandardCharsets.UTF_8);
        System.out.println("📏 변환된 DXF 내용 길이: " + dxfContent.length());
        
        return dxfContent;
    }

    // ==================== 변환된 파일 체크 ====================
    
    public Map<String, Object> checkConvertedFiles(String fileName) {
        File tempDir = new File(TEMP_DIR);
        System.out.println("🔍 변환된 파일 체크 시작: " + TEMP_DIR);
        
        if (!tempDir.exists()) {
            System.out.println("📁 TEMP_DIR이 존재하지 않음");
            return Map.of("hasFiles", false);
        }
        
        if (fileName != null && !fileName.isEmpty()) {
            return checkSpecificFile(tempDir, fileName);
        }
        
        return checkLatestFile(tempDir);
    }

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

    private Map<String, Object> checkLatestFile(File tempDir) {
        File[] dxfFiles = tempDir.listFiles((dir, name) -> 
            name.toLowerCase().endsWith(".dxf"));
        
        if (dxfFiles != null && dxfFiles.length > 0) {
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

    // ==================== 파일 검증 관련 메서드들 ====================
    
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

    private boolean isFileLocked(File file) {
        try (FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.WRITE)) {
            return false;
        } catch (IOException e) {
            System.out.println("🔒 파일 락 감지: " + file.getName());
            return true;
        }
    }

    private boolean isFileSizeStable(File file) {
        try {
            long size1 = file.length();
            System.out.println("📏 첫 번째 크기 체크: " + size1 + " bytes");
            Thread.sleep(2000);
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
                Thread.sleep(2000);
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
            
            if (file.length() < 500) {
                System.out.println("❌ 파일 크기가 너무 작음: " + file.length() + " bytes");
                return false;
            }
            
            long timeSinceModified = System.currentTimeMillis() - file.lastModified();
            if (timeSinceModified < 3000) {
                System.out.println("❌ 파일이 최근에 수정됨: " + timeSinceModified + "ms 전");
                return false;
            }
            
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            
            if (!content.contains("CADSoftTools")) {
                System.out.println("⚠️ CADSoftTools로 생성되지 않은 파일");
                return false;
            }
            
            if (!content.contains("ENTITIES")) {
                System.out.println("⚠️ ENTITIES 섹션이 없음");
                return false;
            }
            
            String[] requiredSections = {"HEADER", "TABLES", "BLOCKS", "ENTITIES", "OBJECTS"};
            for (String section : requiredSections) {
                if (!content.contains(section)) {
                    System.out.println("⚠️ 필수 섹션 누락: " + section);
                    return false;
                }
            }
            
            if (!content.trim().endsWith("EOF")) {
                System.out.println("⚠️ DXF 파일이 EOF로 끝나지 않음");
                return false;
            }
            
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

    // ==================== 파일 삭제 관련 메서드들 ====================
    
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

    public boolean deleteFile(String fileName) {
        try {
            File uploadDir = new File(UPLOAD_DIR);
            File file = new File(uploadDir, fileName);
            
            System.out.println("파일 삭제 시도: " + file.getAbsolutePath());
            System.out.println("파일 존재 여부: " + file.exists());
            
            if (file.exists()) {
                boolean deleted = file.delete();
                System.out.println("파일 삭제 결과: " + deleted);
                return deleted;
            } else {
                System.out.println("삭제할 파일이 존재하지 않음: " + fileName);
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("파일 삭제 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

 // ==================== Aspose CAD 파싱 관련 메서드 ====================

    public Map<String, Object> parseCadFileWithAspose(String fileName) {
        try {
            File cadFile = new File(UPLOAD_DIR, fileName);
            
            if (!cadFile.exists()) {
                throw new RuntimeException("파일이 존재하지 않습니다: " + fileName);
            }
            
            System.out.println("Aspose로 파일 파싱 시작: " + cadFile.getAbsolutePath());
            
            // 파일 확장자로 타입 구분
            String extension = getFileExtension(fileName);
            
            com.aspose.cad.Image image = com.aspose.cad.Image.load(cadFile.getAbsolutePath());
            
            if (extension.equals("dwf")) {
                // DWF 파일 처리
                com.aspose.cad.fileformats.dwf.DwfImage dwfImage = 
                    (com.aspose.cad.fileformats.dwf.DwfImage) image;
                return parseDwfImage(dwfImage, fileName);
            } else {
                // DXF 파일 처리
                com.aspose.cad.fileformats.cad.CadImage cadImage =
                    (com.aspose.cad.fileformats.cad.CadImage) image;
                return parseCadImage(cadImage, fileName);
            }
            
        } catch (Exception e) {
            System.err.println("Aspose 파싱 실패: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("CAD 파일 파싱 중 오류 발생: " + e.getMessage());
        }
    }

    // DXF 파일 파싱
    private Map<String, Object> parseCadImage(com.aspose.cad.fileformats.cad.CadImage cadImage, String fileName) {
        List<Map<String, Object>> entities = new ArrayList<>();
        
        for (com.aspose.cad.fileformats.cad.cadobjects.CadEntityBase entity : cadImage.getEntities()) {
            try {
                Map<String, Object> entityData = parseEntity(entity);
                if (entityData != null) {
                    entities.add(entityData);
                }
            } catch (Exception e) {
                System.err.println("개별 엔티티 파싱 오류: " + e.getMessage());
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("entities", entities);
        result.put("width", cadImage.getWidth());
        result.put("height", cadImage.getHeight());
        result.put("fileType", "dxf");
        result.put("entityCount", entities.size());
        
        System.out.println("DXF 파싱 완료 - 엔티티 수: " + entities.size());
        
        return result;
    }

    // DWF 파일 파싱
    private Map<String, Object> parseDwfImage(com.aspose.cad.fileformats.dwf.DwfImage dwfImage, String fileName) {
        List<Map<String, Object>> entities = new ArrayList<>();
        
        System.out.println("DWF 페이지 수: " + dwfImage.getPages().length);
        
        for (com.aspose.cad.fileformats.dwf.DwfPage page : dwfImage.getPages()) {
            System.out.println("페이지 파싱: " + page.getName());
            
            try {
                // 페이지의 엔티티 추출
                Object[] pageEntities = page.getEntities();
                
                if (pageEntities != null) {
                    System.out.println("페이지 엔티티 수: " + pageEntities.length);
                    
                    for (Object obj : pageEntities) {
                        try {
                            Map<String, Object> entityData = parseDwfEntity(obj);
                            if (entityData != null) {
                                entities.add(entityData);
                            }
                        } catch (Exception e) {
                            System.err.println("DWF 엔티티 파싱 오류: " + e.getMessage());
                        }
                    }
                }
                
            } catch (Exception e) {
                System.err.println("페이지 파싱 오류: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("entities", entities);
        result.put("width", dwfImage.getWidth());
        result.put("height", dwfImage.getHeight());
        result.put("fileType", "dwf");
        result.put("entityCount", entities.size());
        result.put("pageCount", dwfImage.getPages().length);
        
        System.out.println("DWF 파싱 완료 - 엔티티 수: " + entities.size());
        
        return result;
    }

    // DWF 엔티티 파싱
    private Map<String, Object> parseDwfEntity(Object entity) {
        String className = entity.getClass().getSimpleName();
        
        Map<String, Object> data = new HashMap<>();
        data.put("type", className);
        
        System.out.println("DWF 엔티티 타입: " + className);
        
        try {
            // DwfWhipPolyline
            if (entity instanceof com.aspose.cad.fileformats.dwf.whip.objects.drawable.DwfWhipPolyline) {
                com.aspose.cad.fileformats.dwf.whip.objects.drawable.DwfWhipPolyline polyline = 
                    (com.aspose.cad.fileformats.dwf.whip.objects.drawable.DwfWhipPolyline) entity;
                
                com.aspose.cad.fileformats.dwf.whip.objects.DwfWhipLogicalPoint[] points = polyline.getPoints();
                
                if (points != null && points.length > 0) {
                    List<Map<String, Double>> pointsList = new ArrayList<>();
                    
                    for (com.aspose.cad.fileformats.dwf.whip.objects.DwfWhipLogicalPoint point : points) {
                        Map<String, Double> p = new HashMap<>();
                        p.put("x", (double) point.getX());
                        p.put("y", (double) point.getY());
                        pointsList.add(p);
                    }
                    
                    data.put("points", pointsList);
                    data.put("pointCount", points.length);
                    System.out.println("DwfWhipPolyline 파싱 완료 - 포인트 수: " + points.length);
                }
            }
            
            // DwfWhipPolygon
            else if (entity instanceof com.aspose.cad.fileformats.dwf.whip.objects.drawable.DwfWhipPolygon) {
                com.aspose.cad.fileformats.dwf.whip.objects.drawable.DwfWhipPolygon polygon = 
                    (com.aspose.cad.fileformats.dwf.whip.objects.drawable.DwfWhipPolygon) entity;
                
                com.aspose.cad.fileformats.dwf.whip.objects.DwfWhipLogicalPoint[] points = polygon.getPoints();
                
                if (points != null && points.length > 0) {
                    List<Map<String, Double>> pointsList = new ArrayList<>();
                    
                    for (com.aspose.cad.fileformats.dwf.whip.objects.DwfWhipLogicalPoint point : points) {
                        Map<String, Double> p = new HashMap<>();
                        p.put("x", (double) point.getX());
                        p.put("y", (double) point.getY());
                        pointsList.add(p);
                    }
                    
                    data.put("points", pointsList);
                    data.put("pointCount", points.length);
                    data.put("closed", true);
                    System.out.println("DwfWhipPolygon 파싱 완료 - 포인트 수: " + points.length);
                }
            }
            
            // DwfWhipOutlineEllipse
            else if (entity instanceof com.aspose.cad.fileformats.dwf.whip.objects.drawable.DwfWhipOutlineEllipse) {
                com.aspose.cad.fileformats.dwf.whip.objects.drawable.DwfWhipOutlineEllipse ellipse = 
                    (com.aspose.cad.fileformats.dwf.whip.objects.drawable.DwfWhipOutlineEllipse) entity;
                
                com.aspose.cad.fileformats.dwf.whip.objects.DwfWhipLogicalPoint center = ellipse.getPosition();
                
                data.put("centerX", (double) center.getX());
                data.put("centerY", (double) center.getY());
                data.put("majorRadius", (double) ellipse.getMajor());
                data.put("minorRadius", (double) ellipse.getMinor());
                data.put("rotation", (double) ellipse.getRotation());
                
                System.out.println("DwfWhipOutlineEllipse 파싱 완료");
            }
            
            // DwfWhipFilledEllipse
            else if (entity instanceof com.aspose.cad.fileformats.dwf.whip.objects.drawable.DwfWhipFilledEllipse) {
                com.aspose.cad.fileformats.dwf.whip.objects.drawable.DwfWhipFilledEllipse ellipse = 
                    (com.aspose.cad.fileformats.dwf.whip.objects.drawable.DwfWhipFilledEllipse) entity;
                
                com.aspose.cad.fileformats.dwf.whip.objects.DwfWhipLogicalPoint center = ellipse.getPosition();
                
                data.put("centerX", (double) center.getX());
                data.put("centerY", (double) center.getY());
                data.put("majorRadius", (double) ellipse.getMajor());
                data.put("minorRadius", (double) ellipse.getMinor());
                data.put("rotation", (double) ellipse.getRotation());
                data.put("filled", true);
                
                System.out.println("DwfWhipFilledEllipse 파싱 완료");
            }
            
            // DwfWhipViewPort
            else if (entity instanceof com.aspose.cad.fileformats.dwf.whip.objects.DwfWhipViewPort) {
                System.out.println("DwfWhipViewPort 감지 - 스킵");
                return null;
            }
            
        } catch (Exception e) {
            System.err.println(className + " 파싱 실패: " + e.getMessage());
            e.printStackTrace();
        }
        
        return data;
    }

    // DXF 엔티티 파싱
    private Map<String, Object> parseEntity(com.aspose.cad.fileformats.cad.cadobjects.CadEntityBase entity) {
        String className = entity.getClass().getSimpleName();
        
        Map<String, Object> data = new HashMap<>();
        data.put("type", className);
        
        try {
            // Line
            if (className.equals("CadLine")) {
                com.aspose.cad.fileformats.cad.cadobjects.CadLine line = 
                    (com.aspose.cad.fileformats.cad.cadobjects.CadLine) entity;
                data.put("startX", line.getFirstPoint().getX());
                data.put("startY", line.getFirstPoint().getY());
                data.put("endX", line.getSecondPoint().getX());
                data.put("endY", line.getSecondPoint().getY());
            }
            
            // Circle
            else if (className.equals("CadCircle")) {
                com.aspose.cad.fileformats.cad.cadobjects.CadCircle circle = 
                    (com.aspose.cad.fileformats.cad.cadobjects.CadCircle) entity;
                data.put("centerX", circle.getCenterPoint().getX());
                data.put("centerY", circle.getCenterPoint().getY());
                data.put("radius", circle.getRadius());
            }
            
            // Arc
            else if (className.equals("CadArc")) {
                com.aspose.cad.fileformats.cad.cadobjects.CadArc arc = 
                    (com.aspose.cad.fileformats.cad.cadobjects.CadArc) entity;
                data.put("centerX", arc.getCenterPoint().getX());
                data.put("centerY", arc.getCenterPoint().getY());
                data.put("radius", arc.getRadius());
                data.put("startAngle", arc.getStartAngle());
                data.put("endAngle", arc.getEndAngle());
            }
            
            // LWPolyline
            else if (className.equals("CadLwPolyline")) {
                com.aspose.cad.fileformats.cad.cadobjects.CadLwPolyline lwPolyline = 
                    (com.aspose.cad.fileformats.cad.cadobjects.CadLwPolyline) entity;
                
                List<Map<String, Double>> points = new ArrayList<>();
                List<com.aspose.cad.fileformats.cad.cadobjects.Cad2DPoint> coordinates = 
                    lwPolyline.getCoordinates();
                
                if (coordinates != null) {
                    for (com.aspose.cad.fileformats.cad.cadobjects.Cad2DPoint point : coordinates) {
                        Map<String, Double> p = new HashMap<>();
                        p.put("x", point.getX());
                        p.put("y", point.getY());
                        points.add(p);
                    }
                }
                data.put("points", points);
                data.put("closed", lwPolyline.getFlag() == 1);
            }
            
            // Text
            else if (className.equals("CadText")) {
                com.aspose.cad.fileformats.cad.cadobjects.CadText text = 
                    (com.aspose.cad.fileformats.cad.cadobjects.CadText) entity;
                data.put("text", text.getDefaultValue());
                data.put("x", text.getFirstAlignment().getX());
                data.put("y", text.getFirstAlignment().getY());
                data.put("height", text.getTextHeight());
            }
            
            // MText
            else if (className.equals("CadMText")) {
                com.aspose.cad.fileformats.cad.cadobjects.CadMText mtext = 
                    (com.aspose.cad.fileformats.cad.cadobjects.CadMText) entity;
                data.put("text", mtext.getText());
                data.put("x", mtext.getInsertionPoint().getX());
                data.put("y", mtext.getInsertionPoint().getY());
                data.put("height", mtext.getVerticalHeight());
            }
            
            System.out.println("파싱 성공: " + className);
            
        } catch (Exception e) {
            System.err.println(className + " 파싱 실패: " + e.getMessage());
        }
        
        return data;
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            return fileName.substring(lastDot + 1).toLowerCase();
        }
        return "unknown";
    }
    }




