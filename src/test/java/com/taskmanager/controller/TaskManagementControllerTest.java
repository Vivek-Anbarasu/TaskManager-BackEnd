package com.taskmanager.controller;


import com.taskmanager.api.controller.TaskManagementController;
import com.taskmanager.domain.model.Tasks;
import com.taskmanager.exception.BadRequest;
import com.taskmanager.exception.InternalServerError;
import com.taskmanager.exception.NotFound;
import com.taskmanager.service.TaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import com.taskmanager.api.dto.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskManagementControllerTest {

    @Mock
    private TaskService taskService;

    @InjectMocks
    private TaskManagementController taskManagementController;

    @Test
    void getTaskReturnsTaskWhenTaskExists() {
        GetTaskResponse response = GetTaskResponse.builder()
                .id(1)
                .title("Test Task")
                .description("Test Description")
                .status("To Do")
                .build();

        when(taskService.getTask(1)).thenReturn(response);

        ResponseEntity<GetTaskResponse> result = taskManagementController.getTask(1);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
        verify(taskService, times(1)).getTask(1);
    }



    @Test
    void getTaskThrowsInternalServerErrorWhenServiceThrowsException() {
        when(taskService.getTask(anyInt())).thenThrow(new RuntimeException("Database error"));

        assertThrows(RuntimeException.class, () -> taskManagementController.getTask(1));
    }

    @Test
    void saveTaskReturnsCreatedWhenTaskIsSavedSuccessfully() {
        SaveTaskRequest request = new SaveTaskRequest();
        request.setTitle("New Task");
        request.setDescription("New Description");
        request.setStatus("To Do");

        when(taskService.findByTitle("New Task")).thenReturn(null);
        when(taskService.saveTask(any(SaveTaskRequest.class))).thenReturn(1);

        ResponseEntity<String> result = taskManagementController.saveTask(request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals("1", result.getBody());
        verify(taskService, times(1)).findByTitle("New Task");
        verify(taskService, times(1)).saveTask(request);
    }



    @Test
    void saveTaskThrowsInternalServerErrorWhenSaveFails() {
        SaveTaskRequest request = new SaveTaskRequest();
        request.setTitle("New Task");
        request.setDescription("Description");
        request.setStatus("To Do");

        when(taskService.findByTitle("New Task")).thenReturn(null);
        when(taskService.saveTask(any(SaveTaskRequest.class))).thenReturn(0);

        assertThrows(InternalServerError.class, () -> taskManagementController.saveTask(request));
    }

    @Test
    void saveTaskThrowsInternalServerErrorWhenServiceThrowsException() {
        SaveTaskRequest request = new SaveTaskRequest();
        request.setTitle("New Task");
        request.setDescription("Description");
        request.setStatus("To Do");

        when(taskService.findByTitle(anyString())).thenThrow(new RuntimeException("Database error"));

        assertThrows(RuntimeException.class, () -> taskManagementController.saveTask(request));
    }

    @Test
    void updateTaskReturnsOkWhenTaskIsUpdatedSuccessfully() {
        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setId(1);
        request.setTitle("Updated Task");
        request.setDescription("Updated Description");
        request.setStatus("In Progress");

        when(taskService.findByTitle("Updated Task")).thenReturn(null);
        when(taskService.updateTask(any(UpdateTaskRequest.class))).thenReturn(true);

        ResponseEntity<String> result = taskManagementController.updateTask(request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("Successfully Updated", result.getBody());
        verify(taskService, times(1)).findByTitle("Updated Task");
        verify(taskService, times(1)).updateTask(request);
    }

    @Test
    void updateTaskReturnsOkWhenTitleBelongsToSameTask() {
        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setId(1);
        request.setTitle("Existing Task");
        request.setDescription("Updated Description");
        request.setStatus("In Progress");

        Tasks existingTask = new Tasks();
        existingTask.setTaskId(1);
        existingTask.setTitle("Existing Task");

        when(taskService.findByTitle("Existing Task")).thenReturn(existingTask);
        when(taskService.updateTask(any(UpdateTaskRequest.class))).thenReturn(true);

        ResponseEntity<String> result = taskManagementController.updateTask(request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("Successfully Updated", result.getBody());
    }





    @Test
    void updateTaskThrowsInternalServerErrorWhenServiceThrowsException() {
        com.taskmanager.api.dto.UpdateTaskRequest request = new UpdateTaskRequest();
        request.setId(1);
        request.setTitle("Task");
        request.setDescription("Description");
        request.setStatus("To Do");

        when(taskService.findByTitle(anyString())).thenThrow(new RuntimeException("Database error"));

        assertThrows(RuntimeException.class, () -> taskManagementController.updateTask(request));
    }

    @Test
    void deleteTaskReturnsNoContentWhenTaskIsDeletedSuccessfully() {
        GetTaskResponse response = GetTaskResponse.builder()
                .id(1)
                .title("Task to Delete")
                .description("Description")
                .status("To Do")
                .build();

        when(taskService.deleteTask(1)).thenReturn(true);

        ResponseEntity<Void> result = taskManagementController.deleteTask(1);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        verify(taskService, times(1)).deleteTask(1);
    }



    @Test
    void deleteTaskThrowsInternalServerErrorWhenDeletionFails() {
        GetTaskResponse response = GetTaskResponse.builder()
                .id(1)
                .title("Task")
                .description("Description")
                .status("To Do")
                .build();

        when(taskService.deleteTask(1)).thenReturn(false);

        assertThrows(NotFound.class, () -> taskManagementController.deleteTask(1));
    }

    @Test
    void deleteTaskThrowsInternalServerErrorWhenServiceThrowsException() {
        when(taskService.deleteTask(anyInt())).thenThrow(new RuntimeException("Database error"));

        assertThrows(RuntimeException.class, () -> taskManagementController.deleteTask(1));
    }

    @Test
    void getAllTasksReturnsListWhenTasksExist() {
        GetTaskResponse task1 = GetTaskResponse.builder()
                .id(1)
                .title("Task 1")
                .description("Description 1")
                .status("To Do")
                .build();

        GetTaskResponse task2 = GetTaskResponse.builder()
                .id(2)
                .title("Task 2")
                .description("Description 2")
                .status("In Progress")
                .build();

        List<GetTaskResponse> taskList = Arrays.asList(task1, task2);

        when(taskService.getAllTasks()).thenReturn(taskList);

        ResponseEntity<List<GetTaskResponse>> result = taskManagementController.getAllTasks();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(2, result.getBody().size());
        assertEquals(taskList, result.getBody());
        verify(taskService, times(1)).getAllTasks();
    }

    @Test
    void getAllTasksReturnsSingleTaskWhenOnlyOneTaskExists() {
        GetTaskResponse task = GetTaskResponse.builder()
                .id(1)
                .title("Single Task")
                .description("Description")
                .status("Done")
                .build();

        List<GetTaskResponse> taskList = Collections.singletonList(task);

        when(taskService.getAllTasks()).thenReturn(taskList);

        ResponseEntity<List<GetTaskResponse>> result = taskManagementController.getAllTasks();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(1, result.getBody().size());
        verify(taskService, times(1)).getAllTasks();
    }



    @Test
    void getAllTasksThrowsInternalServerErrorWhenServiceThrowsException() {
        when(taskService.getAllTasks()).thenThrow(new RuntimeException("Database error"));

        assertThrows(RuntimeException.class, () -> taskManagementController.getAllTasks());
    }

    @Test
    void saveTaskReturnsBadRequestWhenTitleAlreadyExists() {
        SaveTaskRequest request = new SaveTaskRequest();
        request.setTitle("Existing");
        request.setDescription("Desc");
        request.setStatus("To Do");

        Tasks existing = new Tasks();
        existing.setTaskId(2);
        existing.setTitle("Existing");

        when(taskService.findByTitle("Existing")).thenReturn(existing);

        assertThrows(BadRequest.class, () -> taskManagementController.saveTask(request));
        verify(taskService, times(1)).findByTitle("Existing");
        verify(taskService, never()).saveTask(any());
    }

    @Test
    void updateTaskThrowsBadRequestWhenTitleBelongsToOtherTask() {
        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setId(1);
        request.setTitle("Existing");
        request.setDescription("Desc");
        request.setStatus("To Do");

        Tasks existing = new Tasks();
        existing.setTaskId(2); // different id
        existing.setTitle("Existing");

        when(taskService.findByTitle("Existing")).thenReturn(existing);

        assertThrows(BadRequest.class, () -> taskManagementController.updateTask(request));
        verify(taskService, times(1)).findByTitle("Existing");
    }

    @Test
    void updateTaskThrowsNotFoundWhenUpdateReturnsFalse() {
        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setId(1);
        request.setTitle("New");
        request.setDescription("Desc");
        request.setStatus("To Do");

        when(taskService.findByTitle("New")).thenReturn(null);
        when(taskService.updateTask(any(UpdateTaskRequest.class))).thenReturn(false);

        assertThrows(NotFound.class, () -> taskManagementController.updateTask(request));
        verify(taskService, times(1)).updateTask(request);
    }

    @Test
    void deleteTaskThrowsNotFoundWhenTaskDoesNotExist() {
        when(taskService.deleteTask(1)).thenReturn(false);

        assertThrows(NotFound.class, () -> taskManagementController.deleteTask(1));
        verify(taskService, times(1)).deleteTask(1);
    }

    @Test
    void getAllTasksThrowsNotFoundWhenEmptyList() {
        when(taskService.getAllTasks()).thenReturn(Collections.emptyList());

        assertThrows(NotFound.class, () -> taskManagementController.getAllTasks());
        verify(taskService, times(1)).getAllTasks();
    }
}
