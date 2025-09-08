package com.cad.cadproject.CADDrawing;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface CADDrawingMapper {

    // ---------------- 조회 ----------------
    List<Map<String, Object>> getCadModelList();

    // ---------------- 추가 ----------------
    int insertCadModel(Map<String, Object> modelData);

    // ---------------- 수정 ----------------
    int updateCadModel(Map<String, Object> modelData);

    // ---------------- 삭제 ----------------
    int deleteCadModel(String modelId);
    

}
