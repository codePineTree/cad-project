package com.cad.cadproject.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class PathConfig {

    @Bean(name = "uploadDir")
    public Path uploadDir() {
        // 프로젝트 루트 기준 상대경로 설정 (my-react-app/public/cadfiles)
        return Paths.get("..", "my-react-app", "public", "cadfiles").toAbsolutePath().normalize();
    }

    @Bean(name = "tempDir")
    public Path tempDir() {
        // 프로젝트 루트 기준 상대경로 설정 (my-react-app/public/tempDXF)
        return Paths.get("..", "my-react-app", "public", "tempDXF").toAbsolutePath().normalize();
    }
}
