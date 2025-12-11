package ie.atu.cloudnative.catalogservice.web;

import ie.atu.cloudnative.catalogservice.domain.BookNotFoundException;
import ie.atu.cloudnative.catalogservice.domain.BookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;


import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Configures a slice test for Spring MVC, loading only web layer components
// Focuses testing on the BookController class specifically, excluding other controllers
@WebMvcTest(BookController.class)
class BookControllerMvcTests {

    // Injects MockMvc for simulating HTTP requests in the mock web environment
    @Autowired
    private MockMvc mockMvc;

    // Creates a mock bean for BookService, replacing the real implementation
    // This isolates the controller test from the service layer dependencies
    @MockitoBean
    private BookService bookService;

    @Test
    void whenGetBookNotExistingThenShouldReturn404() throws Exception {
        String isbn = "73737313940";

        // Configures the mock service to throw BookNotFoundException when called
        // Uses BDD (Behavior-Driven Development) style mocking with given-when-then
        given(bookService.viewBookDetails(isbn)).willThrow(BookNotFoundException.class);

        // Performs a GET request to the /books/{isbn} endpoint using MockMvc
        mockMvc.perform(get("/books/" + isbn))
                // Verifies that the controller returns HTTP 404 Not Found status
                // Tests that the controller properly handles the BookNotFoundException
                .andExpect(status().isNotFound());
    }
}