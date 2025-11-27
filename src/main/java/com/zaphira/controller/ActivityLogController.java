package com.zaphira.zaphira;

import com.zaphira.zaphira.ActivityLog;
import com.zaphira.zaphira.ActivityLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
public class ActivityLogController {

    @Autowired ActivityLogRepository repo;

    @GetMapping("/{userId}")
    public List<ActivityLog> getLogs(@PathVariable Long userId) {
        return repo.findByUserIdOrderByTimestampDesc(userId);
    }
}
