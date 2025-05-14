package com.Mazade.project.WebApi.Controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PasswordController {

    @GetMapping("/resetPasswordPage")
    public String resetPasswordPage(@RequestParam String verificationCode, @RequestParam String email) {
        return "restPassword";
    }
}
