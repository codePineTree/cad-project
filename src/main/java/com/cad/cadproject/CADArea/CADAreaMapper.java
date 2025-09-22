package com.cad.cadproject.CADArea;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface CADAreaMapper {

    /**
     * 구역 기본 정보 저장
     * @param area 구역 정보
     */
    void insertCadArea(CADAreaDTO area);

    /**
     * 좌표 정보 저장
     * @param coord 좌표 정보
     */
    void insertCadAreaCoord(CADAreaDTO.CoordinatePoint coord);

    /**
     * 마지막 생성된 구역 ID 조회
     * @return 마지막 구역 ID
     */
    String getLastInsertedAreaId();

    /**
     * 모델별 구역 목록 조회
     * @param modelId 모델 ID
     * @return 구역 목록
     */
    List<CADAreaDTO> selectAreasByModelId(@Param("modelId") String modelId);

    /**
     * 구역별 좌표 목록 조회
     * @param areaId 구역 ID
     * @return 좌표 목록
     */
    List<CADAreaDTO.CoordinatePoint> selectCoordsByAreaId(@Param("areaId") String areaId);

    /**
     * 구역 정보 수정
     * @param area 수정할 구역 정보
     */
    void updateArea(CADAreaDTO area);

    /**
     * 구역 정보 수정 (통합 API용)
     * @param area 수정할 구역 정보
     */
    void updateCadArea(CADAreaDTO area);

    /**
     * 구역별 좌표 삭제
     * @param areaId 구역 ID
     */
    void deleteCoordsByAreaId(@Param("areaId") String areaId);

    /**
     * 구역 삭제
     * @param areaId 구역 ID
     */
    void deleteAreaById(@Param("areaId") String areaId);

    /**
     * 구역별 좌표 개수 조회
     * @param areaId 구역 ID
     * @return 좌표 개수
     */
    int getCoordCountByAreaId(@Param("areaId") String areaId);

    /**
     * 모델별 구역 개수 조회
     * @param modelId 모델 ID
     * @return 구역 개수
     */
    int getAreaCountByModelId(@Param("modelId") String modelId);

    // ========== 전체 삭제를 위한 추가 메서드 ==========

    /**
     * 모델별 구역 ID 목록 조회 (전체 삭제용)
     * @param modelId 모델 ID
     * @return 구역 ID 목록
     */
    List<String> getAreaIdsByModelId(@Param("modelId") String modelId);

    /**
     * 모델별 전체 구역 삭제
     * @param modelId 모델 ID
     */
    void deleteAreasByModelId(@Param("modelId") String modelId);
    
    /**
     * 모델별 구역 목록 조회 (페이징)
     * @param modelId 모델 ID
     * @param offset 시작 위치
     * @param limit 조회 개수
     * @return 구역 목록
     */
    List<CADAreaDTO> selectAreaListByModelId(
        @Param("modelId") String modelId,
        @Param("offset") int offset,
        @Param("limit") int limit
    );

    /**
     * 모델별 구역 총 개수 조회
     * @param modelId 모델 ID
     * @return 총 구역 개수
     */
    int getTotalAreaCountByModelId(@Param("modelId") String modelId);
}