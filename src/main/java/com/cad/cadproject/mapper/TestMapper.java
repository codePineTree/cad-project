package com.cad.cadproject.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TestMapper {
    String selectTest();
    // TestDb 테이블의 TestDescNo, TestDesc 조회
    List<Map<String, Object>> selectTest2();
}
