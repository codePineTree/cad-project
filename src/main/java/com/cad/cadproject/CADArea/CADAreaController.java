package com.cad.cadproject.CADArea;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cad/area")
public class CADAreaController {

    @Autowired
    private CADAreaService cadAreaService;

    /**
     * 구역 통합 처리 API (저장/수정/삭제)
     * drawingStatus에 따라 다른 작업 수행:
     * - 'I': INSERT (새로운 구역 생성)
     * - 'U': UPDATE (기존 구역 수정)
     * - 'D': DELETE (구역 삭제 - areaId가 "ALL"이면 전체 삭제)
     * 
     * @param areaData 구역 기본 정보 + 좌표 배열 + drawingStatus
     * @return 처리 결과
     */
    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> saveArea(@RequestBody CADAreaDTO areaData) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String drawingStatus = areaData.getDrawingStatus();
            System.out.println(">>> 구역 처리 요청 - DrawingStatus: " + drawingStatus + ", ModelId: " + areaData.getModelId());
            
            if (drawingStatus == null || drawingStatus.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "drawingStatus는 필수값입니다. (I/U/D)");
                return ResponseEntity.badRequest().body(response);
            }
            
            String result = cadAreaService.saveAreaWithCoordinates(areaData);
            
            // 결과에 따른 응답 메시지 설정
            String message = "";
            switch (drawingStatus.toUpperCase()) {
                case "I":
                    message = "새 구역이 성공적으로 생성되었습니다.";
                    break;
                case "U": 
                    message = "구역 정보가 성공적으로 수정되었습니다.";
                    break;
                case "D":
                    if ("ALL".equals(areaData.getAreaId())) {
                        message = "모든 구역이 성공적으로 삭제되었습니다.";
                    } else {
                        message = "구역이 성공적으로 삭제되었습니다.";
                    }
                    break;
                default:
                    message = "처리가 완료되었습니다.";
            }
            
            response.put("success", true);
            response.put("areaId", result);
            response.put("message", message);
            response.put("operation", drawingStatus.toUpperCase());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "구역 처리 중 오류가 발생했습니다: " + e.getMessage());
            response.put("drawingStatus", areaData.getDrawingStatus());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 모델별 구역 조회 API
     * @param modelId CAD 모델 ID
     * @return 해당 모델의 모든 구역 정보 (좌표 포함)
     */
    @GetMapping("/list/{modelId}")
    public ResponseEntity<Map<String, Object>> getAreasByModel(@PathVariable String modelId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<CADAreaDTO> areas = cadAreaService.getAreasByModelId(modelId);
            
            response.put("success", true);
            response.put("areas", areas);
            response.put("count", areas.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "구역 조회 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 구역 정보 수정 API (기존 방식 - 호환성 유지)
     * @param areaId 수정할 구역 ID
     * @param updateData 수정할 구역 정보
     * @return 수정 결과
     */
    @PutMapping("/update/{areaId}")
    public ResponseEntity<Map<String, Object>> updateArea(
            @PathVariable String areaId, 
            @RequestBody CADAreaDTO updateData) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            updateData.setAreaId(areaId);
            updateData.setDrawingStatus("U"); // UPDATE 상태로 설정
            
            // 통합 API 호출
            cadAreaService.saveAreaWithCoordinates(updateData);
            
            response.put("success", true);
            response.put("message", "구역 정보가 성공적으로 수정되었습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "구역 수정 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }


}