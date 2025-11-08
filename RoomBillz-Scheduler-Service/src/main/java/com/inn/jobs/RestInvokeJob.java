package com.inn.jobs;

import java.time.Duration;
import java.util.Map;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inn.util.JsonUtils;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class RestInvokeJob implements Job {

	@Autowired
	private WebClient.Builder webClientBuilder;

	@Autowired
	private ObjectMapper objectMapper;

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		JobDataMap map = context.getMergedJobDataMap();

		String url = map.getString("targetUrl");
		String method = map.getString("httpMethod");
		Integer timeoutMs = map.containsKey("timeoutMs") ? map.getInt("timeoutMs") : 30000;

		@SuppressWarnings("unchecked")
		Map<String, String> queryParams = (Map<String, String>) map.get("queryParams");
		@SuppressWarnings("unchecked")
		Map<String, String> headers = (Map<String, String>) map.get("headers");
		Object body = map.get("body");

		try {
			WebClient.RequestBodySpec req = webClientBuilder.build().method(HttpMethod.valueOf(method)).uri(builder -> {
				builder = builder.path(url);
				if (queryParams != null) {
					queryParams.forEach(builder::queryParam);
				}
				return builder.build();
			}).accept(MediaType.APPLICATION_JSON);

			if (headers != null) {
				for (var e : headers.entrySet())
					req = req.header(e.getKey(), e.getValue());
			}

			Mono<String> call;
			if ("GET".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method)) {
				call = req.retrieve().bodyToMono(String.class);
			} else {
				call = req.contentType(MediaType.APPLICATION_JSON).bodyValue(body != null ? body : "").retrieve()
						.bodyToMono(String.class);
			}

			String response = call.timeout(Duration.ofMillis(timeoutMs)).block();
			log.info("REST trigger success -> {} {} | response: {}", method, url,
					JsonUtils.safeTruncate(response, 500));
		} catch (Exception ex) {
			log.error("REST trigger failed -> {} {} | error: {}", method, url, ex.getMessage(), ex);
			throw new JobExecutionException(ex);
		}
	}
}
