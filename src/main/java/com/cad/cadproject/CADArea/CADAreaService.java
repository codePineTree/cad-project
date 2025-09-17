package com.cad.cadproject.CADArea;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CADAreaService {

    @Autowired
    private CADAreaMapper cadAreaMapper;

    /**
     * 구역과 좌표를 함께 저장하는 트랜잭션 메서드
     * @param areaData 구역 기본 정보 + 좌표 배열
     * @return 저장된 구역 ID
     */
    @Transactional
    public String saveAreaWithCoordinates(CADAreaDTO areaData) {
        try {
            // 1. 구역 기본 정보 저장
            cadAreaMapper.insertCadArea(areaData);
            
            // 2. 생성된 구역 ID 조회 (자동 채번된 값)
            String newAreaId = cadAreaMapper.getLastInsertedAreaId();
            
            // 3. 좌표들 저장
            List<CADAreaDTO.CoordinatePoint> coordinates = areaData.getCoordinates();
            if (coordinates != null && !coordinates.isEmpty()) {
                for (int i = 0; i < coordinates.size(); i++) {
                    CADAreaDTO.CoordinatePoint coord = coordinates.get(i);
                    coord.setAreaId(newAreaId);
                    coord.setPointOrder(i + 1); // 1부터 시작하는 순서
                    
                    cadAreaMapper.insertCadAreaCoord(coord);
                }
            }
            
            return newAreaId;
            
        } catch (Exception e) {
            throw new RuntimeException("구역 저장 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 모델 ID로 구역 목록 조회 (좌표 포함)
     * @param modelId CAD 모델 ID
     * @return 구역 목록 (좌표 포함)
     */
    public List<CADAreaDTO> getAreasByModelId(String modelId) {
        try {
            // 1. 해당 모델의 구역들 조회
            List<CADAreaDTO> areas = cadAreaMapper.selectAreasByModelId(modelId);
            
            // 2. 각 구역의 좌표들 조회 및 설정
            for (CADAreaDTO area : areas) {
                List<CADAreaDTO.CoordinatePoint> coordinates = cadAreaMapper.selectCoordsByAreaId(area.getAreaId());
                area.setCoordinates(coordinates);
            }
            
            return areas;
            
        } catch (Exception e) {
            throw new RuntimeException("구역 조회 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 구역 삭제 (좌표도 함께 삭제)
     * @param areaId 삭제할 구역 ID
     */
    @Transactional
    public void deleteAreaWithCoordinates(String areaId) {
        try {
            // 1. 좌표들 먼저 삭제 (FK 제약조건 때문에)
            cadAreaMapper.deleteCoordsByAreaId(areaId);
            
            // 2. 구역 삭제
            cadAreaMapper.deleteAreaById(areaId);
            
        } catch (Exception e) {
            throw new RuntimeException("구역 삭제 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 구역 정보 수정 (좌표는 수정하지 않음)
     * @param updateData 수정할 구역 정보
     */
    public void updateArea(CADAreaDTO updateData) {
        try {
            cadAreaMapper.updateArea(updateData);
            
        } catch (Exception e) {
            throw new RuntimeException("구역 수정 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 구역 면적 계산 (Shoelace 공식)
     * @param coordinates 좌표 배열
     * @return 계산된 면적
     */
    public double calculatePolygonArea(List<CADAreaDTO.CoordinatePoint> coordinates) {
        if (coordinates.size() < 3) {
            return 0.0;
        }
        
        double area = 0.0;
        int n = coordinates.size();
        
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            area += coordinates.get(i).getX() * coordinates.get(j).getY();
            area -= coordinates.get(j).getX() * coordinates.get(i).getY();
        }
        
        return Math.abs(area / 2.0);
    }
}