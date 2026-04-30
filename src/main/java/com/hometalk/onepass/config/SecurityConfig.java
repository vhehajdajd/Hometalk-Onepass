package com.hometalk.onepass.config;


import com.hometalk.onepass.auth.config.CustomOAuth2LoginSuccessHandler;
import com.hometalk.onepass.auth.config.CustomLogoutSuccessHandler;
import com.hometalk.onepass.auth.config.CustomAuthorizationRequestResolver;
import com.hometalk.onepass.auth.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;


@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // OAuth2 로그인 과정에서 각각 사용자 조회, 성공 후 분기, 로그아웃 후처리 역할을 맡는다.
    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomOAuth2LoginSuccessHandler customOAuth2LoginSuccessHandler;
    private final CustomLogoutSuccessHandler customLogoutSuccessHandler;
    private final CustomAuthorizationRequestResolver customAuthorizationRequestResolver;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // 1. "/" 경로와 정적 리소스(css, js 등)는 모두에게 허용
                        .requestMatchers(
                                "/auth",
                                "/auth/loginimage/**",
                                "/auth/signup",
                                "/auth/login",
                                "/auth/register/**",
                                "/oauth2/authorization/**",
                                "/login/oauth2/**"
                        ).permitAll()

                        // 2. 그 외의 모든 요청은 인증(로그인)이 필요함
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/auth")            // 1. 사용자 정의 로그인 페이지 경로
                        .loginProcessingUrl("/auth/login") // 2. 로그인 처리 URL
                        .defaultSuccessUrl("/index", true)   // 3. 로그인 성공 시 이동할 경로
                        .failureUrl("/auth?error=true")   // 4. 로그인 실패 시 이동할 경로
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
                        .deleteCookies("JSESSIONID")
                );


        return http.build();

    }
}
