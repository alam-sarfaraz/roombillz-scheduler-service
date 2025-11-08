package com.inn.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateJobRequest {
	// Quartz keys
	@NotBlank
	private String jobName;
	private String jobGroup = "DEFAULT";

	// Trigger config (choose one of cron or simpleRepeat)
	// CRON example: "0 0/5 * * * ?" (every 5 minutes)
	private String cronExpression;

	// Simple repeat (in seconds)
	private Integer repeatIntervalSeconds; // e.g., 300
	private Integer repeatCount; // null or -1 for forever

	private OffsetDateTime startAt; // optional
	private OffsetDateTime endAt; // optional

	// Misfire policy hints for cron/simple
	@Pattern(regexp = "IGNORE|FIRE_AND_PROCEED|DO_NOTHING", message = "Invalid misfire policy")
	private String misfirePolicy = "FIRE_AND_PROCEED";

	// REST target
	@NotBlank
	private String targetUrl; // http://notification:8082/api/v1/...
	@Pattern(regexp = "GET|POST|PUT|DELETE|PATCH", message = "Invalid HTTP method")
	private String httpMethod = "POST";

	// Optional headers/body/query
	private List<HttpHeaderKV> headers;
	private Map<String, String> queryParams;
	private Object body; // JSON body if required
	private Integer timeoutMs = 30000; // request timeout
}
