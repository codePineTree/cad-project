package com.cad.cadproject.CADAdmin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cad")
public class CADAdminController {

    @Autowired
    private CADAdminService cadAdminService;

    @PostMapping("/uploadDXF")
    public ResponseEntity<?> uploadDXF(@RequestParam("file") MultipartFile file,
                                       @RequestParam("modelId") String modelId) {
        try {
            String result = cadAdminService.uploadDXFFile(file, modelId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("DXF 업로드 실패: " + e.getMessage());
        }
    }

    @PostMapping("/uploadDWF")
    public ResponseEntity<?> uploadDWF(@RequestParam("file") MultipartFile file,
                                       @RequestParam("modelId") String modelId) {
        try {
            String result = cadAdminService.uploadDWFFile(file, modelId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("DWF 업로드 실패: " + e.getMessage());
        }
    }

    @GetMapping(value = "/convertAndGetDxf", produces = "text/plain;charset=UTF-8")
    public String convertAndGetDxf(@RequestParam("fileName") String fileName, HttpServletResponse response) {
        System.out.println("=== 서버 파일 경로 디버깅 시작 ===");
        System.out.println("🔥 convertAndGetDxf 호출됨");
        System.out.println("📂 받은 fileName 파라미터: '" + fileName + "'");
        
        try {
            String result = cadAdminService.convertAndGetDxfContent(fileName);
            
            response.setContentType("text/plain; charset=UTF-8");
            response.setHeader("Cache-Control", "no-cache");
            response.setStatus(200);
            
            System.out.println("📤 Raw String 응답 생성 완료");
            return result;

        } catch (RuntimeException e) {
            System.err.println("❌ convertAndGetDxf 오류: " + e.getMessage());
            e.printStackTrace();
            
            if (e.getMessage().contains("404")) {
                response.setStatus(404);
            } else {
                response.setStatus(500);
            }
            response.setContentType("text/plain; charset=UTF-8");
            return e.getMessage();
        } catch (Exception e) {
            System.err.println("❌ convertAndGetDxf 예상치 못한 오류: " + e.getMessage());
            e.printStackTrace();
            response.setStatus(500);
            response.setContentType("text/plain; charset=UTF-8");
            return "변환 및 읽기 중 오류 발생: " + e.getMessage();
        }
    }

    @GetMapping("/checkConvertedFiles")
    public ResponseEntity<?> checkConvertedFiles(@RequestParam(value = "fileName", required = false) String fileName) {
        try {
            Map<String, Object> result = cadAdminService.checkConvertedFiles(fileName);
            return ResponseEntity.ok().body(result);
        } catch (Exception e) {
            System.err.println("❌ 파일 체크 오류: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("파일 체크 중 오류 발생: " + e.getMessage());
        }
    }
    @DeleteMapping("/cleanupTempFile")
    public ResponseEntity<Map<String, Object>> cleanupTempFile(@RequestParam("fileName") String fileName) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean deleted = cadAdminService.cleanupTempDxfFile(fileName);
            
            response.put("success", deleted);
            response.put("message", deleted ? "임시 파일 삭제 완료" : "삭제할 파일이 없음");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "임시 파일 삭제 중 오류: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    } 
}