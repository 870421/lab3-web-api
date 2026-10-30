package es.unizar.webeng.lab3

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.justRun
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
import java.util.Optional

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
        // Employee 1 exists, so findById(1) always returns Mary.
        every {
            employeeRepository.findById(1)
        } answers {
            Optional.of(Employee("Mary", "Manager", 1))
        }
        // Employee 2 does not exist, so findById(2) is always empty (the controller answers 404).
        every {
            employeeRepository.findById(2)
        } answers {
            Optional.empty()
        }

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

        // VERIFY
        // Idempotent: each request is one read, so two requests mean two reads.
        // Both times we got the same answer (200 for id 1, 404 for id 2).
        verify(exactly = 2) {
            employeeRepository.findById(1)
        }
        verify(exactly = 2) {
            employeeRepository.findById(2)
        }
        // Safe: nothing was saved or deleted. GET of one item must not list the collection either.
        verify(exactly = 0) {
            employeeRepository.save(any())
            employeeRepository.deleteById(any())
            employeeRepository.findAll()
        }
    }

    @Test
    fun `PUT is idempotent but not safe`() {
        // SETUP
        // Initial state: /employees/1 has no representation.
        // Runtime state: the first PUT stores Tom, Manager, at id 1. The second PUT leaves that same employee.
        // First PUT: employee 1 does not exist yet, so the controller creates it (201).
        // Second PUT: employee 1 now exists, so the controller replaces it (200).
        every {
            employeeRepository.findById(1)
        } answers {
            Optional.empty()
        } andThenAnswer {
            Optional.of(Employee("Tom", "Manager", 1))
        }
        // Saving always stores Tom at id 1, because the id comes from the URI.
        every {
            employeeRepository.save(any<Employee>())
        } answers {
            Employee("Tom", "Manager", 1)
        }

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

        // VERIFY
        // Each PUT first checks whether employee 1 exists.
        verify(exactly = 2) {
            employeeRepository.findById(1)
        }
        // Not safe: each PUT writes with save.
        // Idempotent: both saves store exactly the same employee (Tom at id 1),
        // so after the second request the state is the same as after the first.
        verify(exactly = 2) {
            employeeRepository.save(Employee("Tom", "Manager", 1))
        }
        // PUT replaces one employee: it never deletes or lists the collection.
        verify(exactly = 0) {
            employeeRepository.deleteById(any())
            employeeRepository.findAll()
        }
    }

    @Test
    fun `DELETE is idempotent but not safe`() {
        // SETUP
        // Initial state: /employees/1 is associated with an employee.
        // Runtime state: the first DELETE removes that association. The second DELETE finds it already gone.
        // deleteById returns nothing, and it does not fail when the id is already gone.
        // The same stub is used for both DELETE requests.
        justRun {
            employeeRepository.deleteById(1)
        }

        mvc.delete("/employees/1").andExpect {
            status { isNoContent() }
        }

        mvc.delete("/employees/1").andExpect {
            status { isNoContent() }
        }

        // VERIFY
        // Not safe: deleteById is a write.
        // Idempotent: both calls target the same id, and after each one employee 1 is absent.
        verify(exactly = 2) {
            employeeRepository.deleteById(1)
        }
        // DELETE only deletes: it does not save, read one employee, or list the collection.
        verify(exactly = 0) {
            employeeRepository.save(any())
            employeeRepository.findById(any())
            employeeRepository.findAll()
        }
    }

    @Test
    fun `GET collection is safe and idempotent`() {
        // SETUP
        // Initial state: the collection contains exactly Mary, Manager, id 1.
        // Runtime state: the second GET sees that same list.
        // Nothing changes the collection, so findAll always returns the same list.
        every {
            employeeRepository.findAll()
        } answers {
            listOf(Employee("Mary", "Manager", 1))
        }

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

        // VERIFY
        // Idempotent: each request is one list read, and both returned the same list.
        verify(exactly = 2) {
            employeeRepository.findAll()
        }
        // Safe: nothing was saved or deleted. Listing does not read employees one by one either.
        verify(exactly = 0) {
            employeeRepository.save(any())
            employeeRepository.deleteById(any())
            employeeRepository.findById(any())
        }
    }
}
