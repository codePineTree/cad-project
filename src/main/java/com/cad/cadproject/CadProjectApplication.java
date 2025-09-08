package com.cad.cadproject;

import com.cad.cadproject.mapper.TestMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CadProjectApplication implements CommandLineRunner {

    @Autowired
    private TestMapper testMapper;

    public static void main(String[] args) {
        SpringApplication.run(CadProjectApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        String msg = testMapper.selectTest();
        System.out.println("MyBatis 테스트 결과: " + msg);
    }
}
