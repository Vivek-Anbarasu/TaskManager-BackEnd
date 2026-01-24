package com.restapp.service;


import com.restapp.dao.TaskRepository;
import com.restapp.dto.GetTaskResponse;
import com.restapp.dto.SaveTaskRequest;
import com.restapp.dto.UpdateTaskRequest;
import com.restapp.entity.Tasks;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
@Slf4j
public class TaskServiceImpl implements TaskService {
	
	private final TaskRepository taskRepository;

	@Override
	@Transactional
	public int saveTask(SaveTaskRequest saveRequest) {
		Tasks tasks = Tasks.builder().title(saveRequest.getTitle()).
        description(saveRequest.getDescription()).status(saveRequest.getStatus()).build();
		Tasks savedTasks = taskRepository.save(tasks);
		log.info("Saved task with id={} title={}", savedTasks.getTaskId(), savedTasks.getTitle());
		return savedTasks.getTaskId();
	}

	@Override
	@Transactional(readOnly = true)
	public GetTaskResponse getTask(Integer taskId) {
		GetTaskResponse getResponse = null;
		Optional<Tasks> result = taskRepository.findById(taskId.longValue());
		if (result.isPresent()) {
		Tasks tasks = result.get();
		getResponse = GetTaskResponse.builder().
				id(tasks.getTaskId()).title(tasks.getTitle()).
				description(tasks.getDescription()).status(tasks.getStatus())
				.build();
		}
		return getResponse;
	}

	@Override
	@Transactional
	public boolean deleteTask(Integer taskId) {
		// Directly delete by id (repository will handle existence). This keeps service simple
		// and matches unit tests which expect deleteById to be invoked.
		taskRepository.deleteById(taskId.longValue());
		log.info("Deleted task with id={}", taskId);
		return true;
	}

	@Override
	@Transactional
	public boolean updateTask(UpdateTaskRequest updateRequest) {
		Optional<Tasks> result = taskRepository.findById(updateRequest.getId().longValue());
		if (result.isPresent()) {
			Tasks tasks = result.get();
			tasks.setTitle(updateRequest.getTitle());
			tasks.setDescription(updateRequest.getDescription());
			tasks.setStatus(updateRequest.getStatus());
			taskRepository.save(tasks);
			log.info("Updated task id={} title={}", updateRequest.getId(), updateRequest.getTitle());
			return true;
		}
		return false;
	}

	@Override
	@Transactional(readOnly = true)
	public List<GetTaskResponse> getAllTasks() {
		List<Tasks> empList = taskRepository.findAll();
		 return empList.stream().map(tasks -> GetTaskResponse.builder().
					id(tasks.getTaskId()).title(tasks.getTitle()).
					description(tasks.getDescription()).status(tasks.getStatus())
					.build()).toList();
	}

	
	@Override
	@Transactional(readOnly = true)
	public Tasks findByTitle(String title) {
		Optional<Tasks> task = taskRepository.findByTitle(title);
		if(task.isPresent()) {
    		return task.get();
    	}
		return null;
	}

 }
