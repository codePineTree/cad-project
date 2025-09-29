package com.cad.cadproject.config;

import com.aspose.cad.License;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import javax.annotation.PostConstruct;
import java.io.InputStream;
import com.aspose.cad.License;
@Configuration
public class WebConfig implements WebMvcConfigurer {

	@PostConstruct
	public void initAsposeLicense() {
	    try {
	        License license = new License();
	        
	        // ClassLoader로 리소스 읽기
	        InputStream licenseStream = getClass().getClassLoader()
	            .getResourceAsStream("Aspose.TotalProductFamily.lic");
	        
	        if (licenseStream == null) {
	            System.err.println("라이선스 파일을 찾을 수 없습니다!");
	            return;
	        }
	        
	        license.setLicense(licenseStream);
	        licenseStream.close();
	        System.out.println("Aspose.CAD 라이선스 적용 완료!");
	        
	    } catch (Exception e) {
	        System.err.println("Aspose.CAD 라이선스 적용 실패: " + e.getMessage());
	        e.printStackTrace();
	    }
	}

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 모든 URL에 대해
            .allowedOrigins("http://localhost:3000") // React 개발 서버 허용
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 허용 HTTP 메서드
            .allowCredentials(true) // 쿠키 전송 허용
            .maxAge(3600); // preflight 캐시 시간 (초)
    }
}