package com.cad.cadproject.test;

import com.cad.cadproject.mapper.TestMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class TestController5 {

    @Autowired
    private TestMapper testMapper;

    // React(localhost:3000)에서 호출 가능하도록 CORS 허용
    @CrossOrigin(origins = "http://localhost:3000")
    @GetMapping("/api/test5")
    public List<Map<String, Object>> getDbData() {
        return testMapper.selectTest2();
    }
}
