package net.gizmolab.library.librarymanagementsystemdesktop;

import app.angeasla.librarymanagementsystemdesktop.model.Publisher;
import app.angeasla.librarymanagementsystemdesktop.service.IPublisherService;
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
public class PublisherManagementIntegrationTest {

    @Autowired
    private IPublisherService publisherService;

    @Test
    public void testCreatePublisher() {
        // Create a new publisher
        Publisher publisher = new Publisher();
        publisher.setName("Test Publisher");

        // Save the publisher
        Publisher savedPublisher = publisherService.createPublisher(publisher);

        // Verify the publisher was saved
        assertNotNull(savedPublisher);
        assertNotNull(savedPublisher.getPublisherId());
        assertEquals("Test Publisher", savedPublisher.getName());
    }

    @Test
    public void testGetAllPublishers() {
        // Create test publishers
        Publisher publisher1 = new Publisher();
        publisher1.setName("Publisher One");
        publisherService.createPublisher(publisher1);

        Publisher publisher2 = new Publisher();
        publisher2.setName("Publisher Two");
        publisherService.createPublisher(publisher2);

        // Get all publishers
        List<Publisher> publishers = publisherService.getAllPublishers();

        // Verify publishers were retrieved
        assertNotNull(publishers);
        assertTrue(publishers.size() >= 2);
        
        // Check if our test publishers are in the list
        boolean foundPublisher1 = publishers.stream()
                .anyMatch(p -> "Publisher One".equals(p.getName()));
        boolean foundPublisher2 = publishers.stream()
                .anyMatch(p -> "Publisher Two".equals(p.getName()));
        
        assertTrue(foundPublisher1);
        assertTrue(foundPublisher2);
    }

    @Test
    public void testUpdatePublisher() throws Exception {
        // Create a publisher
        Publisher publisher = new Publisher();
        publisher.setName("Original Publisher");
        Publisher savedPublisher = publisherService.createPublisher(publisher);

        // Update the publisher
        savedPublisher.setName("Updated Publisher");
        Publisher updatedPublisher = publisherService.updatePublisher(savedPublisher.getPublisherId(), savedPublisher);

        // Verify the update
        assertNotNull(updatedPublisher);
        assertEquals("Updated Publisher", updatedPublisher.getName());
        assertEquals(savedPublisher.getPublisherId(), updatedPublisher.getPublisherId());
    }

    @Test
    public void testDeletePublisher() throws Exception {
        // Create a publisher
        Publisher publisher = new Publisher();
        publisher.setName("Publisher to Delete");
        Publisher savedPublisher = publisherService.createPublisher(publisher);

        // Get initial count
        List<Publisher> initialPublishers = publisherService.getAllPublishers();
        int initialCount = initialPublishers.size();

        // Delete the publisher
        publisherService.deletePublisher(savedPublisher.getPublisherId());

        // Verify deletion
        List<Publisher> remainingPublishers = publisherService.getAllPublishers();
        assertEquals(initialCount - 1, remainingPublishers.size());
        
        // Verify the specific publisher is not in the list
        boolean publisherExists = remainingPublishers.stream()
                .anyMatch(p -> savedPublisher.getPublisherId().equals(p.getPublisherId()));
        assertFalse(publisherExists);
    }

    @Test
    public void testGetPublisherByName() {
        // Create a publisher
        Publisher publisher = new Publisher();
        publisher.setName("Unique Publisher Name");
        publisherService.createPublisher(publisher);

        // Search by name
        Publisher foundPublisher = publisherService.getPublisherByName("Unique Publisher Name");

        // Verify the publisher was found
        assertNotNull(foundPublisher);
        assertEquals("Unique Publisher Name", foundPublisher.getName());
    }
}