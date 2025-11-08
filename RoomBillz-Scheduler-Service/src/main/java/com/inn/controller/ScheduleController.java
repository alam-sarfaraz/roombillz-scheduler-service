package com.inn.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.inn.dto.CreateJobRequest;
import com.inn.dto.CreateJobResponse;
import com.inn.dto.PageResponse;
import com.inn.service.SchedulerService;
import com.inn.service.TriggerInfoView;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/schedules")
public class ScheduleController {

	private final SchedulerService schedulerService;

	@PostMapping
	public ResponseEntity<CreateJobResponse> create(@Valid @RequestBody CreateJobRequest req) throws Exception {
		return ResponseEntity.ok(schedulerService.createJob(req));
	}

	@GetMapping
	public ResponseEntity<PageResponse<TriggerInfoView>> list() throws Exception {
		List<TriggerInfoView> items = schedulerService.listAllTriggers();
		return ResponseEntity.ok(PageResponse.<TriggerInfoView>builder().items(items).total(items.size()).build());
	}

	@GetMapping("/{group}/{name}")
	public ResponseEntity<TriggerInfoView> details(@PathVariable String group, @PathVariable String name)
			throws Exception {
		return schedulerService.getTriggerInfo(group, name).map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}

	@PostMapping("/{group}/{name}:triggerNow")
	public ResponseEntity<Void> triggerNow(@PathVariable String group, @PathVariable String name) throws Exception {
		schedulerService.triggerNow(group, name);
		return ResponseEntity.accepted().build();
	}

	@PutMapping("/{group}/{name}:pause")
	public ResponseEntity<Void> pause(@PathVariable String group, @PathVariable String name) throws Exception {
		schedulerService.pauseJob(group, name);
		return ResponseEntity.noContent().build();
	}

	@PutMapping("/{group}/{name}:resume")
	public ResponseEntity<Void> resume(@PathVariable String group, @PathVariable String name) throws Exception {
		schedulerService.resumeJob(group, name);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/{group}/{name}")
	public ResponseEntity<Void> delete(@PathVariable String group, @PathVariable String name) throws Exception {
		boolean removed = schedulerService.deleteJob(group, name);
		return removed ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
	}
}
