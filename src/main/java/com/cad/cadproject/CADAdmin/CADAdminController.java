package com.cad.cadproject.CADAdmin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/cad")
public class CADAdminController {

    private final String UPLOAD_DIR = "C:/cadProjectNew/cadProjectNew/my-react-app/public/cadfiles/";

    @Autowired
    private CADAdminService cadAdminService;

    @PostMapping("/uploadDXF")
    public ResponseEntity<?> uploadDXF(@RequestParam("file") MultipartFile file,
                                       @RequestParam("modelId") String modelId) {
        try {
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) uploadDir.mkdirs();

            File dest = new File(UPLOAD_DIR + file.getOriginalFilename());
            file.transferTo(dest);

            System.out.println("DXF 파일 저장 완료: " + dest.getAbsolutePath());
            return ResponseEntity.ok("DXF 업로드 성공");
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("DXF 업로드 실패");
        }
    }

    @PostMapping("/uploadDWF")
    public ResponseEntity<?> uploadDWF(@RequestParam("file") MultipartFile file,
                                       @RequestParam("modelId") String modelId) {
        try {
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) uploadDir.mkdirs();

            File dest = new File(UPLOAD_DIR + file.getOriginalFilename());
            file.transferTo(dest);

            System.out.println("DWF 파일 저장 완료: " + dest.getAbsolutePath());
            return ResponseEntity.ok("DWF 업로드 성공");
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("DWF 업로드 실패");
        }
    }

    @GetMapping("/convertDwfToDxf")
    public ResponseEntity<String> convertDwfToDxf(@RequestParam String fileName) {
        try {
            File dwfFile = Paths.get(UPLOAD_DIR, fileName).toFile();

            if (!dwfFile.exists()) {
                return ResponseEntity.status(404).body("파일 없음");
            }

            // GroupDocs API에 업로드할 때 파일 경로 전달
            String dxfFileUrl = cadAdminService.convertDwfToDxf(dwfFile.getAbsolutePath());

            // 변환 후 URL 그대로 클라이언트에 반환
            return ResponseEntity.ok(dxfFileUrl);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("DWF 변환 실패: " + e.getMessage());
        }
    }



}
