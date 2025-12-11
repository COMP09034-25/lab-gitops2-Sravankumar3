package ie.atu.cloudnative.catalogservice.domain;

public class BookNotFoundException extends RuntimeException {
    public BookNotFoundException(String isbn) {
        super("No book with ISBN " + isbn + " exists.");
    }
}
