package com.inn.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
	@Bean
	public WebClient.Builder webClientBuilder() {
		return WebClient.builder()
				.exchangeStrategies(ExchangeStrategies.builder()
				.codecs(cfg -> cfg.defaultCodecs()
				.maxInMemorySize(2 * 1024 * 1024)).build());
	}
}
