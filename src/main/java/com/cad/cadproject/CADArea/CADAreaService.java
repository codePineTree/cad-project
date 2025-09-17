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
            // === 입력 데이터 검증 및 로그 출력 ===
            System.out.println("=== 구역 저장 시작 - 입력 데이터 검증 ===");
            System.out.println("modelId: [" + areaData.getModelId() + "] (길이: " + 
                (areaData.getModelId() != null ? areaData.getModelId().length() : "null") + ")");
            System.out.println("areaNm: [" + areaData.getAreaNm() + "] (길이: " + 
                (areaData.getAreaNm() != null ? areaData.getAreaNm().length() : "null") + ")");
            System.out.println("areaDesc: [" + areaData.getAreaDesc() + "] (길이: " + 
                (areaData.getAreaDesc() != null ? areaData.getAreaDesc().length() : "null") + ")");
            System.out.println("areaColor: [" + areaData.getAreaColor() + "] (길이: " + 
                (areaData.getAreaColor() != null ? areaData.getAreaColor().length() : "null") + ")");
            System.out.println("areaSize: [" + areaData.getAreaSize() + "] (타입: double)");
            System.out.println("areaStyle: [" + areaData.getAreaStyle() + "] (길이: " + 
                (areaData.getAreaStyle() != null ? areaData.getAreaStyle().length() : "null") + ")");
            
            List<CADAreaDTO.CoordinatePoint> coordinates = areaData.getCoordinates();
            System.out.println("좌표 개수: " + (coordinates != null ? coordinates.size() : "null"));
            
            // === 1. 구역 기본 정보 저장 ===
            System.out.println("=== CAD_AREA 테이블 저장 시도 ===");
            cadAreaMapper.insertCadArea(areaData);
            System.out.println("✅ CAD_AREA 저장 성공");

            // === 2. 생성된 구역 ID 조회 ===
            System.out.println("=== 생성된 AREA_ID 조회 시도 ===");
            String newAreaId = cadAreaMapper.getLastInsertedAreaId();
            System.out.println("생성된 AREA_ID: [" + newAreaId + "]");
            
            if (newAreaId == null || newAreaId.trim().isEmpty()) {
                throw new RuntimeException("AREA_ID 생성 실패 - null 또는 빈 값");
            }

            // === 3. 좌표들 저장 ===
            if (coordinates != null && !coordinates.isEmpty()) {
                System.out.println("=== 좌표 저장 시작 (" + coordinates.size() + "개) ===");
                
                for (int i = 0; i < coordinates.size(); i++) {
                    CADAreaDTO.CoordinatePoint coord = coordinates.get(i);
                    coord.setAreaId(newAreaId);
                    coord.setPointOrder(i + 1); // 1부터 시작하는 순서
                    
                    System.out.println("좌표 " + (i+1) + " 저장 시도:");
                    System.out.println("  - areaId: [" + coord.getAreaId() + "]");
                    System.out.println("  - pointOrder: [" + coord.getPointOrder() + "]");
                    System.out.println("  - x: [" + coord.getX() + "] (타입: double)");
                    System.out.println("  - y: [" + coord.getY() + "] (타입: double)");
                    
                    try {
                        cadAreaMapper.insertCadAreaCoord(coord);
                        System.out.println("✅ 좌표 " + (i+1) + " 저장 성공");
                    } catch (Exception coordEx) {
                        System.out.println("❌ 좌표 " + (i+1) + " 저장 실패: " + coordEx.getMessage());
                        coordEx.printStackTrace();
                        throw coordEx; // 예외 재전파
                    }
                }
                System.out.println("✅ 모든 좌표 저장 완료");
            } else {
                System.out.println("⚠️ 저장할 좌표 없음");
            }

            System.out.println("=== 구역 저장 완료 - AREA_ID: " + newAreaId + " ===");
            return newAreaId;

        } catch (Exception e) {
            System.out.println("=== 구역 저장 실패 상세 정보 ===");
            System.out.println("Error Message: " + e.getMessage());
            System.out.println("Error Class: " + e.getClass().getSimpleName());
            e.printStackTrace();
            
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
            System.out.println("=== 구역 조회 시작: " + modelId + " ===");
            
            // 1. 해당 모델의 구역들 조회
            List<CADAreaDTO> areas = cadAreaMapper.selectAreasByModelId(modelId);
            System.out.println("조회된 구역 개수: " + areas.size());

            // 2. 각 구역의 좌표들 조회 및 설정
            for (CADAreaDTO area : areas) {
                System.out.println("구역 " + area.getAreaId() + "의 좌표 조회 시작");
                List<CADAreaDTO.CoordinatePoint> coordinates = cadAreaMapper.selectCoordsByAreaId(area.getAreaId());
                System.out.println("조회된 좌표 개수: " + coordinates.size());
                area.setCoordinates(coordinates);
                System.out.println("구역 " + area.getAreaId() + " 좌표 설정 완료");
            }

            System.out.println("=== 구역 조회 완료 ===");
            return areas;

        } catch (Exception e) {
            System.out.println("=== 구역 조회 실패 ===");
            e.printStackTrace();
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