package com.varun.taskmgmtapi.ServiceImpl;


import com.varun.taskmgmtapi.dto.TaskDto.CreateTaskRequest;
import com.varun.taskmgmtapi.dto.TaskDto.TaskResponse;
import com.varun.taskmgmtapi.dto.TaskDto.UpdateTaskRequest;
import com.varun.taskmgmtapi.dto.TaskDto.UpdateTaskStatusRequest;
import com.varun.taskmgmtapi.exception.ResourceNotFoundException;
import com.varun.taskmgmtapi.exception.UserNotFoundException;
import com.varun.taskmgmtapi.models.Task;
import com.varun.taskmgmtapi.models.TaskStatus;
import com.varun.taskmgmtapi.models.User;
import com.varun.taskmgmtapi.repository.TaskRepo;
import com.varun.taskmgmtapi.repository.UserRepo;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.Test;

import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

//The test package should match your service's package:
//Mockito creates mock objects for these existing interfaces.
//@InjectMocks creates the real service, while @Mock supplies the substitute dependencies that the service receives through its constructor.


/*
mockito creates Mock obj and uses existing constructor to pass these Mock objs into real service
actual repository interface <<  >> remain unchanged. mockito creates objects for these existing interfaces
mock obj can be configured to return values and its interactions can be verified
 */

//class TaskServiceImpl{
/*private TaskRepo taskRepo;
private UserRepo userRepo;
public TaskServiceImpl(mock obj of TaskRepo,mock obj of UserRepo){
this.taskRepo=mock obj of TaskRepo;
this.userRepo=mock obj of UserRepo;

conceputally,
TaskRepo fakeTaskRepo = mock(TaskRepo.class);
UserRepo fakeUserRepo = mock(UserRepo.class);

TaskServiceImpl taskService =
        new TaskServiceImpl(fakeTaskRepo, fakeUserRepo);
}
 */
// }
//here we are testing the service business logic and we dont want to depend on a running DB
//Unit Test- we check a small,Isolated piece of code, such as one Method
/*
The testing framework (like JUnit 5) is responsible for creating the instance of the test class (e.g. TaskServiceImplTest)
right before it executes the test methods
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
public class TaskServiceImplTest {
    //mockito helps to create fake versions of dependencies like userRepo and TaskRepo so that we control what they should reutrn during a test
    @Mock
    //@Mock cretes mock of UserRepo
    //UserRepo mockUserRepo=mock(UserRepo.class)
    private UserRepo userRepo;

    @Mock
    //@Mock cretes mock of TaskRepo
    //TaskRepo mockTaskRepo=mock(TaskRepo.class)
    private TaskRepo taskRepo;

    @InjectMocks
    //Mockito instantiates this and injects the mock repository(created above) in its constructor
    //InjectMocks- creates new real TaskServiceImpl and injects above created mocks into its constructor
    //TaskServiceImpl taskServiceImpl=new TaskServiceImpl(mockUserRepo,mockTaskRepo)
    private TaskServiceImpl taskServiceImpl;

    @Test
    void getAllTasks_whenNoTasksExist_returnEmptyList() {

        //arrange : means prepare a task obj and configure mock behaviour
        when(taskRepo.findAll()).thenReturn(List.of());
        //hre taskRepo stores address of/points to (mock obj of TaskRepo Repository)
        //it tells Mockito "whenever service calls taskRepo.findAll() return and empty list
        //no actual DB query takes place

        //act : call the public method
        //here we are calling real Method and because, the below repo is mocked, it will return empty list, we configured above
        //List<Task> tasks=taskRepo.findAll()
        List<TaskResponse> result = taskServiceImpl.getAllTasks();

        //assert : check the converted response
        assertTrue(result.isEmpty());
        /*
        This assertion checks whether the returned list is empty.
        If it is empty, the test passes. If it isn't, the test fails
         */
        verify(taskRepo).findAll();
        /*
        This checks that the service actually called taskRepo.findAll().
        It is a separate check from asserting that the result is empty.
         */
    }


    @Test
    void getAllTasks_whenTasksExist_returnTasksResponse() {

        //Arrange
        Task task1 = new Task();
        task1.setTitle("learn java");
        task1.setDescription("practice Unit testing");


        Task task2 = new Task();
        task2.setTitle("learn Python");
        task2.setDescription("Implement using AI and ML");

        when(taskRepo.findAll()).thenReturn(List.of(task1, task2));

        //act
        List<TaskResponse> result = taskServiceImpl.getAllTasks();

        //assert
        assertEquals(2, result.size());
        assertEquals("learn java", result.get(0).getTitle());
        assertEquals("practice Unit testing", result.get(0).getDescription());

        assertEquals("learn Python", result.get(1).getTitle());
        assertEquals("Implement using AI and ML", result.get(1).getDescription());

    }

    @Test
    void createTask_whenUserIsNotAdmin_assignTaskToCurrentUser() {
        //arrange
        //first create logged in user
        User currentUser = new User();
        currentUser.setId(10L);
        currentUser.setName("varun");

        //then u tell the mock userRepo to return this user
        when(userRepo.findById(10L)).thenReturn(Optional.of(currentUser));

        //then prepare/arrange taskRequest
        CreateTaskRequest createTaskRequest = new CreateTaskRequest();
        createTaskRequest.setTaskName("scaler assignments and mock interviews");
        createTaskRequest.setTaskDescription("complete all pending assignments and mock interviews");


        //now act by calling actual service's method

        TaskResponse result = taskServiceImpl.createTask(createTaskRequest, 10L, false);

        //assert

        verify(taskRepo).save(any(Task.class));

        assertEquals(10L, result.getAssignedUserId());

    }

    @Test
    void createTask_whenAdminsAssignsTaskToAnotherUser_assignTaskCorrectly(){

        //arrange
        User assignedUser = new User();
        assignedUser.setId(20L);
        assignedUser.setName("rahul");

        when(userRepo.findById(20L)).thenReturn(Optional.of(assignedUser));

        CreateTaskRequest createTaskRequest = new CreateTaskRequest();
        createTaskRequest.setTaskName("Jobs search");
        createTaskRequest.setTaskDescription("apply for 5 posts daily");
        createTaskRequest.setAssignedUserId(20L);

        //act - current user is admin with id 10L and he is going to assign task to user with id- 20L
        TaskResponse result=taskServiceImpl.createTask(createTaskRequest, 10L, true);

        //assert
        verify(taskRepo).save(any(Task.class));
        assertEquals(20L,result.getAssignedUserId());
    }

    @Test //-- it marks a specific method as an executable testcase
    void createTask_whenAdminAssignsTaskToNonExistingUser_throwException(){
        //arrange
        when(userRepo.findById(100L)).thenReturn(Optional.empty());
        CreateTaskRequest createTaskRequest = new CreateTaskRequest();
        createTaskRequest.setTaskName("learn spring boot");
        createTaskRequest.setTaskDescription("practice rest APIs");
        createTaskRequest.setAssignedUserId(100L);


        //act
       // TaskResponse result=taskServiceImpl.createTask(createTaskRequest, 10L, true);
        //assert
        /*
        You cannot use assertThrows() on the result variable.
        The reason is that the exception is thrown when the method executes,
        before it can return a result.

        Remember this simple rule
        Scenario	                                    How to write it
        Method executes successfully	                Store its returned value in a variable, then assert it.
        Method is expected to throw an exception	    Put the method call inside assertThrows().
         */
        assertThrows(UserNotFoundException.class, ()->taskServiceImpl.createTask(createTaskRequest, 10L, true));
        //it means
        //execute createTask().I expect it to throw UserNotFoundException. if it throws, the test passes, else test fails
        verify(taskRepo,never()).save(any(Task.class));
        //this means "verify that taskrepo.save() was never called,
        // because, we should never save a task if its assigned user doesnot exist
    }

    @Test
    //admin access any task
    void getTaskById_whenUserIsAdmin_returnTaskResponse(){
        //arrange

        //create a task
        Task task=new Task();
        task.setId(1L);
        task.setTitle("learn Python and react");
        task.setDescription("learn basics of python and react and create project");
        //configure the mock repo
        when(taskRepo.findById(1L)).thenReturn(Optional.of(task));

        //act-call actual service's method
        TaskResponse result=taskServiceImpl.getTasksById(1L,10L,true);

        //assert
        //before checking the ID, verify that result is not null
        assertNotNull(result);
        assertEquals(1L,result.getId());
        assertEquals("learn Python and react", result.getTitle());
        assertEquals("learn basics of python and react and create project", result.getDescription());
        //verify
        verify(taskRepo).findById(1L);
        /*
        why we are not verifying whether convertToResponse() was called or not
        because, convertToResponse() is a private method inside TaskServiceImpl.
        Mockito's normal verify() mechanism verifies interactions with mock objects.
        Your convertToResponse() method is part of the real service object, not a mock dependency.
        Therefore, we cant simply write : verify(taskServiceImpl).convertToResponse(task);

        instead, Test the output produced by convertToResponse() indirectly. as mentioned above like title, description etc
        Because getTasksById() calls convertToResponse() and returns its result.
        If the conversion is incorrect, these assertions will catch it.
         */
    }

    @Test
    //user access own task
    void getTaskById_whenUserIsNotAdminButIsOwnerOfTask_returnTaskResponse(){

        User currentUser = new User();
        currentUser.setId(10L);
        currentUser.setName("varun");

        Task task=new Task();
        task.setId(2L);
        task.setTitle("learn HLD");
        task.setDescription("HLD is required for SDE2 and above roles");
        task.setAssignedUser(currentUser);


        when(taskRepo.findById(2L)).thenReturn(Optional.of(task));

        TaskResponse result=taskServiceImpl.getTasksById(2L,10L,false);

        assertNotNull(result);
        assertEquals(2L,result.getId());
        assertEquals("learn HLD", result.getTitle());
        assertEquals("HLD is required for SDE2 and above roles", result.getDescription());
        assertEquals(10L,result.getAssignedUserId());

        verify(taskRepo).findById(2L);
    }

    @Test
    void getTaskById_whenUserTriesToGetTaskofAnotherUser_throwException(){

        User currentUser = new User();
        currentUser.setId(10L);
        currentUser.setName("varun");

        User assignedUser = new User();
        assignedUser.setId(20L);

        Task task=new Task();
        task.setId(3L);
        task.setTitle("learn data engg");
        task.setDescription("data engg is required for SDE2 roles");
        task.setAssignedUser(assignedUser);

        when(taskRepo.findById(3L)).thenReturn(Optional.of(task));

        assertThrows(AccessDeniedException.class,
                ()-> taskServiceImpl.getTasksById(3L,10L,false));

        verify(taskRepo).findById(3L);
    }

    @Test
    void getTaskById_whenTaskDoesNotExist_throwException(){
        //arrange
        when(taskRepo.findById(5L)).thenReturn(Optional.empty());

        //act and assert
        assertThrows(ResourceNotFoundException.class,
                ()-> taskServiceImpl.getTasksById(5L,10L,true));

        //verify
        verify(taskRepo).findById(5L);
    }

    @Test
    void UpdateTask_whenUserIsNotAdminButOwnerOfTask_returnUpdatedTaskResponse(){

        User assignedUser = new User();
        assignedUser.setId(30L);

        Task task=new Task();
        task.setId(4L);
        task.setAssignedUser(assignedUser);

        UpdateTaskRequest updateTaskRequest = new UpdateTaskRequest();
        updateTaskRequest.setTaskName("learn spring boot");
        updateTaskRequest.setDescription("practice JWT authentication and spring security");
        updateTaskRequest.setStatus(TaskStatus.IN_PROGRESS);
        updateTaskRequest.setDueDate(LocalDate.of(2026,11,30));

        when(taskRepo.findById(4L)).thenReturn(Optional.of(task));
        when(taskRepo.save(any(Task.class))).thenReturn(task);

        TaskResponse result=taskServiceImpl.UpdateTask(4L,updateTaskRequest,30L,false);

        assertNotNull(result);
        assertEquals(4L,result.getId());
        assertEquals("learn spring boot", result.getTitle());
        assertEquals("practice JWT authentication and spring security", result.getDescription());
        assertEquals(30L,result.getAssignedUserId());
        assertEquals(LocalDate.of(2026,11,30),result.getDueDate());
        assertEquals(TaskStatus.IN_PROGRESS,result.getStatus());

        verify(taskRepo).findById(4L);
        verify(taskRepo).save(any(Task.class));
    }

    @Test
    void UpdateTask_whenAdminUpdatesAnotherUserTask_returnUpdatedTaskResponse(){
        //arrange

        User assignedUser = new User();
        assignedUser.setId(30L);

        Task task=new Task();
        task.setId(5L);
        task.setAssignedUser(assignedUser);

        when(taskRepo.findById(5L)).thenReturn(Optional.of(task));

        UpdateTaskRequest updateTaskRequest = new UpdateTaskRequest();
        updateTaskRequest.setTaskName("learn spring boot");
        updateTaskRequest.setDescription("practice JWT authentication and spring security");
        updateTaskRequest.setStatus(TaskStatus.DONE);
        //updateTaskRequest.setDueDate(LocalDate.of(2026,11,30));
        TaskResponse result=taskServiceImpl.UpdateTask(5L,updateTaskRequest,10L,true);

        assertNotNull(result);
        assertEquals(5L,result.getId());
        assertEquals("learn spring boot", result.getTitle());
        assertEquals("practice JWT authentication and spring security", result.getDescription());
        assertEquals(TaskStatus.DONE,result.getStatus());

        verify(taskRepo).findById(5L);
    }

    @Test
    void UpdateTask_whenUserTriesToUpdateAnotherUserTask_throwException(){

        User assignedUser = new User();
        assignedUser.setId(30L);

        Task task=new Task();
        task.setId(5L);
        task.setAssignedUser(assignedUser);

        UpdateTaskRequest updateTaskRequest = new UpdateTaskRequest();
        updateTaskRequest.setTaskName("learn spring boot");
        updateTaskRequest.setDescription("practice JWT authentication and spring security");
        updateTaskRequest.setStatus(TaskStatus.IN_PROGRESS);
        updateTaskRequest.setDueDate(LocalDate.of(2026,11,30));

        when(taskRepo.findById(5L)).thenReturn(Optional.of(task));

        assertThrows(AccessDeniedException.class,
                ()-> taskServiceImpl.UpdateTask(5L,updateTaskRequest,10L,false));

        verify(taskRepo).findById(5L);
        verify(taskRepo,never()).save(any(Task.class));
        /*These two lines are particularly important.
- findById() must be called to retrieve the task.
- save() must NOT be called because the user is not authorized to update it.
This confirms that your service rejects the request before saving anything.

         */
    }
    @Test
    void UpdateTask_whenTaskIdDoesNotExist_throwException(){
        when(taskRepo.findById(5L)).thenReturn(Optional.empty());
        UpdateTaskRequest updateTaskRequest = new UpdateTaskRequest();
        updateTaskRequest.setTaskName("learn spring boot");
        updateTaskRequest.setDescription("practice JWT authentication and spring security");
        updateTaskRequest.setStatus(TaskStatus.IN_PROGRESS);
        updateTaskRequest.setDueDate(LocalDate.of(2026,11,30));

        assertThrows(ResourceNotFoundException.class,
                ()->taskServiceImpl.UpdateTask(5L,updateTaskRequest,10L,false));

        verify(taskRepo).findById(5L);
        verify(taskRepo,never()).save(any(Task.class));
    }
@Test
    void ChangeTaskStatus_whenUserUpdatesOwnTaskStatusfromTODO2INPROGRESS_returnUpdatedTaskResponse(){

        User assignedUser = new User();
        assignedUser.setId(30L);

        Task task=new Task();
        task.setId(5L);
        task.setAssignedUser(assignedUser);
        task.setStatus(TaskStatus.TODO);

        UpdateTaskStatusRequest updateTaskStatusRequest = new UpdateTaskStatusRequest();
        updateTaskStatusRequest.setStatus(TaskStatus.IN_PROGRESS);

        when(taskRepo.findById(5L)).thenReturn(Optional.of(task));

        TaskResponse result=taskServiceImpl.changeTaskStatus(5L,updateTaskStatusRequest,30L,false);

        assertEquals(5L,result.getId());
        assertEquals(TaskStatus.IN_PROGRESS,result.getStatus());
        verify(taskRepo).findById(5L);
        verify(taskRepo).save(any(Task.class));
    }
@Test
    void ChangeTaskStatus_whenAdminChangesTaskStatusOfAnotherUserfromTODO2INPROGRESS_returnUpdatedTaskResponse(){
        User assignedUser = new User();
        assignedUser.setId(30L);
        Task task=new Task();
        task.setId(5L);
        task.setAssignedUser(assignedUser);
        task.setStatus(TaskStatus.TODO);

        when(taskRepo.findById(5L)).thenReturn(Optional.of(task));
        UpdateTaskStatusRequest updateTaskStatusRequest = new UpdateTaskStatusRequest();
        updateTaskStatusRequest.setStatus(TaskStatus.IN_PROGRESS);

        TaskResponse result= taskServiceImpl.changeTaskStatus(5L,updateTaskStatusRequest,20L,true);

        assertEquals(5L,result.getId());
        assertEquals(30L,result.getAssignedUserId());
        assertEquals(TaskStatus.IN_PROGRESS,result.getStatus());

        verify(taskRepo).findById(5L);
    }
    @Test
    void ChangeTaskStatus_whenUserTriesToUpdateAnotherUserTaskStatus_throwException(){
        User assignedUser = new User();
        assignedUser.setId(30L);

        Task task=new Task();
        task.setId(5L);
        task.setAssignedUser(assignedUser);
        task.setStatus(TaskStatus.TODO);

        when(taskRepo.findById(5L)).thenReturn(Optional.of(task));

        UpdateTaskStatusRequest updateTaskStatusRequest = new UpdateTaskStatusRequest();
        updateTaskStatusRequest.setStatus(TaskStatus.IN_PROGRESS);

        assertThrows(AccessDeniedException.class,
                ()->taskServiceImpl.changeTaskStatus(5L,updateTaskStatusRequest,20L,false));

        verify(taskRepo).findById(5L);
        verify(taskRepo,never()).save(any(Task.class));
    }
    @Test
void changeTaskStatus_whenUserTriesToUpdateOwnTaskStatusfromTODO2DONE_throwException(){

        User assignedUser = new User();
        assignedUser.setId(30L);
        Task task=new Task();
        task.setId(5L);
        task.setAssignedUser(assignedUser);
        task.setStatus(TaskStatus.TODO);

        when(taskRepo.findById(5L)).thenReturn(Optional.of(task));
        UpdateTaskStatusRequest updateTaskStatusRequest = new UpdateTaskStatusRequest();
        updateTaskStatusRequest.setStatus(TaskStatus.DONE);

        assertThrows(IllegalArgumentException.class,
                ()->taskServiceImpl.changeTaskStatus(5L,updateTaskStatusRequest,30L,false));

        verify(taskRepo).findById(5L);
        verify(taskRepo,never()).save(any(Task.class));
}

@Test
void changeTaskStatus_whenUserTriesToUpdateOwnTaskStatusfromInProgress2Done_returnUpdatedTaskResponse(){

        User assignedUser = new User();
        assignedUser.setId(30L);
        Task task=new Task();
        task.setId(5L);
        task.setAssignedUser(assignedUser);
        task.setStatus(TaskStatus.IN_PROGRESS);


        UpdateTaskStatusRequest updateTaskStatusRequest = new UpdateTaskStatusRequest();
        updateTaskStatusRequest.setStatus(TaskStatus.DONE);
        when(taskRepo.findById(5L)).thenReturn(Optional.of(task));

        TaskResponse result=taskServiceImpl.changeTaskStatus(5L,updateTaskStatusRequest,30L,false);

        assertEquals(5L,result.getId());
        assertEquals(30L,result.getAssignedUserId());
        assertEquals(TaskStatus.DONE,result.getStatus());

        verify(taskRepo).findById(5L);
        verify(taskRepo).save(any(Task.class));
}

    @Test
    void changeTaskStatus_whenUserTriesToUpdateOwnTaskStatusfromDones2InProgress_returnUpdatedTaskResponse(){
        User assignedUser = new User();
        assignedUser.setId(30L);
        Task task=new Task();
        task.setId(5L);
        task.setAssignedUser(assignedUser);
        task.setStatus(TaskStatus.DONE);


        UpdateTaskStatusRequest updateTaskStatusRequest = new UpdateTaskStatusRequest();
        updateTaskStatusRequest.setStatus(TaskStatus.IN_PROGRESS);
        when(taskRepo.findById(5L)).thenReturn(Optional.of(task));

        TaskResponse result=taskServiceImpl.changeTaskStatus(5L,updateTaskStatusRequest,30L,false);

        assertEquals(5L,result.getId());
        assertEquals(30L,result.getAssignedUserId());
        assertEquals(TaskStatus.IN_PROGRESS,result.getStatus());

        verify(taskRepo).findById(5L);
        verify(taskRepo).save(any(Task.class));
    }

    @Test
    void changeTaskStatus_whenUserTriesToChangeTaskStatusButTaskDoesNotExist_throwException(){

        when(taskRepo.findById(5L)).thenReturn(Optional.empty());
        UpdateTaskStatusRequest updateTaskStatusRequest = new UpdateTaskStatusRequest();
        updateTaskStatusRequest.setStatus(TaskStatus.TODO);

        assertThrows(ResourceNotFoundException.class,
                ()->taskServiceImpl.changeTaskStatus(5L,updateTaskStatusRequest,30L,false));

        verify(taskRepo).findById(5L);
        verify(taskRepo,never()).save(any(Task.class));
    }

    @Test
    void changeTaskStatus_whenTaskStatusChangesSuccessfully_checkIfUpdatedAtChangedOrNot(){
        User assignedUser = new User();
        assignedUser.setId(30L);
        Task task=new Task();
        task.setId(5L);
        task.setAssignedUser(assignedUser);
        task.setStatus(TaskStatus.TODO);
        task.setUpdatedAt(LocalDateTime.of(2026,11,5,18,29,30));

        when(taskRepo.findById(5L)).thenReturn(Optional.of(task));
        UpdateTaskStatusRequest updateTaskStatusRequest = new UpdateTaskStatusRequest();
        updateTaskStatusRequest.setStatus(TaskStatus.IN_PROGRESS);

        TaskResponse result=taskServiceImpl.changeTaskStatus(5L,updateTaskStatusRequest,30L,false);
        assertEquals(TaskStatus.IN_PROGRESS,result.getStatus());
        assertNotNull(result.getUpdatedAt());
        assertTrue(result.getUpdatedAt().isAfter(LocalDateTime.of(2026,11,5,18,29,30)));
        //suppose our service executes task.setUpdatedAt(LocalDateTime.now()) and
        //then test executes assertEquals(LocalDateTime.now(), result.getUpdatedAt());
        //these ar 2 separate calls to LocalDateTime.now()
        //so better approach is to set old UpdatedAt value in your Task before calling the service
        //and Then check whether the returned timestamp is later than the old timestamp.

        verify(taskRepo).findById(5L);
        verify(taskRepo).save(any(Task.class));
    }

    @Test
    void changeTaskStatus_whenInvalidTransitionAttempted_UpdatedAtShouldNotChange(){
        User assignedUser = new User();
        assignedUser.setId(30L);

        Task task=new Task();
        task.setId(5L);
        task.setAssignedUser(assignedUser);
        task.setStatus(TaskStatus.TODO);
        LocalDateTime updatedAt=LocalDateTime.of(2026,11,5,18,29,30);
        task.setUpdatedAt(updatedAt);

        UpdateTaskStatusRequest updateTaskStatusRequest = new UpdateTaskStatusRequest();
        updateTaskStatusRequest.setStatus(TaskStatus.DONE);

        when(taskRepo.findById(5L)).thenReturn(Optional.of(task));


        assertThrows(IllegalArgumentException.class,
                ()->taskServiceImpl.changeTaskStatus(5L,updateTaskStatusRequest,30L,false));
        assertEquals(updatedAt,task.getUpdatedAt());

                verify(taskRepo).findById(5L);
                verify(taskRepo,never()).save(any(Task.class));
    }
}

/*
Remember this testing principle
What are we testing?	How do we test it?
Repository interaction	verify(taskRepo)
Service output	assertEquals()
Exception behaviour	assertThrows()
Private method	Test indirectly through its public calling method
 */