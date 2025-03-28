package com.voxloud.provisioning.configuration.properties;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class ProvisioningProperties {
    @Value("${provisioning.domain}")
    private String domain;
    @Value("${provisioning.port}")
    private String port;
    @Value("${provisioning.codecs}")
    private String[] codecs;
}