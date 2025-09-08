package com.cad.cadproject.test;

import com.cad.cadproject.mapper.TestMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController4 {

    @Autowired
    private TestMapper testMapper;

    // React(localhost:3000)에서 호출 가능하도록 CORS 허용
    @CrossOrigin(origins = "http://localhost:3000")
    @GetMapping("/api/test4")
    public String getDbData() {
        return testMapper.selectTest();
    }
}
