package com.example.signupauth.controller;

import com.example.signupauth.model.Bug;
import com.example.signupauth.model.Bug.Priority;
import com.example.signupauth.model.Bug.Status;
import com.example.signupauth.model.User;
import com.example.signupauth.service.BugService;
import com.example.signupauth.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final BugService bugService;
    private final NotificationService notificationService;

    @GetMapping("/dashboard")
    public String dashboard(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String view,
            @RequestParam(required = false) Priority priority,
            Model model) {
        
        // Add notifications for upcoming bugs
        LocalDateTime now = LocalDateTime.now();
        List<Bug> upcomingBugs = notificationService.getUpcomingBugs(now);
        model.addAttribute("upcomingBugs", upcomingBugs);
        model.addAttribute("notificationService", notificationService);
        
        // Add available developers for assignment
        if (user.isTester()) {
            model.addAttribute("availableDevelopers", bugService.getAllDevelopers());
        }
        
        if ("priority".equals(view)) {
            model.addAttribute("bugs", bugService.getBugsByPriorityOrder(user));
            model.addAttribute("viewMode", "priority");
        } else {
            // Show different bugs based on role
            if (user.isDeveloper()) {
                model.addAttribute("openBugs", bugService.getBugsAssignedToUser(user, false));
                model.addAttribute("resolvedBugs", bugService.getBugsAssignedToUser(user, true));
            } else { // Tester
                model.addAttribute("openBugs", bugService.getBugsByReporterAndStatus(user, false));
                model.addAttribute("resolvedBugs", bugService.getBugsByReporterAndStatus(user, true));
            }
            model.addAttribute("viewMode", "standard");
        }
        
        if (priority != null) {
            model.addAttribute("bugs", bugService.getBugsByPriority(user, priority));
            model.addAttribute("viewMode", "priority");
            model.addAttribute("selectedPriority", priority);
        }

        model.addAttribute("newBug", new Bug());
        model.addAttribute("priorities", Priority.values());
        model.addAttribute("statuses", Status.values());
        model.addAttribute("userRole", user.getRole());
        return "dashboard";
    }

    @GetMapping("/bugs/search")
    public String searchBugs(
            @AuthenticationPrincipal User user,
            @RequestParam String query,
            Model model) {
        model.addAttribute("bugs", bugService.searchBugs(user, query));
        model.addAttribute("searchQuery", query);
        model.addAttribute("priorities", Priority.values());
        model.addAttribute("statuses", Status.values());
        return "search";
    }

    @GetMapping("/bugs/{bugId}/edit")
    public String editBugForm(
            @AuthenticationPrincipal User user,
            @PathVariable Long bugId,
            Model model) {
        Bug bug = bugService.getBugById(bugId);
        if (!bug.getReporterUser().getId().equals(user.getId())) {
            throw new RuntimeException("Not authorized to edit this bug");
        }
        model.addAttribute("bug", bug);
        model.addAttribute("priorities", Priority.values());
        model.addAttribute("statuses", Status.values());
        return "edit-bug";
    }

    @PostMapping("/bugs")
    public String createBug(
            @AuthenticationPrincipal User user,
            @ModelAttribute Bug bug,
            @RequestParam("dueDate") @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime dueDate,
            @RequestParam(required = false) Long assignedDeveloperId,
            RedirectAttributes redirectAttributes) {
        try {
            if (!user.isTester()) {
                throw new RuntimeException("Only testers can create bugs");
            }

            log.info("Creating new bug for user: {}", user.getUsername());
            
            User assignedDev = assignedDeveloperId != null ? 
                bugService.getUserById(assignedDeveloperId) : user;
            
            if (assignedDev != null && !assignedDev.isDeveloper()) {
                throw new RuntimeException("Can only assign bugs to developers");
            }
            
            bug.setReporterUser(user);
            bug.setCreatedBy(user);
            bug.setAssignedUser(assignedDev);
            bug.setReportedDate(LocalDateTime.now());
            bug.setDueDate(dueDate);
            bug.setResolved(false);
            bug.setStatus(Status.OPEN);
            
            Bug savedBug = bugService.saveBug(bug);
            log.info("Successfully created bug with ID: {}", savedBug.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Bug reported successfully!");
            return "redirect:/dashboard";
            
        } catch (Exception e) {
            log.error("Error creating bug: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Failed to report bug: " + e.getMessage());
            return "redirect:/dashboard";
        }
    }

    @PostMapping("/bugs/{bugId}/edit")
    public String updateBug(
            @AuthenticationPrincipal User user,
            @PathVariable Long bugId,
            @ModelAttribute Bug updatedBug,
            @RequestParam("dueDate") @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime dueDate,
            RedirectAttributes redirectAttributes) {
        try {
            Bug existingBug = bugService.getBugById(bugId);
            if (!existingBug.getReporterUser().getId().equals(user.getId())) {
                throw new RuntimeException("Not authorized to edit this bug");
            }
            
            updatedBug.setReporterUser(existingBug.getReporterUser());
            updatedBug.setAssignedUser(existingBug.getAssignedUser());
            updatedBug.setReportedDate(existingBug.getReportedDate());
            updatedBug.setDueDate(dueDate);
            bugService.updateBug(bugId, updatedBug);
            
            redirectAttributes.addFlashAttribute("successMessage", "Bug updated successfully!");
            return "redirect:/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update bug: " + e.getMessage());
            return "redirect:/bugs/" + bugId + "/edit";
        }
    }

    @PostMapping("/bugs/{bugId}/toggle")
    public String toggleBugResolution(
            @AuthenticationPrincipal User user,
            @PathVariable Long bugId,
            RedirectAttributes redirectAttributes) {
        try {
            Bug bug = bugService.getBugById(bugId);
            if (!bug.getReporterUser().getId().equals(user.getId()) && 
                !bug.getAssignedUser().getId().equals(user.getId())) {
                throw new RuntimeException("Not authorized to modify this bug");
            }
            bugService.toggleBugResolution(bugId);
            redirectAttributes.addFlashAttribute("successMessage", "Bug status updated successfully!");
            return "redirect:/dashboard";
        } catch (Exception e) {
            log.error("Error toggling bug resolution: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Failed to update bug status: " + e.getMessage());
            return "redirect:/dashboard";
        }
    }

    @PostMapping("/bugs/{bugId}/delete")
    public String deleteBug(
            @AuthenticationPrincipal User user,
            @PathVariable Long bugId,
            RedirectAttributes redirectAttributes) {
        try {
            Bug bug = bugService.getBugById(bugId);
            if (!bug.getReporterUser().getId().equals(user.getId())) {
                throw new RuntimeException("Not authorized to delete this bug");
            }
            bugService.deleteBug(bugId);
            redirectAttributes.addFlashAttribute("successMessage", "Bug deleted successfully!");
            return "redirect:/dashboard";
        } catch (Exception e) {
            log.error("Error deleting bug: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Failed to delete bug: " + e.getMessage());
            return "redirect:/dashboard";
        }
    }

    @PostMapping("/bugs/{bugId}/assign")
    public String assignBug(
            @AuthenticationPrincipal User user,
            @PathVariable Long bugId,
            @RequestParam Long developerId,
            RedirectAttributes redirectAttributes) {
        try {
            if (!user.isTester()) {
                throw new RuntimeException("Only testers can assign bugs");
            }
            bugService.assignBugToDeveloper(bugId, developerId, user);
            redirectAttributes.addFlashAttribute("successMessage", "Bug assigned successfully!");
            return "redirect:/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to assign bug: " + e.getMessage());
            return "redirect:/dashboard";
        }
    }

    @PostMapping("/bugs/{bugId}/status")
    public String updateBugStatus(
            @AuthenticationPrincipal User user,
            @PathVariable Long bugId,
            @RequestParam Status newStatus,
            @RequestParam(required = false) String statusNotes,
            RedirectAttributes redirectAttributes) {
        try {
            Bug bug = bugService.getBugById(bugId);
            
            // Verify permissions
            if (!bug.getAssignedUser().getId().equals(user.getId()) && 
                !bug.getReporterUser().getId().equals(user.getId())) {
                throw new RuntimeException("Not authorized to update this bug's status");
            }
            
            // Only developers can mark as IN_PROGRESS or RESOLVED
            if ((newStatus == Status.IN_PROGRESS || newStatus == Status.RESOLVED) && 
                !user.isDeveloper()) {
                throw new RuntimeException("Only developers can set this status");
            }
            
            // Only testers can mark as CLOSED
            if (newStatus == Status.CLOSED && !user.isTester()) {
                throw new RuntimeException("Only testers can close bugs");
            }
            
            bugService.updateBugStatus(bugId, newStatus, statusNotes);
            redirectAttributes.addFlashAttribute("successMessage", "Bug status updated successfully!");
            return "redirect:/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Failed to update bug status: " + e.getMessage());
            return "redirect:/dashboard";
        }
    }
} 