package ie.atu.cloudnative.catalogservice.domain;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.Disabled;

import static org.assertj.core.api.Assertions.assertThat;

class BookValidationTests {

    private static Validator validator;

    // Identifies a block of code executed before all tests in the class
    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // Identifies a test case
//    @Disabled
    @Test
    void whenAllFieldsCorrectThenValidationSucceeds() {
        //  Creates a book with a valid ISBN
        var book = new Book("1234567890", "Title", "Author", 9.90);
        Set<ConstraintViolation<Book>> violations = validator.validate(book);
        // Asserts that there is no validation error
        assertThat(violations).isEmpty();
    }

    @Test
//    @Disabled
    void whenIsbnNotDefinedThenValidationFails() {
        var book = new Book("", "Title", "Author", 9.90);
        Set<ConstraintViolation<Book>> violations = validator.validate(book);
        assertThat(violations).hasSize(2);
        List<String> constraintViolationMessages = violations.stream()
                .map(ConstraintViolation::getMessage).collect(Collectors.toList());
        assertThat(constraintViolationMessages)
                .contains("The book ISBN must be defined.")
                // Asserts that the violated validation constraint is about the incorrect ISBN
                .contains("The ISBN format must be valid.");
    }

//    @Disabled
    @Test
    void whenIsbnDefinedButIncorrectThenValidationFails() {
        // Creates a book with non-valid ISBN code
        var book = new Book("a234567890", "Title", "Author", 9.90);
        Set<ConstraintViolation<Book>> violations = validator.validate(book);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("The ISBN format must be valid.");
    }

//    @Disabled
    @Test
    void whenTitleIsNotDefinedThenValidationFails() {
        var book = new Book("1234567890", "", "Author", 9.90);
        Set<ConstraintViolation<Book>> violations = validator.validate(book);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("The book title must be defined.");
    }

//    @Disabled
    @Test
    void whenAuthorIsNotDefinedThenValidationFails() {
        var book = new Book("1234567890", "Title", "", 9.90);
        Set<ConstraintViolation<Book>> violations = validator.validate(book);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("The book author must be defined.");
    }

//    @Disabled
    @Test
    void whenPriceIsNotDefinedThenValidationFails() {
        var book = new Book("1234567890", "Title", "Author", null);
        Set<ConstraintViolation<Book>> violations = validator.validate(book);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("The book price must be defined.");
    }

//    @Disabled
    @Test
    void whenPriceDefinedButZeroThenValidationFails() {
        var book = new Book("1234567890", "Title", "Author", 0.0);
        Set<ConstraintViolation<Book>> violations = validator.validate(book);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("The book price must be greater than zero.");
    }

//    @Disabled
    @Test
    void whenPriceDefinedButNegativeThenValidationFails() {
        var book = new Book("1234567890", "Title", "Author", -9.90);
        Set<ConstraintViolation<Book>> violations = validator.validate(book);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("The book price must be greater than zero.");
    }
}