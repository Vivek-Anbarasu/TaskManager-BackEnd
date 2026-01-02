package com.restapp.controller;

import com.restapp.dto.GetTaskResponse;
import com.restapp.dto.SaveTaskRequest;
import com.restapp.dto.UpdateTaskRequest;
import com.restapp.entity.Tasks;
import com.restapp.exception.BadRequest;
import com.restapp.exception.InternalServerError;
import com.restapp.exception.NotFound;
import com.restapp.service.TaskService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping("/v1")
@Slf4j
public class TaskManagementController{

	@Autowired
	private TaskService taskService;

	@GetMapping(path = "/getTask/{taskId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<GetTaskResponse> getTask(@NotNull(message="TaskId is mandatory") @PathVariable("taskId") Integer taskId) throws InternalServerError,NotFound {
		GetTaskResponse getResponse = null;
		try {
			getResponse = taskService.getTask(taskId);
			if (getResponse == null) {
				log.error("No records found for taskId = " + taskId);
				throw new NotFound("No records found for taskId = " + taskId);
			}else {
				return new ResponseEntity<>(getResponse, HttpStatus.OK);
			}
		} catch (Exception e) {
			log.error("Error in getTask", e);
			throw new InternalServerError("Error in getTask" + e.getMessage());
		}
	}

	@PostMapping(path = "/saveTask", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> saveTask(@Valid @RequestBody SaveTaskRequest saveRequest) throws InternalServerError,BadRequest {
		int taskId;
		try {
			Tasks tasks = taskService.findByTitle(saveRequest.getTitle());
			if (tasks != null) {
				log.error("Title already exists: " + saveRequest.getTitle());
				throw new BadRequest("Title already exists");
			} else {
				taskId = taskService.saveTask(saveRequest);
				if (taskId == 0) {
					log.error("Failed to saveTask"+saveRequest.getTitle());
					throw new InternalServerError("Failed to saveTask "+saveRequest.getTitle());
				}else {
					log.info("Successfully Saved"+saveRequest.getTitle());
					return new ResponseEntity<>(String.valueOf(taskId), HttpStatus.CREATED);
				}
			}
		} catch (Exception e) {
			log.error("Failed to saveTask", e);
			throw new InternalServerError("Failed to save " + e.getMessage());
		}
	}

	@PutMapping(path = "/updateTask", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> updateTask(@Valid @RequestBody UpdateTaskRequest updateRequest) throws InternalServerError,NotFound,BadRequest {
		boolean response = false;
        log.info("Update request received for taskId = " + updateRequest.getId());
		try {
			Tasks tasks = taskService.findByTitle(updateRequest.getTitle());
			if (tasks != null && (tasks.getTaskId().intValue() != updateRequest.getId().intValue())) {
				log.error("Title already exists: " + updateRequest.getTitle());
				throw new BadRequest("Title already exists: " + updateRequest.getTitle());
			} else {
				response = taskService.updateTask(updateRequest);
				if (response) {
					log.info("Succesfully Updated: " + updateRequest.getTitle());
					return new ResponseEntity<>("Successfully Updated", HttpStatus.OK);
				} else {
					log.error("No records found for taskId = " + updateRequest.getId());
					throw new NotFound("No records found for taskId = " + updateRequest.getId());
				}
			}
		} catch (Exception e) {
			log.error("Failed to update Task", e);
			throw new InternalServerError("Failed to update Task " + e.getMessage());
		}
	}

	@DeleteMapping("/deleteTask/{id}")
	public ResponseEntity<Void> deleteTask(@NotNull(message="TaskId is mandatory") @PathVariable("id") Integer id) throws InternalServerError,NotFound {
		boolean response;
        log.info("Delete request received for taskId = " + id);
		try {
            GetTaskResponse getResponse = taskService.getTask(id);
			if (getResponse == null) {
				log.error("No records found for taskId = " + id);
				throw new NotFound("No records found for taskId = " + id);
			} else {
				response = taskService.deleteTask(id);
				if (!response) {
					log.error("Error in deleteTask");
					throw new InternalServerError("Error in deleteTask ");
				}else {
					log.info("Succesfully Deleted :" + id);
					return new ResponseEntity<>(HttpStatus.NO_CONTENT);
				}
			}
		} catch (Exception e) {
			log.error("Error in deleteTask", e);
			throw new InternalServerError("Error in deleteTask "+e.getMessage());
		}
	}

	@GetMapping(path = "/getAllTasks" , produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<GetTaskResponse>> getAllTasks() throws InternalServerError, NotFound {
		List<GetTaskResponse> responseList = null;
		try {
			responseList = taskService.getAllTasks();
			if (responseList == null) {
				log.info("No records found");
				throw new NotFound("No records found ");
			}else {
				return new ResponseEntity<>(responseList, HttpStatus.OK);
			}
		} catch (Exception e) {
			log.error("Error in getAllTasks", e);
			throw new InternalServerError("Error in getAllTasks "+e.getMessage());
		}
	}
}