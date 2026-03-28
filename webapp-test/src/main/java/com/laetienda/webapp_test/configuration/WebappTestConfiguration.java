package com.laetienda.webapp_test.configuration;

import com.laetienda.utils.service.api.ApiUser;
import com.laetienda.utils.service.api.ApiUserImplementation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class WebappTestConfiguration {

    private final RestClient httpClient;

    public WebappTestConfiguration(RestClient restClient){
        this.httpClient = restClient;
    }

    @Bean
    public ApiUser getUserApi(){
        return new ApiUserImplementation(httpClient);
    }

}
