package com.cad.cadproject.CADDrawing;

import com.cad.cadproject.CADAdmin.CADAdminMapper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

@Service
public class CADDrawingService {

    private final CADDrawingMapper drawingMapper;
    private final CADAdminMapper adminMapper;

    // 실제 파일 저장 경로
    private final String UPLOAD_DIR = "C:/cadProjectNew/cadProjectNew/my-react-app/public/cadfiles/";

    public CADDrawingService(CADDrawingMapper drawingMapper, CADAdminMapper adminMapper) {
        this.drawingMapper = drawingMapper;
        this.adminMapper = adminMapper;
    }

    // ---------------- 조회 ----------------
    public List<Map<String, Object>> getCadModelList() {
        return drawingMapper.getCadModelList();
    }

    // ---------------- 저장 ----------------
    public Map<String, Object> saveCadModels(List<Map<String, Object>> modelList) {
        int addCnt = 0;
        int updateCnt = 0;
        int deleteCnt = 0;

        if (modelList != null) {
            for (Map<String, Object> row : modelList) {
                String status = (String) row.get("RowStatus"); // I / U / D
                String filePath = (String) row.get("FILE_PATH"); // 새 파일명

                if ("I".equals(status)) {
                    // 신규 저장
                    drawingMapper.insertCadModel(row);
                    addCnt++;
                    System.out.println("추가중 MODEL_ID: " + row.get("MODEL_ID"));

                } else if ("U".equals(status)) {
                    // 기존 파일 경로 조회
                    String oldFilePath = adminMapper.getFilePathByModelId((String) row.get("MODEL_ID"));

                    // DB 업데이트
                    drawingMapper.updateCadModel(row);
                    updateCnt++;
                    System.out.println("수정중 MODEL_ID: " + row.get("MODEL_ID"));

                    // 기존 파일 삭제 (새 파일과 다를 때만)
                    if (oldFilePath != null && !oldFilePath.equals(filePath)) {
                        File oldFile = new File(UPLOAD_DIR + oldFilePath);
                        if (oldFile.exists() && oldFile.isFile()) {
                            boolean deleted = oldFile.delete();
                            System.out.println("기존 파일 삭제 " + (deleted ? "성공" : "실패") + ": " + oldFile.getAbsolutePath());
                        }
                    }

                    // 새 파일 적용 (프론트에서 업로드 후 FILE_PATH 넘겨준 상태)
                    System.out.println("새 파일 적용: " + UPLOAD_DIR + filePath);

                } else if ("D".equals(status)) {
                    // DB 삭제
                    drawingMapper.deleteCadModel((String) row.get("MODEL_ID"));
                    deleteCnt++;
                    System.out.println("삭제중 MODEL_ID: " + row.get("MODEL_ID"));

                    // 파일 삭제
                    if (filePath != null) {
                        File file = new File(UPLOAD_DIR + filePath);
                        if (file.exists() && file.isFile()) {
                            boolean deleted = file.delete();
                            System.out.println("파일 삭제 " + (deleted ? "성공" : "실패") + ": " + file.getAbsolutePath());
                        }
                    }
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("I", addCnt);
        result.put("U", updateCnt);
        result.put("D", deleteCnt);
        return result;
    }
}
