package com.laetienda.kcUser.configuration;

import com.laetienda.lib.service.ToolBoxService;
import com.laetienda.lib.service.ToolBoxServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KcUserConfiguration {

    @Bean
    public ToolBoxService getToolBox(){
        return new ToolBoxServiceImpl();
    }
}
