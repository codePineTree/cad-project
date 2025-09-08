package com.cad.cadproject.test;

import com.cad.cadproject.mapper.TestMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController2 {

    @Autowired
    private TestMapper testMapper;

    @GetMapping("/test2")
    public String testDBConnection() {
        return testMapper.selectTest();
    }
}
