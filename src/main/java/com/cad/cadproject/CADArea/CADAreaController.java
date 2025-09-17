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
     * 구역 저장 API
     * @param areaData 구역 기본 정보 + 좌표 배열
     * @return 저장된 구역 ID
     */
    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> saveArea(@RequestBody CADAreaDTO areaData) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String savedAreaId = cadAreaService.saveAreaWithCoordinates(areaData);
            // --- 여기서 model_id 확인 로그 ---
            System.out.println(">>> Received model_id: " + areaData.getModelId());
            response.put("success", true);
            response.put("areaId", savedAreaId);
            response.put("message", "구역이 성공적으로 저장되었습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "구역 저장 중 오류가 발생했습니다: " + e.getMessage());
            
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
     * 구역 삭제 API
     * @param areaId 삭제할 구역 ID
     * @return 삭제 결과
     */
    @DeleteMapping("/delete/{areaId}")
    public ResponseEntity<Map<String, Object>> deleteArea(@PathVariable String areaId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            cadAreaService.deleteAreaWithCoordinates(areaId);
            
            response.put("success", true);
            response.put("message", "구역이 성공적으로 삭제되었습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "구역 삭제 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 구역 정보 수정 API
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
            cadAreaService.updateArea(updateData);
            
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