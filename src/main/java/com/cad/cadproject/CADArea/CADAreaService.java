package com.cad.cadproject.CADArea;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CADAreaService {

    @Autowired
    private CADAreaMapper cadAreaMapper;

    @Transactional
    public String saveAreaWithCoordinates(CADAreaDTO areaData) {
        try {
            String drawingStatus = areaData.getDrawingStatus();

            if ("I".equals(drawingStatus)) {
                // === INSERT 로직 ===
                System.out.println(">>> INSERT 구역 생성 - ModelId: " + areaData.getModelId());

                cadAreaMapper.insertCadArea(areaData);
                String newAreaId = cadAreaMapper.getLastInsertedAreaId();
                if (newAreaId == null || newAreaId.trim().isEmpty()) {
                    throw new RuntimeException("AREA_ID 생성 실패");
                }

                // 좌표 저장
                List<CADAreaDTO.CoordinatePoint> coordinates = areaData.getCoordinates();
                if (coordinates != null && !coordinates.isEmpty()) {
                    for (int i = 0; i < coordinates.size(); i++) {
                        CADAreaDTO.CoordinatePoint coord = coordinates.get(i);
                        coord.setAreaId(newAreaId);
                        coord.setPointOrder(i + 1);
                        cadAreaMapper.insertCadAreaCoord(coord);
                    }
                }

                System.out.println(">>> INSERT 완료 - 새 AreaId: " + newAreaId);
                return newAreaId;

            } else if ("D".equals(drawingStatus)) {
                // === DELETE 로직 ===
                String areaId = areaData.getAreaId();
                if (areaId == null || areaId.trim().isEmpty()) {
                    throw new RuntimeException("삭제할 AREA_ID가 없습니다");
                }

                if ("ALL".equals(areaId)) {
                    // 전체 삭제
                    String modelId = areaData.getModelId();
                    if (modelId == null || modelId.trim().isEmpty()) {
                        throw new RuntimeException("전체 삭제 시 MODEL_ID가 필요합니다");
                    }

                    System.out.println(">>> DELETE ALL 전체 삭제 - ModelId: " + modelId);

                    // 1. 해당 모델의 모든 구역 ID 조회
                    List<String> areaIds = cadAreaMapper.getAreaIdsByModelId(modelId);

                    if (areaIds != null && !areaIds.isEmpty()) {
                        // 2. 각 구역의 좌표들 삭제
                        for (String id : areaIds) {
                            cadAreaMapper.deleteCoordsByAreaId(id);
                        }
                        // 3. 모든 구역 삭제
                        cadAreaMapper.deleteAreasByModelId(modelId);
                        
                        System.out.println(">>> DELETE ALL 완료 - ModelId: " + modelId + ", 삭제된 구역 수: " + areaIds.size());
                    } else {
                        System.out.println(">>> 삭제할 구역이 없음 - ModelId: " + modelId);
                    }

                    return "ALL_DELETED";

                } else {
                    // 개별 삭제
                    System.out.println(">>> DELETE 개별 삭제 - AreaId: " + areaId);

                    // 1. 좌표들 먼저 삭제 (FK 제약조건 때문에)
                    cadAreaMapper.deleteCoordsByAreaId(areaId);

                    // 2. 구역 삭제
                    cadAreaMapper.deleteAreaById(areaId);

                    System.out.println(">>> DELETE 완료 - AreaId: " + areaId);
                    return areaId;
                }

            } else {
                System.out.println("⚠️ 지원하지 않는 DrawingStatus: " + drawingStatus);
                return null;
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("구역 처리 중 오류 발생: " + e.getMessage(), e);
        }
    }

    /**
     * 좌표 저장 공통 메서드
     */
    private void insertCoordinates(String areaId, List<CADAreaDTO.CoordinatePoint> coordinates) {
        if (coordinates != null && !coordinates.isEmpty()) {
            for (int i = 0; i < coordinates.size(); i++) {
                CADAreaDTO.CoordinatePoint coord = coordinates.get(i);
                coord.setAreaId(areaId);
                coord.setPointOrder(i + 1);
                cadAreaMapper.insertCadAreaCoord(coord);
            }
            System.out.println(">>> 좌표 " + coordinates.size() + "개 저장 완료 - AreaId: " + areaId);
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