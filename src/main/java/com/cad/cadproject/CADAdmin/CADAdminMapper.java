package com.cad.cadproject.CADAdmin;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CADAdminMapper {
    // MODEL_ID로 기존 파일 경로 조회
    String getFilePathByModelId(String modelId);
}
