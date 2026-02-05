package com.taskmanager.api.controller;

import com.taskmanager.api.dto.*;

import com.taskmanager.domain.model.Tasks;
import com.taskmanager.exception.BadRequest;
import com.taskmanager.exception.InternalServerError;
import com.taskmanager.exception.NotFound;
import com.taskmanager.service.TaskService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping("/task")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Task Services")
public class TaskManagementController{

	private final TaskService taskService;

	@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
	@GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<GetTaskResponse> getTask(@PathVariable("id") Integer id) {
        log.info("Get request received for taskId = {}", id);
		GetTaskResponse getResponse = taskService.getTask(id);
		if (getResponse == null) {
			log.info("No records found for taskId = {}", id);
			throw new NotFound("No records found for taskId = " + id);
		}
		return new ResponseEntity<>(getResponse, HttpStatus.OK);
	}

	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping(path = "/", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> saveTask(@Valid @RequestBody SaveTaskRequest saveRequest) {
		Tasks tasks = taskService.findByTitle(saveRequest.getTitle());
		if (tasks != null) {
			log.warn("Title already exists: {}", saveRequest.getTitle());
			throw new BadRequest("Title already exists");
		}
		Long taskId = taskService.saveTask(saveRequest);
		if (taskId == 0) {
			log.error("Failed to saveTask {}", saveRequest.getTitle());
			throw new InternalServerError("Failed to saveTask " + saveRequest.getTitle());
		}
		log.info("Successfully Saved {}", saveRequest.getTitle());
		return new ResponseEntity<>(String.valueOf(taskId), HttpStatus.CREATED);
	}

	@PreAuthorize("hasRole('ADMIN')")
	@PutMapping(path = "/", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> updateTask(@Valid @RequestBody UpdateTaskRequest updateRequest) {
		log.info("Update request received for taskId = {}", updateRequest.getId());
		Tasks tasks = taskService.findByTitle(updateRequest.getTitle());
		if (tasks != null && (tasks.getTaskId().intValue() != updateRequest.getId().intValue())) {
			log.warn("Title already exists: {}", updateRequest.getTitle());
			throw new BadRequest("Title already exists: " + updateRequest.getTitle());
		}
		boolean response = taskService.updateTask(updateRequest);
		if (response) {
			log.info("Succesfully Updated: {}", updateRequest.getTitle());
			return new ResponseEntity<>("Successfully Updated", HttpStatus.OK);
		}
		log.info("No records found for taskId = {}", updateRequest.getId());
		throw new NotFound("No records found for taskId = " + updateRequest.getId());
	}

	@PreAuthorize("hasRole('ADMIN')")
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteTask(@PathVariable("id") Integer id) {
        log.info("Delete request received for taskId = {}", id);
        boolean response = taskService.deleteTask(id);
        if (!response) {
            log.error("No records found for taskId = {}", id);
            throw new NotFound("No records found for taskId = " + id);
        }
        log.info("Succesfully Deleted :{}", id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
	@GetMapping(path = "/" , produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<GetTaskResponse>> getAllTasks() {
        log.info("Get all tasks request received");
		List<GetTaskResponse> responseList = taskService.getAllTasks();
		if (responseList == null || responseList.isEmpty()) {
			log.info("No records found");
			throw new NotFound("No records found ");
		}
		return new ResponseEntity<>(responseList, HttpStatus.OK);
	}
}