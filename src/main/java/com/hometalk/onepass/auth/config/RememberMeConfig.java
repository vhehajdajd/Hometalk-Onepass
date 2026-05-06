package com.hometalk.onepass.auth.config;

import com.hometalk.onepass.auth.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

import javax.sql.DataSource;

@Configuration
public class RememberMeConfig {

    public static final String REMEMBER_ME_KEY = "hometalk-onepass-remember-me-key";
    public static final String REMEMBER_ME_PARAMETER = "remember-me";
    public static final String OAUTH2_REMEMBER_ME_SESSION_KEY = "OAUTH2_REMEMBER_ME";
    public static final int REMEMBER_ME_SECONDS = 60 * 60 * 24 * 14;

    @Bean
    public PersistentTokenRepository persistentTokenRepository(DataSource dataSource) {
        JdbcTokenRepositoryImpl repository = new JdbcTokenRepositoryImpl();
        repository.setDataSource(dataSource);
        return repository;
    }

    @Bean
    public RememberMeServices rememberMeServices(CustomUserDetailsService userDetailsService,
                                                 PersistentTokenRepository persistentTokenRepository) {
        PersistentTokenBasedRememberMeServices services = new PersistentTokenBasedRememberMeServices(
                REMEMBER_ME_KEY,
                userDetailsService,
                persistentTokenRepository
        );
        services.setParameter(REMEMBER_ME_PARAMETER);
        services.setTokenValiditySeconds(REMEMBER_ME_SECONDS);
        return services;
    }
}
