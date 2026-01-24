package com.taskmanager.service;

import com.taskmanager.api.dto.*;
import com.taskmanager.domain.model.Tasks;
import com.taskmanager.domain.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskServiceImpl taskService;

    @Test
    void saveTaskReturnsTaskIdWhenTaskIsSavedSuccessfully() throws Exception {
        SaveTaskRequest request = new SaveTaskRequest();
        request.setTitle("New Task");
        request.setDescription("Task Description");
        request.setStatus("To Do");

        Tasks savedTask = new Tasks();
        savedTask.setTaskId(1);
        savedTask.setTitle("New Task");
        savedTask.setDescription("Task Description");
        savedTask.setStatus("To Do");

        when(taskRepository.save(any(Tasks.class))).thenReturn(savedTask);

        int result = taskService.saveTask(request);

        assertEquals(1, result);
        verify(taskRepository, times(1)).save(any(Tasks.class));
    }

    @Test
    void saveTaskSetsAllFieldsCorrectly() throws Exception {
        SaveTaskRequest request = new SaveTaskRequest();
        request.setTitle("Test Title");
        request.setDescription("Test Description");
        request.setStatus("In Progress");

        Tasks savedTask = new Tasks();
        savedTask.setTaskId(5);

        when(taskRepository.save(any(Tasks.class))).thenAnswer(invocation -> {
            Tasks task = invocation.getArgument(0);
            assertEquals("Test Title", task.getTitle());
            assertEquals("Test Description", task.getDescription());
            assertEquals("In Progress", task.getStatus());
            return savedTask;
        });

        int result = taskService.saveTask(request);

        assertEquals(5, result);
    }

    @Test
    void getTaskReturnsTaskResponseWhenTaskExists() throws Exception {
        Tasks task = new Tasks();
        task.setTaskId(1);
        task.setTitle("Existing Task");
        task.setDescription("Existing Description");
        task.setStatus("Done");

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        GetTaskResponse result = taskService.getTask(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("Existing Task", result.getTitle());
        assertEquals("Existing Description", result.getDescription());
        assertEquals("Done", result.getStatus());
        verify(taskRepository, times(1)).findById(1L);
    }

    @Test
    void getTaskReturnsNullWhenTaskDoesNotExist() throws Exception {
        when(taskRepository.findById(anyLong())).thenReturn(Optional.empty());

        GetTaskResponse result = taskService.getTask(99);

        assertNull(result);
        verify(taskRepository, times(1)).findById(99L);
    }

    @Test
    void getTaskConvertsIntegerIdToLong() throws Exception {
        when(taskRepository.findById(100L)).thenReturn(Optional.empty());

        taskService.getTask(100);

        verify(taskRepository, times(1)).findById(100L);
    }

    @Test
    void deleteTaskReturnsTrueWhenTaskIsDeleted() throws Exception {
        doNothing().when(taskRepository).deleteById(anyLong());

        boolean result = taskService.deleteTask(1);

        assertTrue(result);
        verify(taskRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteTaskConvertsIntegerIdToLong() throws Exception {
        doNothing().when(taskRepository).deleteById(50L);

        taskService.deleteTask(50);

        verify(taskRepository, times(1)).deleteById(50L);
    }

    @Test
    void updateTaskReturnsTrueWhenTaskExistsAndIsUpdated() throws Exception {
        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setId(1);
        request.setTitle("Updated Title");
        request.setDescription("Updated Description");
        request.setStatus("In Progress");

        Tasks existingTask = new Tasks();
        existingTask.setTaskId(1);
        existingTask.setTitle("Old Title");
        existingTask.setDescription("Old Description");
        existingTask.setStatus("To Do");

        when(taskRepository.findById(1L)).thenReturn(Optional.of(existingTask));
        when(taskRepository.save(any(Tasks.class))).thenReturn(existingTask);

        boolean result = taskService.updateTask(request);

        assertTrue(result);
        assertEquals("Updated Title", existingTask.getTitle());
        assertEquals("Updated Description", existingTask.getDescription());
        assertEquals("In Progress", existingTask.getStatus());
        verify(taskRepository, times(1)).findById(1L);
        verify(taskRepository, times(1)).save(existingTask);
    }

    @Test
    void updateTaskReturnsFalseWhenTaskDoesNotExist() throws Exception {
        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setId(999);
        request.setTitle("Updated Title");
        request.setDescription("Updated Description");
        request.setStatus("Done");

        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        boolean result = taskService.updateTask(request);

        assertFalse(result);
        verify(taskRepository, times(1)).findById(999L);
        verify(taskRepository, never()).save(any(Tasks.class));
    }

    @Test
    void updateTaskUpdatesOnlyProvidedFields() throws Exception {
        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setId(2);
        request.setTitle("New Title");
        request.setDescription("New Description");
        request.setStatus("Done");

        Tasks existingTask = new Tasks();
        existingTask.setTaskId(2);

        when(taskRepository.findById(2L)).thenReturn(Optional.of(existingTask));
        when(taskRepository.save(any(Tasks.class))).thenAnswer(invocation -> {
            Tasks task = invocation.getArgument(0);
            assertEquals("New Title", task.getTitle());
            assertEquals("New Description", task.getDescription());
            assertEquals("Done", task.getStatus());
            return task;
        });

        boolean result = taskService.updateTask(request);

        assertTrue(result);
    }

    @Test
    void getAllTasksReturnsListOfTaskResponsesWhenTasksExist() throws Exception {
        Tasks task1 = new Tasks();
        task1.setTaskId(1);
        task1.setTitle("Task 1");
        task1.setDescription("Description 1");
        task1.setStatus("To Do");

        Tasks task2 = new Tasks();
        task2.setTaskId(2);
        task2.setTitle("Task 2");
        task2.setDescription("Description 2");
        task2.setStatus("In Progress");

        Tasks task3 = new Tasks();
        task3.setTaskId(3);
        task3.setTitle("Task 3");
        task3.setDescription("Description 3");
        task3.setStatus("Done");

        when(taskRepository.findAll()).thenReturn(Arrays.asList(task1, task2, task3));

        List<GetTaskResponse> result = taskService.getAllTasks();

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(1, result.get(0).getId());
        assertEquals("Task 1", result.get(0).getTitle());
        assertEquals(2, result.get(1).getId());
        assertEquals("Task 2", result.get(1).getTitle());
        assertEquals(3, result.get(2).getId());
        assertEquals("Task 3", result.get(2).getTitle());
        verify(taskRepository, times(1)).findAll();
    }

    @Test
    void getAllTasksReturnsEmptyListWhenNoTasksExist() throws Exception {
        when(taskRepository.findAll()).thenReturn(Collections.emptyList());

        List<GetTaskResponse> result = taskService.getAllTasks();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(taskRepository, times(1)).findAll();
    }

    @Test
    void getAllTasksMapsAllFieldsCorrectly() throws Exception {
        Tasks task = new Tasks();
        task.setTaskId(10);
        task.setTitle("Complete Task");
        task.setDescription("Full Description");
        task.setStatus("Done");

        when(taskRepository.findAll()).thenReturn(Collections.singletonList(task));

        List<GetTaskResponse> result = taskService.getAllTasks();

        assertEquals(1, result.size());
        GetTaskResponse response = result.get(0);
        assertEquals(10, response.getId());
        assertEquals("Complete Task", response.getTitle());
        assertEquals("Full Description", response.getDescription());
        assertEquals("Done", response.getStatus());
    }

    @Test
    void findByTitleReturnsTaskWhenTitleExists() throws Exception {
        Tasks task = new Tasks();
        task.setTaskId(1);
        task.setTitle("Specific Title");
        task.setDescription("Description");
        task.setStatus("To Do");

        when(taskRepository.findByTitle("Specific Title")).thenReturn(Optional.of(task));

        Tasks result = taskService.findByTitle("Specific Title");

        assertNotNull(result);
        assertEquals(1, result.getTaskId());
        assertEquals("Specific Title", result.getTitle());
        verify(taskRepository, times(1)).findByTitle("Specific Title");
    }

    @Test
    void findByTitleReturnsNullWhenTitleDoesNotExist() throws Exception {
        when(taskRepository.findByTitle("Nonexistent Title")).thenReturn(Optional.empty());

        Tasks result = taskService.findByTitle("Nonexistent Title");

        assertNull(result);
        verify(taskRepository, times(1)).findByTitle("Nonexistent Title");
    }

    @Test
    void findByTitleHandlesNullTitle() throws Exception {
        when(taskRepository.findByTitle(null)).thenReturn(Optional.empty());

        Tasks result = taskService.findByTitle(null);

        assertNull(result);
        verify(taskRepository, times(1)).findByTitle(null);
    }

    @Test
    void findByTitleHandlesEmptyString() throws Exception {
        when(taskRepository.findByTitle("")).thenReturn(Optional.empty());

        Tasks result = taskService.findByTitle("");

        assertNull(result);
        verify(taskRepository, times(1)).findByTitle("");
    }
}

