package com.inn.jobs;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class HttpCallJob implements Job {

	@Override
	public void execute(JobExecutionContext context) {
		String url = context.getMergedJobDataMap().getString("targetUrl");
		String httpMethod = context.getMergedJobDataMap().getString("httpMethod");

		RestTemplate restTemplate = new RestTemplate();

		try {
			ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.valueOf(httpMethod), null,
					String.class);

			System.out.println("Scheduler executed call: " + url + " Status: " + response.getStatusCode());

		} catch (Exception e) {
			System.err.println("Scheduler failed calling: " + url);
			System.err.println("Reason: " + e.getMessage());
		}
	}
}
