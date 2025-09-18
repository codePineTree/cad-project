package com.cad.cadproject.CADArea;

import lombok.Getter;
import lombok.Setter;
import java.util.Date;
import java.util.List;

@Getter
@Setter
public class CADAreaDTO {
    // 구역 기본 정보
    private String areaId;
    private String modelId;
    private String areaNm;
    private double areaSize;
    private String areaDesc;
    private String areaColor;
    private String areaStyle;
    private Date regDdtm;
    private String regId;
    private Date lstAdjDdtm;
    private String lstAdjId;

    // 구역 상태 (화면에서 넘어오는 DrawingStatus)
    private String drawingStatus;

    // 구역 좌표 리스트
    private List<CoordinatePoint> coordinates;

    // 생성자
    public CADAreaDTO() {}

    public CADAreaDTO(String modelId, String areaNm, String areaDesc, String areaColor) {
        this.modelId = modelId;
        this.areaNm = areaNm;
        this.areaDesc = areaDesc;
        this.areaColor = areaColor;
    }

    @Getter
    @Setter
    public static class CoordinatePoint {
        private String coordId;
        private String areaId;
        private int pointOrder;
        private double x;
        private double y;
        private Date regDdtm;
        private String regId;
        private Date lstAdjDdtm;
        private String lstAdjId;

        // 생성자
        public CoordinatePoint() {}

        public CoordinatePoint(int pointOrder, double x, double y) {
            this.pointOrder = pointOrder;
            this.x = x;
            this.y = y;
        }
    }
}
