package com.cad.cadproject.CADArea;

import java.util.List;

public class CADAreaDTO {
    // 구역 기본 정보
    private String areaId;
    private String modelId;
    private String areaNm;
    private double areaSize;
    private String areaDesc;
    private String areaColor;
    private String areaStyle;
    private String regDdtm;
    private String regId;
    private String lstAdjDdtm;
    private String lstAdjId;
    
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

    // 내부 클래스 - 좌표 정보
    public static class CoordinatePoint {
        private String coordId;
        private String areaId;
        private int pointOrder;
        private double x;
        private double y;
        private String regDdtm;
        private String regId;
        private String lstAdjDdtm;
        private String lstAdjId;

        // 생성자
        public CoordinatePoint() {}

        public CoordinatePoint(int pointOrder, double x, double y) {
            this.pointOrder = pointOrder;
            this.x = x;
            this.y = y;
        }

        // Getter & Setter
        public String getCoordId() {
            return coordId;
        }

        public void setCoordId(String coordId) {
            this.coordId = coordId;
        }

        public String getAreaId() {
            return areaId;
        }

        public void setAreaId(String areaId) {
            this.areaId = areaId;
        }

        public int getPointOrder() {
            return pointOrder;
        }

        public void setPointOrder(int pointOrder) {
            this.pointOrder = pointOrder;
        }

        public double getX() {
            return x;
        }

        public void setX(double x) {
            this.x = x;
        }

        public double getY() {
            return y;
        }

        public void setY(double y) {
            this.y = y;
        }

        public String getRegDdtm() {
            return regDdtm;
        }

        public void setRegDdtm(String regDdtm) {
            this.regDdtm = regDdtm;
        }

        public String getRegId() {
            return regId;
        }

        public void setRegId(String regId) {
            this.regId = regId;
        }

        public String getLstAdjDdtm() {
            return lstAdjDdtm;
        }

        public void setLstAdjDdtm(String lstAdjDdtm) {
            this.lstAdjDdtm = lstAdjDdtm;
        }

        public String getLstAdjId() {
            return lstAdjId;
        }

        public void setLstAdjId(String lstAdjId) {
            this.lstAdjId = lstAdjId;
        }
    }

    // Getter & Setter - 구역 기본 정보
    public String getAreaId() {
        return areaId;
    }

    public void setAreaId(String areaId) {
        this.areaId = areaId;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getAreaNm() {
        return areaNm;
    }

    public void setAreaNm(String areaNm) {
        this.areaNm = areaNm;
    }

    public double getAreaSize() {
        return areaSize;
    }

    public void setAreaSize(double areaSize) {
        this.areaSize = areaSize;
    }

    public String getAreaDesc() {
        return areaDesc;
    }

    public void setAreaDesc(String areaDesc) {
        this.areaDesc = areaDesc;
    }

    public String getAreaColor() {
        return areaColor;
    }

    public void setAreaColor(String areaColor) {
        this.areaColor = areaColor;
    }

    public String getAreaStyle() {
        return areaStyle;
    }

    public void setAreaStyle(String areaStyle) {
        this.areaStyle = areaStyle;
    }

    public String getRegDdtm() {
        return regDdtm;
    }

    public void setRegDdtm(String regDdtm) {
        this.regDdtm = regDdtm;
    }

    public String getRegId() {
        return regId;
    }

    public void setRegId(String regId) {
        this.regId = regId;
    }

    public String getLstAdjDdtm() {
        return lstAdjDdtm;
    }

    public void setLstAdjDdtm(String lstAdjDdtm) {
        this.lstAdjDdtm = lstAdjDdtm;
    }

    public String getLstAdjId() {
        return lstAdjId;
    }

    public void setLstAdjId(String lstAdjId) {
        this.lstAdjId = lstAdjId;
    }

    // Getter & Setter - 좌표 리스트
    public List<CoordinatePoint> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(List<CoordinatePoint> coordinates) {
        this.coordinates = coordinates;
    }
}