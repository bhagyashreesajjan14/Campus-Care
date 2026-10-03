package com.campuscare.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class SuccessController {

    @GetMapping("/success")
    public String success(
            @RequestParam Long id,
            Model model) {

        model.addAttribute("id", id);

        return "success";
    }
}