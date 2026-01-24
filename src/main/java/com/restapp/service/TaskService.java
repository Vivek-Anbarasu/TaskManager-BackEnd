package com.restapp.service;

import com.restapp.dto.GetTaskResponse;
import com.restapp.dto.SaveTaskRequest;
import com.restapp.dto.UpdateTaskRequest;
import com.restapp.entity.Tasks;

import java.util.List;

public interface TaskService 
{
	int saveTask(SaveTaskRequest saveRequest);

	GetTaskResponse getTask(Integer TaskId);

	boolean deleteTask(Integer TaskId);

	boolean updateTask(UpdateTaskRequest updateRequest);

	List<GetTaskResponse> getAllTasks();

	Tasks findByTitle(String title);
}
