package com.cad.cadproject.CADAdmin;

import java.io.File;
import java.io.IOException;

public class ODAConverter {

    private static final String ODA_CONVERTER_PATH = "C:/Program Files/ODA/ODAFileConverter 26.7.0/ODAFileConverter.exe";

    /**
     * DWF 파일을 DXF로 변환
     * @param inputFile 원본 DWF 파일
     * @param outputFile 최종 DXF 파일
     * @throws IOException 변환 실패 시
     * @throws InterruptedException 프로세스 실패 시
     */
    public static void convert(File inputFile, File outputFile) throws IOException, InterruptedException {
        // 1. ODAConverter 명령어 구성
        // 예: ODAFileConverter.exe inputFolder outputFolder version format
        // format: DXF  / version: 예를 들어 ACAD2018
        String inputFolder = inputFile.getParent();
        String outputFolder = outputFile.getParent();
        String version = "ACAD2018"; // 필요 시 조정
        String format = "DXF";

        ProcessBuilder pb = new ProcessBuilder(
                ODA_CONVERTER_PATH,
                inputFolder,
                outputFolder,
                version,
                format,
                "0" // 0 = Recursive off
        );

        pb.redirectErrorStream(true); // 표준 출력과 에러 합침
        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new IOException("ODAConverter 실행 실패. 종료 코드: " + exitCode);
        }

        // 변환 후 outputFolder 안에 DXF 파일이 생성됨
    }
}
