package com.laetienda.messenger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MessengerApplication {
//
//	@Bean
//	public RestTemplate getRestTemplate(RestTemplateBuilder builder){
//		return builder.build();
//	}
//
//	@Bean
//	public RestClientService getRestClientService(){
//		return new RestClientServiceImpl();
//	}
//
//	@Bean
//	public UserApiDeprecated getUserApi(){
//		return new UserApiDeprecatedImplementation();
//	}
//
//	@Bean
//	public MessengerApi getMessengerApi(){
//		return new MessengerApiImplementation();
//	}
//
//	@Bean
//	public CustomRestAuthenticationProvider getAuthenticationProvider(){
//		return new CustomRestAuthenticationProvider();
//	}

	public static void main(String[] args) {
		SpringApplication.run(MessengerApplication.class, args);
	}

}
