package ie.atu.cloudnative.catalogservice.domain;

import org.springframework.stereotype.Service;
// Stereotype annotation that marks a class to be a service managed by Spring
@Service
public class BookService {

    private final BookRepository bookRepository;


    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;   // BookRepository is provided through constructor autowiring.
    }

    public Iterable<Book> viewBookList() {
        return bookRepository.findAll();
    }

    public Book viewBookDetails(String isbn) {
        return bookRepository.findByIsbn(isbn)
                .orElseThrow(() -> new BookNotFoundException(isbn));    //When trying to view a book that doesn’t exist, a dedicated exception is thrown.
    }

    public Book addBookToCatalog(Book book) {
        if (bookRepository.existsByIsbn(book.isbn())) {
            throw new BookAlreadyExistsException(book.isbn());  // When adding the same book to the catalog multiple times, a dedicated exception is thrown
        }
        return bookRepository.save(book);
    }

    public void removeBookFromCatalog(String isbn) {
        bookRepository.deleteByIsbn(isbn);
    }

    public Book editBookDetails(String isbn, Book book) {
        return bookRepository.findByIsbn(isbn)
                .map(existingBook -> {
                    var bookToUpdate = new Book(    // When editing the book, all the Book fields can be updated except the ISBN code, because it’s the entity identifier.
                            existingBook.isbn(),
                            book.title(),
                            book.author(),
                            book.price());
                    return bookRepository.save(bookToUpdate);
                })
                .orElseGet(() -> addBookToCatalog(book)); //When changing the details for a book not in the catalog yet, create a new book.
    }

}