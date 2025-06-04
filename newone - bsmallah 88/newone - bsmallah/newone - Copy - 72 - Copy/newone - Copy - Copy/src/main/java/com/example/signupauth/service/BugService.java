package com.example.signupauth.service;

import com.example.signupauth.model.Bug;
import com.example.signupauth.model.Bug.Priority;
import com.example.signupauth.model.Bug.Status;
import com.example.signupauth.model.User;
import com.example.signupauth.model.User.Role;
import com.example.signupauth.repository.BugRepository;
import com.example.signupauth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BugService {

    private final BugRepository bugRepository;
    private final UserRepository userRepository;

    @Transactional
    public Bug saveBug(Bug bug) {
        if (bug.getCreatedBy() == null) {
            bug.setCreatedBy(bug.getReporterUser());
        }
        return bugRepository.save(bug);
    }

    public Bug getBugById(Long id) {
        return bugRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bug not found with id: " + id));
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    public List<User> getAllDevelopers() {
        return userRepository.findByRole(Role.DEVELOPER);
    }

    public List<Bug> getBugsByPriorityOrder(User user) {
        if (user.isDeveloper()) {
            return bugRepository.findByAssignedUserOrderByPriorityDesc(user);
        } else {
            return bugRepository.findByReporterUserOrderByPriorityDesc(user);
        }
    }

    public List<Bug> getBugsAssignedToUser(User user, boolean resolved) {
        return bugRepository.findByAssignedUserAndResolved(user, resolved);
    }

    public List<Bug> getBugsByReporterAndStatus(User user, boolean resolved) {
        return bugRepository.findByReporterUserAndResolved(user, resolved);
    }

    public List<Bug> getBugsByPriority(User user, Priority priority) {
        if (user.isDeveloper()) {
            return bugRepository.findByAssignedUserAndPriority(user, priority);
        } else {
            return bugRepository.findByReporterUserAndPriority(user, priority);
        }
    }

    public List<Bug> searchBugs(User user, String query) {
        if (user.isDeveloper()) {
            return bugRepository.findByAssignedUserAndTitleContainingIgnoreCase(user, query);
        } else {
            return bugRepository.findByReporterUserAndTitleContainingIgnoreCase(user, query);
        }
    }

    @Transactional
    public Bug updateBug(Long id, Bug updatedBug) {
        Bug existingBug = getBugById(id);
        existingBug.setTitle(updatedBug.getTitle());
        existingBug.setDescription(updatedBug.getDescription());
        existingBug.setPriority(updatedBug.getPriority());
        existingBug.setStatus(updatedBug.getStatus());
        existingBug.setDueDate(updatedBug.getDueDate());
        return bugRepository.save(existingBug);
    }

    @Transactional
    public Bug assignBugToDeveloper(Long bugId, Long developerId, User tester) {
        Bug bug = getBugById(bugId);
        User developer = getUserById(developerId);

        if (!tester.isTester()) {
            throw new RuntimeException("Only testers can assign bugs");
        }
        if (!developer.isDeveloper()) {
            throw new RuntimeException("Can only assign bugs to developers");
        }
        if (!bug.getReporterUser().getId().equals(tester.getId())) {
            throw new RuntimeException("Only the bug reporter can reassign the bug");
        }

        bug.setAssignedUser(developer);
        bug.setStatusChangeNotes("Reassigned to " + developer.getUsername());
        return bugRepository.save(bug);
    }

    @Transactional
    public Bug updateBugStatus(Long bugId, Status newStatus, String statusNotes) {
        Bug bug = getBugById(bugId);
        bug.setStatus(newStatus);
        bug.setResolved(newStatus == Status.RESOLVED || newStatus == Status.CLOSED);
        bug.setStatusChangeNotes(statusNotes);
        return bugRepository.save(bug);
    }

    @Transactional
    public void toggleBugResolution(Long id) {
        Bug bug = getBugById(id);
        bug.setResolved(!bug.isResolved());
        if (bug.isResolved()) {
            bug.setStatus(Status.RESOLVED);
        } else {
            bug.setStatus(Status.OPEN);
        }
        bugRepository.save(bug);
    }

    public void deleteBug(Long id) {
        bugRepository.deleteById(id);
    }
} 