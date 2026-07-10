package net.gizmolab.library.librarymanagementsystemdesktop;

import app.angeasla.librarymanagementsystemdesktop.model.Author;
import app.angeasla.librarymanagementsystemdesktop.service.IAuthorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AuthorManagementIntegrationTest {

    @Autowired
    private IAuthorService authorService;

    @Test
    public void testCreateAndRetrieveAuthor() {
        // Create a new author
        Author author = new Author();
        author.setFirstname("John");
        author.setLastname("Doe");

        // Save the author
        Author savedAuthor = authorService.createAuthor(author);

        // Verify the author was saved
        assertNotNull(savedAuthor);
        assertNotNull(savedAuthor.getAuthorId());
        assertEquals("John", savedAuthor.getFirstname());
        assertEquals("Doe", savedAuthor.getLastname());

        // Retrieve the author by ID
        try {
            Author retrievedAuthor = authorService.getAuthorById(savedAuthor.getAuthorId());
            assertNotNull(retrievedAuthor);
            assertEquals(savedAuthor.getAuthorId(), retrievedAuthor.getAuthorId());
            assertEquals("John", retrievedAuthor.getFirstname());
            assertEquals("Doe", retrievedAuthor.getLastname());
        } catch (Exception e) {
            fail("Should be able to retrieve the saved author");
        }
    }

    @Test
    public void testGetAllAuthors() {
        // Create multiple authors
        Author author1 = new Author();
        author1.setFirstname("Jane");
        author1.setLastname("Smith");
        authorService.createAuthor(author1);

        Author author2 = new Author();
        author2.setFirstname("Bob");
        author2.setLastname("Johnson");
        authorService.createAuthor(author2);

        // Retrieve all authors
        List<Author> authors = authorService.getAllAuthors();
        assertNotNull(authors);
        assertTrue(authors.size() >= 2);

        // Check if our authors are in the list
        boolean foundJane = authors.stream().anyMatch(a -> "Jane".equals(a.getFirstname()) && "Smith".equals(a.getLastname()));
        boolean foundBob = authors.stream().anyMatch(a -> "Bob".equals(a.getFirstname()) && "Johnson".equals(a.getLastname()));
        
        assertTrue(foundJane);
        assertTrue(foundBob);
    }

    @Test
    public void testUpdateAuthor() {
        // Create an author
        Author author = new Author();
        author.setFirstname("Original");
        author.setLastname("Name");
        Author savedAuthor = authorService.createAuthor(author);

        // Update the author
        savedAuthor.setFirstname("Updated");
        savedAuthor.setLastname("NewName");
        try {
            Author updatedAuthor = authorService.updateAuthor(savedAuthor.getAuthorId(), savedAuthor);
            
            // Verify the update
            assertNotNull(updatedAuthor);
            assertEquals("Updated", updatedAuthor.getFirstname());
            assertEquals("NewName", updatedAuthor.getLastname());
        } catch (Exception e) {
            fail("Should be able to update the author");
        }
    }

    @Test
    public void testDeleteAuthor() throws Exception {
        // Create an author
        Author author = new Author();
        author.setFirstname("ToDelete");
        author.setLastname("Author");
        Author savedAuthor = authorService.createAuthor(author);
        Long authorId = savedAuthor.getAuthorId();

        // Verify the author exists
        try {
            Author retrievedAuthor = authorService.getAuthorById(authorId);
            assertNotNull(retrievedAuthor);
        } catch (Exception e) {
            fail("Author should exist before deletion");
        }

        // Delete the author
        authorService.deleteAuthor(authorId);

        // Verify the author is deleted (should throw exception)
        assertThrows(Exception.class, () -> {
            authorService.getAuthorById(authorId);
        });
    }

    @Test
    public void testAuthorWithOnlyLastName() {
        // Test creating an author with only last name (first name is optional)
        Author author = new Author();
        author.setLastname("OnlyLastName");
        
        Author savedAuthor = authorService.createAuthor(author);
        
        assertNotNull(savedAuthor);
        assertNotNull(savedAuthor.getAuthorId());
        assertNull(savedAuthor.getFirstname());
        assertEquals("OnlyLastName", savedAuthor.getLastname());
    }

    @Test
    public void testCountAuthors() {
        // Get initial count
        Long initialCount = authorService.countAuthors();
        
        // Add some authors
        Author author1 = new Author();
        author1.setFirstname("Count");
        author1.setLastname("Test1");
        authorService.createAuthor(author1);
        
        Author author2 = new Author();
        author2.setFirstname("Count");
        author2.setLastname("Test2");
        authorService.createAuthor(author2);
        
        // Verify count increased
        Long newCount = authorService.countAuthors();
        assertEquals(initialCount + 2, newCount);
    }
}