package es.unizar.webeng.lab3

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

private val MANAGER_REQUEST_BODY = { name: String ->
    """
    { 
        "role": "Manager", 
        "name": "$name" 
    }
    """
}

private val MANAGER_RESPONSE_BODY = { name: String, id: Int ->
    """
    { 
       "name" : "$name",
       "role" : "Manager",
       "id" : $id
    }
    """
}

private val MANAGER_LIST_BODY =
    """
    [{
       "name" : "Mary",
       "role" : "Manager",
       "id" : 1
    }]
    """

@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
class ControllerTests {
    @Autowired
    private lateinit var mvc: MockMvc

    @MockkBean
    private lateinit var employeeRepository: EmployeeRepository

    @Test
    fun `POST is not safe and not idempotent`() {
        // SETUP
        // Initial state: the collection is empty. No employee exists.
        // Runtime state: the first save stores Mary, Manager, as id 1. The second save stores Mary, Manager, as id 2.
        // The mock repository plays the database: each call to save gives back
        // the employee with a new id (1 the first time, 2 the second time).
        every {
            employeeRepository.save(any<Employee>())
        } answers {
            Employee("Mary", "Manager", 1)
        } andThenAnswer {
            Employee("Mary", "Manager", 2)
        }

        mvc
            .post("/employees") {
                contentType = MediaType.APPLICATION_JSON
                content = MANAGER_REQUEST_BODY("Mary")
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isCreated() }
                header { string("Location", "http://localhost/employees/1") }
                content {
                    contentType(MediaType.APPLICATION_JSON)
                    json(MANAGER_RESPONSE_BODY("Mary", 1))
                }
            }

        mvc
            .post("/employees") {
                contentType = MediaType.APPLICATION_JSON
                content = MANAGER_REQUEST_BODY("Mary")
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isCreated() }
                header { string("Location", "http://localhost/employees/2") }
                content {
                    contentType(MediaType.APPLICATION_JSON)
                    json(MANAGER_RESPONSE_BODY("Mary", 2))
                }
            }

        // VERIFY
        // Not idempotent: two identical requests caused two saves, so two employees
        // (id 1 and id 2) now exist. The controller sets id = null before saving,
        // so the client can never choose the id.
        verify(exactly = 2) {
            employeeRepository.save(Employee("Mary", "Manager", null))
        }
        // Not safe: save is a write. POST only creates, so it must not
        // read one employee, list the collection, or delete anything.
        verify(exactly = 0) {
            employeeRepository.findById(any())
            employeeRepository.findAll()
            employeeRepository.deleteById(any())
        }
    }

    @Test
    fun `GET is safe and idempotent`() {
        // SETUP
        // Initial state: /employees/1 is Mary, Manager, id 1. /employees/2 has no representation.
        // Runtime state: both stay that way.
        TODO("Complete the mock setup for GET test")

        mvc.get("/employees/1").andExpect {
            status { isOk() }
            content {
                contentType(MediaType.APPLICATION_JSON)
                json(MANAGER_RESPONSE_BODY("Mary", 1))
            }
        }

        mvc.get("/employees/1").andExpect {
            status { isOk() }
            content {
                contentType(MediaType.APPLICATION_JSON)
                json(MANAGER_RESPONSE_BODY("Mary", 1))
            }
        }

        mvc.get("/employees/2").andExpect {
            status { isNotFound() }
        }

        mvc.get("/employees/2").andExpect {
            status { isNotFound() }
        }

        // VERIFY - COMPLETE ME!
        // Hint: Since GET is safe, what repository methods should NOT be called?
        // Count how many times each method was called based on the test requests.
        TODO("Complete the verification for GET test")
    }

    @Test
    fun `PUT is idempotent but not safe`() {
        // SETUP
        // Initial state: /employees/1 has no representation.
        // Runtime state: the first PUT stores Tom, Manager, at id 1. The second PUT leaves that same employee.
        TODO("Complete the mock setup for PUT test")

        mvc
            .put("/employees/1") {
                contentType = MediaType.APPLICATION_JSON
                content = MANAGER_REQUEST_BODY("Tom")
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isCreated() }
                header { string("Content-Location", "http://localhost/employees/1") }
                content {
                    contentType(MediaType.APPLICATION_JSON)
                    json(MANAGER_RESPONSE_BODY("Tom", 1))
                }
            }

        mvc
            .put("/employees/1") {
                contentType = MediaType.APPLICATION_JSON
                content = MANAGER_REQUEST_BODY("Tom")
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isOk() }
                header { string("Content-Location", "http://localhost/employees/1") }
                content {
                    contentType(MediaType.APPLICATION_JSON)
                    json(MANAGER_RESPONSE_BODY("Tom", 1))
                }
            }

        // VERIFY - COMPLETE ME!
        // Hint: What repository methods should be called for PUT operations?
        // Think about the controller logic and how many times each method should be invoked.
        TODO("Complete the verification for PUT test")
    }

    @Test
    fun `DELETE is idempotent but not safe`() {
        // SETUP
        // Initial state: /employees/1 is associated with an employee.
        // Runtime state: the first DELETE removes that association. The second DELETE finds it already gone.
        TODO("Complete the mock setup for DELETE test")
        mvc.delete("/employees/1").andExpect {
            status { isNoContent() }
        }

        mvc.delete("/employees/1").andExpect {
            status { isNoContent() }
        }

        // VERIFY
        // Hint: What repository methods should be called for DELETE operations?
        // What methods should NOT be called? Think about the nature of DELETE operations.
        TODO("Complete the verification for DELETE test")
    }

    @Test
    fun `GET collection is safe and idempotent`() {
        // SETUP - COMPLETE ME!
        // Initial state: the collection contains exactly Mary, Manager, id 1.
        // Runtime state: the second GET sees that same list.
        TODO("Complete the mock setup for the collection GET")

        mvc.get("/employees").andExpect {
            status { isOk() }
            content {
                contentType(MediaType.APPLICATION_JSON)
                json(MANAGER_LIST_BODY)
            }
        }

        mvc.get("/employees").andExpect {
            status { isOk() }
            content {
                contentType(MediaType.APPLICATION_JSON)
                json(MANAGER_LIST_BODY)
            }
        }

        // VERIFY - COMPLETE ME!
        // Hint: Count the list calls. A safe list must not save, delete, or fetch one id.
        TODO("Complete the verification for the collection GET")
    }
}
