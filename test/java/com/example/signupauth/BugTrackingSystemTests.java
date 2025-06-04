package com.example.signupauth;

import com.example.signupauth.model.Bug;
import com.example.signupauth.model.User;
import com.example.signupauth.service.BugService;
import com.example.signupauth.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class BugTrackingSystemTests {

    @Autowired
    private BugService bugService;

    @Autowired
    private UserService userService;

    private User tester;
    private User developer;
    private Bug testBug;

    @BeforeEach
    void setUp() {
        // Create test users
        tester = new User();
        tester.setUsername("test.tester");
        tester.setPassword("password123");
        tester.setEmail("tester@test.com");
        tester.setFullName("Test Tester");
        tester.setRole(User.Role.TESTER);
        tester = userService.registerUser(tester);

        developer = new User();
        developer.setUsername("test.developer");
        developer.setPassword("password123");
        developer.setEmail("developer@test.com");
        developer.setFullName("Test Developer");
        developer.setRole(User.Role.DEVELOPER);
        developer = userService.registerUser(developer);

        // Create a test bug
        testBug = new Bug();
        testBug.setTitle("Test Bug");
        testBug.setDescription("This is a test bug");
        testBug.setSeverity(Bug.Severity.HIGH);
        testBug.setStatus(Bug.Status.OPEN);
        testBug.setReportedDate(LocalDateTime.now());
        testBug.setDueDate(LocalDateTime.now().plusDays(7));
        testBug.setReporterUser(tester);
        testBug.setAssignedUser(developer);
        testBug.setCreatedBy(tester);
        testBug = bugService.saveBug(testBug);
    }

    @Test
    @DisplayName("Test 1: Tester Creates Bug")
    @WithMockUser(username = "test.tester", roles = "TESTER")
    void testTesterCreatesBug() {
        Bug newBug = new Bug();
        newBug.setTitle("New Test Bug");
        newBug.setDescription("Description for new test bug");
        newBug.setSeverity(Bug.Severity.MEDIUM);
        newBug.setReportedDate(LocalDateTime.now());
        newBug.setDueDate(LocalDateTime.now().plusDays(5));
        newBug.setReporterUser(tester);
        newBug.setAssignedUser(developer);
        newBug.setCreatedBy(tester);

        Bug savedBug = bugService.saveBug(newBug);
        assertNotNull(savedBug.getId());
        assertEquals("New Test Bug", savedBug.getTitle());
        assertEquals(Bug.Status.OPEN, savedBug.getStatus());
        assertFalse(savedBug.isResolved());
    }

    @Test
    @DisplayName("Test 2: Developer Updates Bug Status")
    @WithMockUser(username = "test.developer", roles = "DEVELOPER")
    void testDeveloperUpdatesBugStatus() {
        // Test OPEN status
        testBug.setStatus(Bug.Status.OPEN);
        Bug updatedBug = bugService.updateBug(testBug.getId(), testBug);
        assertEquals(Bug.Status.OPEN, updatedBug.getStatus());
        assertFalse(updatedBug.isResolved());

        // Test IN_PROGRESS status
        testBug.setStatus(Bug.Status.IN_PROGRESS);
        updatedBug = bugService.updateBug(testBug.getId(), testBug);
        assertEquals(Bug.Status.IN_PROGRESS, updatedBug.getStatus());
        assertFalse(updatedBug.isResolved());

        // Test RESOLVED status
        testBug.setStatus(Bug.Status.RESOLVED);
        updatedBug = bugService.updateBug(testBug.getId(), testBug);
        assertEquals(Bug.Status.RESOLVED, updatedBug.getStatus());
        assertTrue(updatedBug.isResolved());
    }

    @Test
    @DisplayName("Test 3: Tester Manages Bug")
    @WithMockUser(username = "test.tester", roles = "TESTER")
    void testTesterManagesBug() {
        // Test editing bug
        testBug.setTitle("Updated Bug Title");
        testBug.setDescription("Updated description");
        Bug updatedBug = bugService.updateBug(testBug.getId(), testBug);
        assertEquals("Updated Bug Title", updatedBug.getTitle());
        assertEquals("Updated description", updatedBug.getDescription());

        // Test toggling resolution
        testBug.setResolved(true);
        updatedBug = bugService.updateBug(testBug.getId(), testBug);
        assertTrue(updatedBug.isResolved());

        // Test deleting bug
        bugService.deleteBug(testBug.getId());
        assertThrows(RuntimeException.class, () -> bugService.getBugById(testBug.getId()));
    }

    @Test
    @DisplayName("Test 4: Search Functionality")
    void testSearchFunctionality() {
        // Create multiple bugs with different titles
        createTestBug("UI Bug in Login", developer);
        createTestBug("Database Connection Error", developer);
        createTestBug("Performance Issue", developer);

        // Test search
        List<Bug> searchResults = bugService.searchBugs(developer, "UI");
        assertFalse(searchResults.isEmpty());
        assertTrue(searchResults.stream().anyMatch(bug -> bug.getTitle().contains("UI")));

        searchResults = bugService.searchBugs(developer, "Database");
        assertFalse(searchResults.isEmpty());
        assertTrue(searchResults.stream().anyMatch(bug -> bug.getTitle().contains("Database")));
    }

    @Test
    @DisplayName("Test 5: Bug Lifecycle")
    void testBugLifecycle() {
        // Test initial state
        assertEquals(Bug.Status.OPEN, testBug.getStatus());
        assertFalse(testBug.isResolved());

        // Test status transitions
        testBug.setStatus(Bug.Status.OPEN);
        Bug updatedBug = bugService.updateBug(testBug.getId(), testBug);
        assertEquals(Bug.Status.OPEN, updatedBug.getStatus());

        testBug.setStatus(Bug.Status.IN_PROGRESS);
        updatedBug = bugService.updateBug(testBug.getId(), testBug);
        assertEquals(Bug.Status.IN_PROGRESS, updatedBug.getStatus());

        testBug.setStatus(Bug.Status.RESOLVED);
        updatedBug = bugService.updateBug(testBug.getId(), testBug);
        assertEquals(Bug.Status.RESOLVED, updatedBug.getStatus());
        assertTrue(updatedBug.isResolved());
    }

    private Bug createTestBug(String title, User assignedDeveloper) {
        Bug bug = new Bug();
        bug.setTitle(title);
        bug.setDescription("Test description for " + title);
        bug.setSeverity(Bug.Severity.MEDIUM);
        bug.setStatus(Bug.Status.OPEN);
        bug.setReportedDate(LocalDateTime.now());
        bug.setDueDate(LocalDateTime.now().plusDays(7));
        bug.setReporterUser(tester);
        bug.setAssignedUser(assignedDeveloper);
        bug.setCreatedBy(tester);
        return bugService.saveBug(bug);
    }
} 