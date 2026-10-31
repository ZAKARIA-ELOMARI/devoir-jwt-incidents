package com.enset.incidents.controller;

import com.enset.incidents.dto.Dtos.StatisticsDto;
import com.enset.incidents.dto.Dtos.UserDto;
import com.enset.incidents.service.AdminService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * /api/admin/** : reserve a ADMIN (double protection : SecurityConfig + @PreAuthorize).
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public List<UserDto> users() {
        return adminService.getAllUsers();
    }

    @PutMapping("/users/{id}/enable")
    public UserDto enable(@PathVariable Long id, Authentication auth) {
        return adminService.setEnabled(id, true, auth.getName());
    }

    @PutMapping("/users/{id}/disable")
    public UserDto disable(@PathVariable Long id, Authentication auth) {
        return adminService.setEnabled(id, false, auth.getName());
    }

    @GetMapping("/statistics")
    public StatisticsDto statistics() {
        return adminService.getStatistics();
    }
}
