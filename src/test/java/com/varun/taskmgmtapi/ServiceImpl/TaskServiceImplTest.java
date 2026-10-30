package com.varun.taskmgmtapi.ServiceImpl;


import com.varun.taskmgmtapi.dto.TaskDto.CreateTaskRequest;
import com.varun.taskmgmtapi.dto.TaskDto.TaskResponse;
import com.varun.taskmgmtapi.models.Task;
import com.varun.taskmgmtapi.models.User;
import com.varun.taskmgmtapi.repository.TaskRepo;
import com.varun.taskmgmtapi.repository.UserRepo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
}
