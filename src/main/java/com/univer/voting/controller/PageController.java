package com.univer.voting.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/activate")
    public String showActivatePage() {
        return "activate";
    }

    @GetMapping("/verify-email")
    public String showVerifyPage() {
        return "email-verified";
    }
}
