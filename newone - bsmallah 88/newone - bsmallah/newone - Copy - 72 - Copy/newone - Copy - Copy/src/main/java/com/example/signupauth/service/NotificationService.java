package com.example.signupauth.service;

import com.example.signupauth.model.Bug;
import com.example.signupauth.repository.BugRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {
    
    private final BugRepository bugRepository;

    public List<Bug> getUpcomingBugs(LocalDateTime now) {
        return bugRepository.findByAssignedUserAndResolved(null, false).stream()
                .filter(bug -> {
                    LocalDateTime dueDate = bug.getDueDate();
                    return dueDate != null && 
                           ChronoUnit.HOURS.between(now, dueDate) <= 24 && 
                           ChronoUnit.HOURS.between(now, dueDate) > 0;
                })
                .collect(Collectors.toList());
    }

    public String getUrgencyClass(Bug bug) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dueDate = bug.getDueDate();
        
        if (dueDate == null) {
            return "";
        }

        long hours = ChronoUnit.HOURS.between(now, dueDate);
        
        if (hours < 0) {
            return "overdue";
        } else if (hours < 4) {
            return "urgent";
        } else if (hours < 24) {
            return "warning";
        } else {
            return "";
        }
    }

    public String getTimeRemainingText(Bug bug) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dueDate = bug.getDueDate();
        
        if (dueDate == null) {
            return "No due date set";
        }

        long hours = ChronoUnit.HOURS.between(now, dueDate);
        
        if (hours < 0) {
            return "Overdue";
        } else if (hours == 0) {
            return "Due now";
        } else if (hours < 24) {
            return hours + " hour" + (hours == 1 ? "" : "s") + " remaining";
        } else {
            long days = hours / 24;
            return days + " day" + (days == 1 ? "" : "s") + " remaining";
        }
    }
} 