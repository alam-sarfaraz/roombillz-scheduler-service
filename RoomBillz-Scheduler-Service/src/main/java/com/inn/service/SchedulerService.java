package com.inn.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.stereotype.Service;

import com.inn.dto.CreateJobRequest;
import com.inn.dto.CreateJobResponse;
import com.inn.jobs.HttpCallJob;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

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

		JobDataMap dataMap = new JobDataMap();
		dataMap.put("targetUrl", req.getTargetUrl());
		dataMap.put("httpMethod", req.getHttpMethod());

		JobDetail jobDetail = JobBuilder.newJob(HttpCallJob.class).withIdentity(req.getJobName(), req.getJobGroup())
				.usingJobData(dataMap).storeDurably(false).build();

		Trigger trigger;

		if (req.getCronExpression() != null && !req.getCronExpression().isBlank()) {
			CronScheduleBuilder cron = CronScheduleBuilder.cronSchedule(req.getCronExpression())
					.withMisfireHandlingInstructionFireAndProceed();

			trigger = TriggerBuilder.newTrigger().withIdentity(req.getJobName() + "_trigger", req.getJobGroup())
					.startNow() // important
					.withSchedule(cron).forJob(jobDetail).build();

		} else {
			SimpleScheduleBuilder simple = SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(60)
					.repeatForever().withMisfireHandlingInstructionFireNow();

			trigger = TriggerBuilder.newTrigger().withIdentity(req.getJobName() + "_trigger", req.getJobGroup())
					.startNow() // important
					.withSchedule(simple).forJob(jobDetail).build();
		}

		Date next = scheduler.scheduleJob(jobDetail, trigger);

		return CreateJobResponse.builder().jobName(req.getJobName()).jobGroup(req.getJobGroup())
				.triggerName(trigger.getKey().getName()).triggerType(trigger instanceof CronTrigger ? "CRON" : "SIMPLE")
				.nextFireTime(next).build();
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

	public void triggerNow(String group, String name) throws SchedulerException {
		scheduler.triggerJob(JobKey.jobKey(name, group));
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
}
