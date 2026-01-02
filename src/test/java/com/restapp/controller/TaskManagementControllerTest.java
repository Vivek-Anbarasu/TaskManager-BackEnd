package com.restapp.controller;

import com.restapp.dto.GetTaskResponse;
import com.restapp.dto.SaveTaskRequest;
import com.restapp.dto.UpdateTaskRequest;
import com.restapp.entity.Tasks;
import com.restapp.exception.BadRequest;
import com.restapp.exception.InternalServerError;
import com.restapp.exception.NotFound;
import com.restapp.service.TaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
    void getTaskReturnsTaskWhenTaskExists() throws Exception {
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
    void getTaskThrowsInternalServerErrorWhenServiceThrowsException() throws Exception {
        when(taskService.getTask(anyInt())).thenThrow(new RuntimeException("Database error"));

        assertThrows(InternalServerError.class, () -> taskManagementController.getTask(1));
    }

    @Test
    void saveTaskReturnsCreatedWhenTaskIsSavedSuccessfully() throws Exception {
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
    void saveTaskThrowsInternalServerErrorWhenSaveFails() throws Exception {
        SaveTaskRequest request = new SaveTaskRequest();
        request.setTitle("New Task");
        request.setDescription("Description");
        request.setStatus("To Do");

        when(taskService.findByTitle("New Task")).thenReturn(null);
        when(taskService.saveTask(any(SaveTaskRequest.class))).thenReturn(0);

        assertThrows(InternalServerError.class, () -> taskManagementController.saveTask(request));
    }

    @Test
    void saveTaskThrowsInternalServerErrorWhenServiceThrowsException() throws Exception {
        SaveTaskRequest request = new SaveTaskRequest();
        request.setTitle("New Task");
        request.setDescription("Description");
        request.setStatus("To Do");

        when(taskService.findByTitle(anyString())).thenThrow(new RuntimeException("Database error"));

        assertThrows(InternalServerError.class, () -> taskManagementController.saveTask(request));
    }

    @Test
    void updateTaskReturnsOkWhenTaskIsUpdatedSuccessfully() throws Exception {
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
    void updateTaskReturnsOkWhenTitleBelongsToSameTask() throws Exception {
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
    void updateTaskThrowsInternalServerErrorWhenServiceThrowsException() throws Exception {
        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setId(1);
        request.setTitle("Task");
        request.setDescription("Description");
        request.setStatus("To Do");

        when(taskService.findByTitle(anyString())).thenThrow(new RuntimeException("Database error"));

        assertThrows(InternalServerError.class, () -> taskManagementController.updateTask(request));
    }

    @Test
    void deleteTaskReturnsNoContentWhenTaskIsDeletedSuccessfully() throws Exception {
        GetTaskResponse response = GetTaskResponse.builder()
                .id(1)
                .title("Task to Delete")
                .description("Description")
                .status("To Do")
                .build();

        when(taskService.getTask(1)).thenReturn(response);
        when(taskService.deleteTask(1)).thenReturn(true);

        ResponseEntity<Void> result = taskManagementController.deleteTask(1);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        verify(taskService, times(1)).getTask(1);
        verify(taskService, times(1)).deleteTask(1);
    }



    @Test
    void deleteTaskThrowsInternalServerErrorWhenDeletionFails() throws Exception {
        GetTaskResponse response = GetTaskResponse.builder()
                .id(1)
                .title("Task")
                .description("Description")
                .status("To Do")
                .build();

        when(taskService.getTask(1)).thenReturn(response);
        when(taskService.deleteTask(1)).thenReturn(false);

        assertThrows(InternalServerError.class, () -> taskManagementController.deleteTask(1));
    }

    @Test
    void deleteTaskThrowsInternalServerErrorWhenServiceThrowsException() throws Exception {
        when(taskService.getTask(anyInt())).thenThrow(new RuntimeException("Database error"));

        assertThrows(InternalServerError.class, () -> taskManagementController.deleteTask(1));
    }

    @Test
    void getAllTasksReturnsListWhenTasksExist() throws Exception {
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
    void getAllTasksReturnsSingleTaskWhenOnlyOneTaskExists() throws Exception {
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
    void getAllTasksThrowsInternalServerErrorWhenServiceThrowsException() throws Exception {
        when(taskService.getAllTasks()).thenThrow(new RuntimeException("Database error"));

        assertThrows(InternalServerError.class, () -> taskManagementController.getAllTasks());
    }
}

