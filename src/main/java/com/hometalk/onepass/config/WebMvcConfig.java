package com.hometalk.onepass.config;

import com.hometalk.onepass.auth.config.ApprovalStatusInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.nio.file.Paths;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final ApprovalStatusInterceptor approvalStatusInterceptor;

    @Value("${file.upload.path}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absolutePath = Paths.get(uploadPath).toAbsolutePath().normalize().toString()
                .replace("\\", "/");
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:///" + absolutePath + "/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(approvalStatusInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth",
                        "/auth/login",
                        "/auth/register/**",
                        "/oauth2/authorization/**",
                        "/login/oauth2/**",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/auth/loginimage/**",
                        "/uploads/**"
                );
    }
}
