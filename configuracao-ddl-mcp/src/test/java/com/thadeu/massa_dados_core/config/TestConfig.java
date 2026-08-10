package com.thadeu.massa_dados_core.config;

import com.thadeu.massa_dados_core.mcp.DdlToolsService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public DdlToolsService ddlToolsService() {
        return mock(DdlToolsService.class);
    }
}