package com.cad.cadproject.test;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@RestController
public class testController {

    private static final String URL = "jdbc:oracle:thin:@localhost:1521:XE";
    private static final String USERNAME = "CAD_USER";
    private static final String PASSWORD = "a12345";

    @GetMapping("/test-db")
    public String testDbConnection() {
        try (Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD)) {
            if (conn != null && !conn.isClosed()) {
                return "✅ DB 연결 성공!";
            } else {
                return "⚠️ DB 연결 실패: 연결이 닫혀 있음";
            }
        } catch (SQLException e) {
            // 오류 메시지와 스택트레이스를 자세히 반환
            StringBuilder errorMsg = new StringBuilder();
            errorMsg.append("❌ DB 연결 실패!\n");
            errorMsg.append("SQLState: ").append(e.getSQLState()).append("\n");
            errorMsg.append("ErrorCode: ").append(e.getErrorCode()).append("\n");
            errorMsg.append("Message: ").append(e.getMessage()).append("\n");

            Throwable cause = e.getCause();
            while (cause != null) {
                errorMsg.append("Cause: ").append(cause.getMessage()).append("\n");
                cause = cause.getCause();
            }

            return errorMsg.toString();
        }
    }
}
