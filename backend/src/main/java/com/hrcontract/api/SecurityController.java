package com.hrcontract.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class SecurityController {
    private final AccessControl access;

    public SecurityController(AccessControl access) { this.access = access; }

    @GetMapping("/me")
    public Map<String, Object> me(HttpServletRequest request) { return access.currentUser(request); }
}
