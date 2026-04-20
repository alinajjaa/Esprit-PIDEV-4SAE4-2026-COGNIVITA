package com.alzheimer.medicalrecords.controller;

import com.alzheimer.medicalrecords.entity.*;
import com.alzheimer.medicalrecords.repository.*;
import com.alzheimer.medicalrecords.service.*;
import com.alzheimer.medicalrecords.user.*;
import com.alzheimer.medicalrecords.dto.*;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class DashboardController {

    @GetMapping
    public String index() {
        return "redirect:/admin-dashboard.html";
    }

    @GetMapping("dashboard")
    public String dashboard() {
        return "redirect:/admin-dashboard.html";
    }

    @GetMapping("admin")
    public String admin() {
        return "redirect:/admin-dashboard.html";
    }
}
