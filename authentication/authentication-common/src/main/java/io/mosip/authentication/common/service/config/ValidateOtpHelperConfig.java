package io.mosip.authentication.common.service.config;

import io.mosip.authentication.common.service.integration.ValidateOtpHelper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ValidateOtpHelperConfig {

    @Bean
    public ValidateOtpHelper validateOtpHelper() {
        return new ValidateOtpHelper();
    }
}
