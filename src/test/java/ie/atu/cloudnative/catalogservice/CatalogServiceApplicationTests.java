package ie.atu.cloudnative.catalogservice;

import ie.atu.cloudnative.catalogservice.domain.Book;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

// Configures Spring Boot to start a full application context with a web server on a random port
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CatalogServiceApplicationTests {

    // Injects WebTestClient for making HTTP requests to the running test server
    @Autowired
    private WebTestClient webTestClient;

    @Test
    void whenGetRequestWithIdThenBookReturned() {
        var bookIsbn = "1231231230";
        var bookToCreate = new Book(bookIsbn, "Title", "Author", 9.90);

        // First, create a book to test retrieval
        Book expectedBook = webTestClient
                .post()
                .uri("/books")
                .bodyValue(bookToCreate)
                .exchange()
                // Verifies the response status is 201 Created
                .expectStatus().isCreated()
                // Validates the response body contains a non-null Book object
                .expectBody(Book.class).value(book -> assertThat(book).isNotNull())
                // Extracts and returns the Book object from the response for further use
                .returnResult().getResponseBody();

        // Now test retrieving the created book by ISBN
        webTestClient
                .get()
                .uri("/books/" + bookIsbn)
                .exchange()
                // Verifies any 2xx success status code
                .expectStatus().is2xxSuccessful()
                // Validates the returned book matches the expected book's ISBN
                .expectBody(Book.class).value(actualBook -> {
                    assertThat(actualBook).isNotNull();
                    assertThat(actualBook.isbn()).isEqualTo(expectedBook.isbn());
                });
    }

    @Test
    void whenPostRequestThenBookCreated() {
        var expectedBook = new Book("1231231231", "Title", "Author", 9.90);

        // Test creating a new book via POST request
        webTestClient
                .post()
                .uri("/books")
                .bodyValue(expectedBook)
                .exchange()
                // Expects 201 Created status for successful book creation
                .expectStatus().isCreated()
                // Validates the created book has the same ISBN as the input
                .expectBody(Book.class).value(actualBook -> {
                    assertThat(actualBook).isNotNull();
                    assertThat(actualBook.isbn()).isEqualTo(expectedBook.isbn());
                });
    }

    @Test
    void whenPutRequestThenBookUpdated() {
        var bookIsbn = "1231231232";
        var bookToCreate = new Book(bookIsbn, "Title", "Author", 9.90);

        // First, create a book to update
        Book createdBook = webTestClient
                .post()
                .uri("/books")
                .bodyValue(bookToCreate)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Book.class).value(book -> assertThat(book).isNotNull())
                .returnResult().getResponseBody();

        // Create an updated version with a new price
        var bookToUpdate = new Book(createdBook.isbn(), createdBook.title(), createdBook.author(), 7.95);

        // Test updating the book via PUT request
        webTestClient
                .put()
                .uri("/books/" + bookIsbn)
                .bodyValue(bookToUpdate)
                .exchange()
                // Expects 200 OK status for successful update
                .expectStatus().isOk()
                // Validates the updated book has the new price
                .expectBody(Book.class).value(actualBook -> {
                    assertThat(actualBook).isNotNull();
                    assertThat(actualBook.price()).isEqualTo(bookToUpdate.price());
                });
    }

    @Test
    void whenDeleteRequestThenBookDeleted() {
        var bookIsbn = "1231231233";
        var bookToCreate = new Book(bookIsbn, "Title", "Author", 9.90);

        // First, create a book to delete
        webTestClient
                .post()
                .uri("/books")
                .bodyValue(bookToCreate)
                .exchange()
                .expectStatus().isCreated();

        // Delete the book via DELETE request
        webTestClient
                .delete()
                .uri("/books/" + bookIsbn)
                .exchange()
                // Expects 204 No Content status for successful deletion
                .expectStatus().isNoContent();

        // Verify the book is actually deleted by attempting to retrieve it
        webTestClient
                .get()
                .uri("/books/" + bookIsbn)
                .exchange()
                // Expects 404 Not Found since the book was deleted
                .expectStatus().isNotFound()
                // Validates the error message content matches expected format
                .expectBody(String.class).value(errorMessage ->
                        assertThat(errorMessage).isEqualTo("No book with ISBN " + bookIsbn + " exists.")
                );
    }

}