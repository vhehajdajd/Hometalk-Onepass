package com.hometalk.onepass.config;


import com.hometalk.onepass.auth.config.CustomOAuth2LoginSuccessHandler;
import com.hometalk.onepass.auth.config.CustomLogoutSuccessHandler;
import com.hometalk.onepass.auth.config.CustomAuthorizationRequestResolver;
import com.hometalk.onepass.auth.config.CustomLoginSuccessHandler;
import com.hometalk.onepass.auth.config.RememberMeConfig;
import com.hometalk.onepass.auth.service.CustomOAuth2UserService;
import com.hometalk.onepass.auth.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;


@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    // OAuth2 로그인 과정에서 각각 사용자 조회, 성공 후 분기, 로그아웃 후처리 역할을 맡는다.
    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomLoginSuccessHandler customLoginSuccessHandler;
    private final CustomOAuth2LoginSuccessHandler customOAuth2LoginSuccessHandler;
    private final CustomLogoutSuccessHandler customLogoutSuccessHandler;
    private final CustomAuthorizationRequestResolver customAuthorizationRequestResolver;
    private final CustomUserDetailsService customUserDetailsService;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final PersistentTokenRepository persistentTokenRepository;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authenticationProvider(daoAuthenticationProvider())
                // ★ 여기에 추가
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**")
                        .ignoringRequestMatchers("/admin/**")
                        .ignoringRequestMatchers("/community/**")
                        .ignoringRequestMatchers("/auth/register/email/**")
                )

                .authorizeHttpRequests(auth -> auth
                        // 1. 인증 없이 접근 가능한 공개 페이지와 정적 리소스
                        .requestMatchers(
                                "/",
                                "/home",
                                "/login",
                                "/auth",
                                "/auth/loginimage/**",
                                "/auth/login",
                                "/auth/oauth2/**",
                                "/auth/register/**",
                                "/auth/api/check-id-duplication",

                                // react api 관련 url 허용
                                "/api/**"

                        ).permitAll()

                        // 2. 승인 상태 안내 페이지는 로그인 사용자만 접근
                        .requestMatchers("/auth/approval/**").authenticated()

                        // 3. 관리자 전용 기능
                        .requestMatchers(
                                "/admin/**",
                                "/api/admin/**",
                                "/community/admin/**",
                                "/api/posts/**",
                                "/billing/admin/**",
                                "/billing/api/billing/admin/**",
                                "/api/billing/admin/**",
                                "/reservation/admin/**"
                        ).authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/reservations/*/approve").authenticated()
                        .requestMatchers(HttpMethod.GET, "/notice/write", "/notice/*/edit", "/notice/api/drafts")
                        .authenticated()
                        .requestMatchers(HttpMethod.POST, "/notice/write", "/notice/*/edit", "/notice/*/delete", "/notice/image-upload")
                        .authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/inquiries/*/respond").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/complaints/*/respond", "/api/complaints/*/complete")
                        .authenticated()

                        // 4. 주차 직원 전용 기능
                        .requestMatchers("/staff/**").authenticated()

                        // 5. 입주민 기능
                        .requestMatchers(
                                "/parking/**",
                                "/reservation/**",
                                "/api/reservations/**",
                                "/billing/**",
                                "/api/billing/resident/**",
                                "/api/billing/*/detail",
                                "/inquiries/**",
                                "/api/inquiries/**",
                                "/api/inquiry/**",
                                "/complaints/**",
                                "/api/complaints/**",
                                "/api/complaint/**",
                                "/api/facility/**",
                                "/api/resident/**"
                                ).authenticated()
                        .requestMatchers(HttpMethod.POST, "/schedule/write", "/schedule/write/repeat").authenticated()
                        .requestMatchers(HttpMethod.POST, "/schedule/api/*/edit").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/schedule/api/**").authenticated()
                        .requestMatchers("/schedule/**").authenticated()
                        .requestMatchers("/notice/**").authenticated()
                        .requestMatchers("/community/**", "/api/community/**").authenticated()

                        // 6. 마이페이지는 스태프 업무 계정에서는 사용하지 않는다.
                        .requestMatchers("/myPage", "/myPage/popup", "/auth/withdraw")
                        .authenticated()

                        // 7. 알림, 대시보드 등 공통 로그인 사용자 기능
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        // 비로그인 사용자가 보호 페이지를 요청하면 로그인 페이지 대신 홈으로 보내고 알림을 띄운다.
                        .authenticationEntryPoint((request, response, authException) -> {
                            if (isApiRequest(request.getRequestURI(), request.getContextPath())) {
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                return;
                            }

                            response.sendRedirect(request.getContextPath()
                                    + "/auth?redirectURL="
                                    + URLEncoder.encode(getRedirectUrl(request), StandardCharsets.UTF_8));
                        })
                        // 로그인은 되어 있지만 권한이 없는 페이지 요청은 이동 없이 알림만 보여준다.
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            if (isApiRequest(request.getRequestURI(), request.getContextPath())) {
                                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                return;
                            }

                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("text/html;charset=UTF-8");
                            response.getWriter().write("""
                                    <script>
                                        alert('접근 권한이 없는 페이지입니다.');
                                        history.back();
                                    </script>
                                    """);
                        })
                )
                .formLogin(form -> form
                        .loginPage("/auth")            // 1. 사용자 정의 로그인 페이지 경로
                        // React 로그인 폼은 이 URL로 form-urlencoded 요청을 보낸다.
                        // 실제 인증 처리는 기존 Spring Security formLogin 필터가 그대로 담당한다.
                        .loginProcessingUrl("/api/auth/login") // 2. 로그인 처리 URL
                        .successHandler(customLoginSuccessHandler)   // 3. 로그인 성공 시 이동할 경로
                        // API 로그인 실패 시 HTML redirect 대신 React가 처리할 수 있도록 401만 내려준다.
                        .failureHandler((request, response, exception) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"error\":\""
                                    + exception.getClass().getSimpleName()
                                    + "\",\"message\":\""
                                    + exception.getMessage()
                                    + "\"}");
                        })
                        .permitAll()                  // 5. 로그인 페이지는 누구나 접근 가능해야 함
                        .usernameParameter("loginId") // username이 아닌 login_id으로 name 설정
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/auth") // 로그인 페이지를 동일하게 사용
                        // 카카오 로그인 요청에 prompt=login 같은 추가 파라미터를 주입한다.
                        .authorizationEndpoint(authorization -> authorization
                                .authorizationRequestResolver(customAuthorizationRequestResolver)
                        )
                        .successHandler(customOAuth2LoginSuccessHandler) // 핸들러 등록
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                )
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        // 로컬/소셜 로그아웃을 하나의 엔드포인트로 통합하고,
                        // 카카오 사용자는 외부 로그아웃까지 이어서 처리한다.
                        .logoutSuccessHandler(customLogoutSuccessHandler)
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID", RememberMeConfig.REMEMBER_ME_PARAMETER)
                )
                .rememberMe(rememberMe -> rememberMe
                        .key(RememberMeConfig.REMEMBER_ME_KEY)
                        .rememberMeParameter(RememberMeConfig.REMEMBER_ME_PARAMETER)
                        .tokenValiditySeconds(RememberMeConfig.REMEMBER_ME_SECONDS)
                        .userDetailsService(customUserDetailsService)
                        .tokenRepository(persistentTokenRepository)
                );


        return http.build();

    }

    // fetch/API 요청은 HTML 리다이렉트가 섞이지 않도록 401/403 상태 코드만 내려준다.
    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(customUserDetailsService);
        provider.setPasswordEncoder(bCryptPasswordEncoder);
        return provider;
    }

    private boolean isApiRequest(String requestUri, String contextPath) {
        String path = requestUri;
        if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }

        return path.startsWith("/api/")
                || path.contains("/api/");
    }

    private String getRedirectUrl(jakarta.servlet.http.HttpServletRequest request) {
        String redirectUrl = request.getRequestURI();
        String queryString = request.getQueryString();
        if (queryString != null && !queryString.isBlank()) {
            redirectUrl += "?" + queryString;
        }

        return redirectUrl;
    }
}
