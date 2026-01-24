package com.taskmanager.service;

import com.taskmanager.api.dto.*;

import com.taskmanager.domain.model.Tasks;

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
