package com.inn.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.stereotype.Service;

import com.inn.dto.CreateJobRequest;
import com.inn.dto.CreateJobResponse;
import com.inn.jobs.RestInvokeJob;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerService {

	private final Scheduler scheduler;

	@PostConstruct
	void startScheduler() {
		try {
			if (!scheduler.isStarted())
				scheduler.start();
		} catch (SchedulerException e) {
			throw new RuntimeException(e);
		}
	}

	public CreateJobResponse createJob(CreateJobRequest req) throws SchedulerException {
		// Build JobDetail
		JobDataMap jdm = new JobDataMap();
		jdm.put("targetUrl", req.getTargetUrl());
		jdm.put("httpMethod", req.getHttpMethod());
		jdm.put("timeoutMs", req.getTimeoutMs());

		Map<String, String> headers = new LinkedHashMap<>();
		if (req.getHeaders() != null) {
			req.getHeaders().forEach(h -> headers.put(h.getName(), h.getValue()));
		}
		jdm.put("headers", headers);
		jdm.put("queryParams", req.getQueryParams());
		jdm.put("body", req.getBody());

		JobDetail jobDetail = JobBuilder.newJob(RestInvokeJob.class).withIdentity(req.getJobName(), req.getJobGroup())
				.usingJobData(jdm).storeDurably(false).build();

		// Build Trigger
		Trigger trigger;
		Date startAt = req.getStartAt() != null ? Date.from(req.getStartAt().toInstant())
				: Date.from(Instant.now().plusSeconds(5));
		Date endAt = req.getEndAt() != null ? Date.from(req.getEndAt().toInstant()) : null;

		String misfire = req.getMisfirePolicy(); // IGNORE | FIRE_AND_PROCEED | DO_NOTHING

		if (req.getCronExpression() != null && !req.getCronExpression().isBlank()) {
			CronScheduleBuilder cron = CronScheduleBuilder.cronSchedule(req.getCronExpression());
			cron = applyCronMisfire(cron, misfire);

			trigger = TriggerBuilder.newTrigger().withIdentity(req.getJobName() + "_trigger", req.getJobGroup())
					.withSchedule(cron).startAt(startAt).endAt(endAt).forJob(jobDetail).build();
		} else {
			// Simple trigger
			int interval = Optional.ofNullable(req.getRepeatIntervalSeconds()).orElse(60);
			int count = Optional.ofNullable(req.getRepeatCount()).orElse(SimpleTrigger.REPEAT_INDEFINITELY);

			SimpleScheduleBuilder simple = SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(interval);

			simple = applySimpleMisfire(simple, misfire);

			if (count == SimpleTrigger.REPEAT_INDEFINITELY) {
				simple = simple.repeatForever();
			} else {
				simple = simple.withRepeatCount(count);
			}

			trigger = TriggerBuilder.newTrigger().withIdentity(req.getJobName() + "_trigger", req.getJobGroup())
					.withSchedule(simple).startAt(startAt).endAt(endAt).forJob(jobDetail).build();
		}

		// Schedule
		Date next = scheduler.scheduleJob(jobDetail, trigger);

		return CreateJobResponse.builder().jobName(jobDetail.getKey().getName()).jobGroup(jobDetail.getKey().getGroup())
				.triggerName(trigger.getKey().getName()).triggerType(trigger instanceof CronTrigger ? "CRON" : "SIMPLE")
				.nextFireTime(next).build();
	}

	private CronScheduleBuilder applyCronMisfire(CronScheduleBuilder b, String policy) {
		return switch (policy) {
		case "IGNORE" -> b.withMisfireHandlingInstructionIgnoreMisfires();
		case "DO_NOTHING" -> b.withMisfireHandlingInstructionDoNothing();
		default -> b.withMisfireHandlingInstructionFireAndProceed(); // FIRE_AND_PROCEED
		};
	}

	private SimpleScheduleBuilder applySimpleMisfire(SimpleScheduleBuilder b, String policy) {
		return switch (policy) {
		case "IGNORE" -> b.withMisfireHandlingInstructionIgnoreMisfires();
		case "DO_NOTHING" -> b.withMisfireHandlingInstructionNextWithExistingCount();
		default -> b.withMisfireHandlingInstructionFireNow(); // FIRE_AND_PROCEED analog
		};
	}

	public List<TriggerInfoView> listAllTriggers() throws SchedulerException {
		List<TriggerInfoView> out = new ArrayList<>();
		for (String group : scheduler.getTriggerGroupNames()) {
			Set<TriggerKey> keys = scheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals(group));
			for (TriggerKey key : keys) {
				Trigger t = scheduler.getTrigger(key);
				Trigger.TriggerState state = scheduler.getTriggerState(key);
				JobKey jobKey = t.getJobKey();
				out.add(TriggerInfoView.builder().jobGroup(jobKey.getGroup()).jobName(jobKey.getName())
						.triggerName(key.getName()).triggerGroup(key.getGroup())
						.triggerType(t instanceof CronTrigger ? "CRON" : "SIMPLE")
						.previousFireTime(t.getPreviousFireTime()).nextFireTime(t.getNextFireTime()).state(state.name())
						.build());
			}
		}
		return out;
	}

	public void pauseJob(String group, String name) throws SchedulerException {
		scheduler.pauseJob(JobKey.jobKey(name, group));
	}

	public void resumeJob(String group, String name) throws SchedulerException {
		scheduler.resumeJob(JobKey.jobKey(name, group));
	}

	public boolean deleteJob(String group, String name) throws SchedulerException {
		return scheduler.deleteJob(JobKey.jobKey(name, group));
	}

	public void triggerNow(String group, String name) throws SchedulerException {
		scheduler.triggerJob(JobKey.jobKey(name, group));
	}

	public Optional<TriggerInfoView> getTriggerInfo(String group, String name) throws SchedulerException {
		TriggerKey tk = TriggerKey.triggerKey(name + "_trigger", group);
		Trigger t = scheduler.getTrigger(tk);
		if (t == null)
			return Optional.empty();
		Trigger.TriggerState state = scheduler.getTriggerState(tk);
		JobKey jobKey = t.getJobKey();
		return Optional.of(TriggerInfoView.builder().jobGroup(jobKey.getGroup()).jobName(jobKey.getName())
				.triggerName(tk.getName()).triggerGroup(tk.getGroup())
				.triggerType(t instanceof CronTrigger ? "CRON" : "SIMPLE").previousFireTime(t.getPreviousFireTime())
				.nextFireTime(t.getNextFireTime()).state(state.name()).build());
	}
}
