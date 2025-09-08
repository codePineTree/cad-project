package com.cad.cadproject.CADDrawing;

import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cad/models")
public class CADDrawingController {

    private final CADDrawingService service;

    public CADDrawingController(CADDrawingService service) {
        this.service = service;
    }

    // ---------------- 조회 ----------------
    @PostMapping("/getCadModelList")
    public List<Map<String, Object>> getCadModelList(@RequestBody Map<String, Object> request) {
        return service.getCadModelList();
    }

    // ---------------- 저장 ----------------
    @PostMapping("/saveCadModelList")
    public Map<String, Object> saveCadModels(@RequestBody List<Map<String, Object>> modelList) {
        return service.saveCadModels(modelList);
    }
}
