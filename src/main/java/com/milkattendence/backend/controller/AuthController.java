package com.milkattendence.backend.controller;

import com.milkattendence.backend.model.User;
import com.milkattendence.backend.repository.UserRepository;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    // ❗ FIXED — removed colon so Spring loads actual email
    @Value("${spring.mail.username}")
    private String senderEmail;

    public AuthController(UserRepository userRepository, JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.mailSender = mailSender;
    }

    // ==========================================================
    // 🔵 LOGIN
    // ==========================================================
    @PostMapping("/login")
    public String login(@RequestBody User user) {
        Optional<User> found = userRepository.findByEmail(user.getEmail());
        if (found.isEmpty()) {
            return "User not found. Please register.";
        }
        if (!found.get().getPassword().equals(user.getPassword())) {
            return "Invalid password.";
        }
        return "Login successful!";
    }

    // ==========================================================
    // 🟢 REGISTER
    // ==========================================================
    @PostMapping("/register")
    public String register(@RequestBody User user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return "User already exists.";
        }
        userRepository.save(user);
        return "Registration successful!";
    }

    // ==========================================================
    // 🟣 FORGOT PASSWORD — SEND OTP
    // ==========================================================

    // Temporary store for OTPs
    private static class OtpInfo {
        String otp;
        LocalDateTime expiresAt;
        OtpInfo(String otp, LocalDateTime expiresAt) {
            this.otp = otp;
            this.expiresAt = expiresAt;
        }
    }

    private final ConcurrentHashMap<String, OtpInfo> otpStore = new ConcurrentHashMap<>();

    @PostMapping("/forgot")
    public Map<String, Object> forgotPassword(@RequestBody Map<String, Object> body) {

        String email = Objects.toString(body.get("email"), "").trim().toLowerCase();

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return Map.of("success", false, "error", "User not found");
        }

        try {
            // Generate 6-digit OTP
            String otp = String.valueOf(new Random().nextInt(900000) + 100000);

            // Save OTP for 10 minutes
            otpStore.put(email, new OtpInfo(otp, LocalDateTime.now().plusMinutes(10)));

            sendOtpEmail(email, otp);

            return Map.of("success", true, "message", "OTP sent to email");

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("success", false, "error", "Failed to send email");
        }
    }

    private void sendOtpEmail(String email, String otp) throws Exception {

        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");

        helper.setFrom(senderEmail);
        helper.setTo(email);
        helper.setSubject("Your OTP for Password Reset");

        String html =
                "<h3>Your OTP for resetting your password:</h3>" +
                "<h1 style='color:blue;'>" + otp + "</h1>" +
                "<p>This OTP is valid for <b>10 minutes</b>.</p>";

        helper.setText(html, true);
        mailSender.send(msg);
    }

    // ==========================================================
    // 🟠 RESET PASSWORD (VERIFY OTP)
    // ==========================================================
    @PostMapping("/reset")
    public Map<String, Object> resetPassword(@RequestBody Map<String, Object> body) {

        String email = Objects.toString(body.get("email"), "").trim().toLowerCase();
        String otp = Objects.toString(body.get("otp"), "").trim();
        String newPassword = Objects.toString(body.get("newPassword"), "").trim();

        if (email.isBlank() || otp.isBlank() || newPassword.isBlank()) {
            return Map.of("success", false, "error", "Missing fields");
        }

        OtpInfo info = otpStore.get(email);
        if (info == null) {
            return Map.of("success", false, "error", "OTP not found. Request again.");
        }

        if (LocalDateTime.now().isAfter(info.expiresAt)) {
            otpStore.remove(email);
            return Map.of("success", false, "error", "OTP expired. Request new OTP.");
        }

        if (!info.otp.equals(otp)) {
            return Map.of("success", false, "error", "Invalid OTP");
        }

        // OTP valid → update password
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return Map.of("success", false, "error", "User not found");
        }

        User user = userOpt.get();
        user.setPassword(newPassword);
        userRepository.save(user);

        // Remove OTP after success
        otpStore.remove(email);

        return Map.of("success", true, "message", "Password reset successful");
    }
}
