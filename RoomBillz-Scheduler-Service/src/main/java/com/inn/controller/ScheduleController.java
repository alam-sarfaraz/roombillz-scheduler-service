package com.inn.controller;

import java.util.List;

import org.springframework.http.MediaType;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "Scheduler API", description = "APIs for managing Quartz job scheduling")
public class ScheduleController {

	private final SchedulerService schedulerService;

	@Operation(summary = "Create a new scheduled job", description = "Creates a new Quartz job with either cron or interval trigger")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Job created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CreateJobResponse.class))),
			@ApiResponse(responseCode = "400", description = "Invalid job configuration", content = @Content),
			@ApiResponse(responseCode = "500", description = "Server error while creating job", content = @Content) })
	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<CreateJobResponse> create(@Valid @RequestBody CreateJobRequest req) throws Exception {
		return ResponseEntity.ok(schedulerService.createJob(req));
	}

	@Operation(summary = "List all scheduled jobs", description = "Returns list of all jobs with next and previous fire times")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Jobs listed successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PageResponse.class))) })
	@GetMapping
	public ResponseEntity<PageResponse<TriggerInfoView>> list() throws Exception {
		List<TriggerInfoView> items = schedulerService.listAllTriggers();
		return ResponseEntity.ok(PageResponse.<TriggerInfoView>builder().items(items).total(items.size()).build());
	}

	@Operation(summary = "Get job details", description = "Fetch details for a specific job including trigger state")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Job details fetched successfully"),
			@ApiResponse(responseCode = "404", description = "Job not found", content = @Content) })
	@GetMapping("/{group}/{name}")
	public ResponseEntity<TriggerInfoView> details(@PathVariable String group, @PathVariable String name)
			throws Exception {
		return schedulerService.getTriggerInfo(group, name).map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}

	@Operation(summary = "Trigger job immediately", description = "Executes the job now without waiting for next schedule")
	@ApiResponses(value = { @ApiResponse(responseCode = "202", description = "Job triggered"),
			@ApiResponse(responseCode = "404", description = "Job not found", content = @Content) })
	@PostMapping("/{group}/{name}:triggerNow")
	public ResponseEntity<Void> triggerNow(@PathVariable String group, @PathVariable String name) throws Exception {
		schedulerService.triggerNow(group, name);
		return ResponseEntity.accepted().build();
	}

	@Operation(summary = "Pause a job", description = "Pauses the job and prevents it from running")
	@ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Job paused"),
			@ApiResponse(responseCode = "404", description = "Job not found", content = @Content) })
	@PutMapping("/{group}/{name}:pause")
	public ResponseEntity<Void> pause(@PathVariable String group, @PathVariable String name) throws Exception {
		schedulerService.pauseJob(group, name);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Resume a paused job", description = "Resumes a previously paused job")
	@ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Job resumed"),
			@ApiResponse(responseCode = "404", description = "Job not found", content = @Content) })
	@PutMapping("/{group}/{name}:resume")
	public ResponseEntity<Void> resume(@PathVariable String group, @PathVariable String name) throws Exception {
		schedulerService.resumeJob(group, name);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Delete a job", description = "Removes the job and its triggers permanently")
	@ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Job deleted"),
			@ApiResponse(responseCode = "404", description = "Job not found", content = @Content) })
	@DeleteMapping("/{group}/{name}")
	public ResponseEntity<Void> delete(@PathVariable String group, @PathVariable String name) throws Exception {
		boolean removed = schedulerService.deleteJob(group, name);
		return removed ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
	}
}
