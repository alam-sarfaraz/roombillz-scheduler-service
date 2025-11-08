package com.inn.dto;

import java.util.Date;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateJobResponse {
	private String jobName;
	private String jobGroup;
	private String triggerName;
	private String triggerType; // CRON or SIMPLE
	private Date nextFireTime;
}
