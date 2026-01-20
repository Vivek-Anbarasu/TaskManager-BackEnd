package com.restapp.service;


import com.restapp.dao.TaskRepository;
import com.restapp.dto.GetTaskResponse;
import com.restapp.dto.SaveTaskRequest;
import com.restapp.dto.UpdateTaskRequest;
import com.restapp.entity.Tasks;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {
	
	private final TaskRepository taskRepository;

	@Override
	@Transactional
	public int saveTask(SaveTaskRequest saveRequest) throws Exception {
		Tasks tasks = Tasks.builder().title(saveRequest.getTitle()).
        description(saveRequest.getDescription()).status(saveRequest.getStatus()).build();
		Tasks savedTasks = taskRepository.save(tasks);
		return savedTasks.getTaskId();
	}

	@Override
	@Transactional
	public GetTaskResponse getTask(Integer taskId) throws Exception{
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
	public boolean deleteTask(Integer TaskId) throws Exception{
		taskRepository.deleteById(TaskId.longValue());
		return true;
	}

	@Override
	@Transactional
	public boolean updateTask(UpdateTaskRequest updateRequest) throws Exception {
		Optional<Tasks> result = taskRepository.findById(updateRequest.getId().longValue());
		if (result.isPresent()) {
			Tasks tasks = result.get();
			tasks.setTitle(updateRequest.getTitle());
			tasks.setDescription(updateRequest.getDescription());
			tasks.setStatus(updateRequest.getStatus());
			taskRepository.save(tasks);
			return true;
		}
		return false;
	}

	@Override
	@Transactional(readOnly = true)
	public List<GetTaskResponse> getAllTasks() throws Exception {
		List<Tasks> empList = taskRepository.findAll();
		 return empList.stream().map(tasks -> GetTaskResponse.builder().
					id(tasks.getTaskId()).title(tasks.getTitle()).
					description(tasks.getDescription()).status(tasks.getStatus())
					.build()).toList();
	}

	
	@Override
	@Transactional(readOnly = true)
	public Tasks findByTitle(String title) throws Exception {
		Optional<Tasks> task = taskRepository.findByTitle(title);
		  System.out.println("findByTitle ");
		if(task.isPresent()) {
    		return task.get();
    	}
		return null;
	}

}
