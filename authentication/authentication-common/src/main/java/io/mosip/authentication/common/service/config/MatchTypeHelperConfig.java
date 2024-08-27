package io.mosip.authentication.common.service.config;

import io.mosip.authentication.common.service.helper.MatchTypeHelper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MatchTypeHelperConfig {

    @Bean
    public MatchTypeHelper matchTypeHelper() {
        return new MatchTypeHelper();
    }
}