package com.inn.service;

import java.util.Date;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TriggerInfoView {
	private String jobGroup;
	private String jobName;
	private String triggerName;
	private String triggerGroup;
	private String triggerType; // CRON/SIMPLE
	private Date previousFireTime;
	private Date nextFireTime;
	private String state; // NORMAL/PAUSED/...
}
