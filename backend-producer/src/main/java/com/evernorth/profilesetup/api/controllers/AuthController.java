package com.evernorth.profilesetup.api.controllers;

// import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.evernorth.profilesetup.dto.LoginDTO;
import com.evernorth.profilesetup.dto.OtpDTO;
import com.evernorth.profilesetup.kafka.producer.OtpProducer;
import com.evernorth.profilesetup.service.database.entity.User;
import com.evernorth.profilesetup.service.database.repository.UserRepository;
import com.evernorth.profilesetup.service.jwt.JwtService;
import com.evernorth.profilesetup.service.redis.OtpQuery;
import com.evernorth.profilesetup.service.redis.entity.Otp;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController { 
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OtpProducer otpProducer;

    @Autowired
    private OtpQuery otpQuery;

    @Autowired
    private JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDTO payload, HttpServletResponse response) {
        System.out.println(payload.getUsername() + " " + payload.getPassword());
        
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(payload.getUsername(), payload.getPassword()));

            System.out.println("Authentication result: " + authentication.isAuthenticated());
            if (authentication.isAuthenticated()) {
                System.out.println("Authenticated");
                String jwtToken = jwtService.generateToken(payload.getUsername());
                Cookie cookie = new Cookie("jwt", jwtToken);
                cookie.setHttpOnly(true);
                cookie.setSecure(false); // Set to false for localhost (non-HTTPS)
                cookie.setPath("/");
                cookie.setMaxAge(1 * 24 * 60 * 60);
                response.addCookie(cookie);
                
                Map<String, Object> responseBody = new HashMap<>();
                responseBody.put("message", "Login Successful");
                responseBody.put("token", jwtToken);
                return ResponseEntity.ok(responseBody);
            } else {
                System.out.println("Unauthenticated");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("message", "Invalid user request!");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }
        } catch (Exception e) {
            System.out.println("Authentication error: " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Invalid credentials");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }


    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("jwt", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);

        response.addCookie(cookie);

        System.out.println("Logged out Successfully");
        return ResponseEntity.ok("Logged out successfully");
    }

    // Forgot Password - Step 1: Verify member ID and send OTP
    @PostMapping("/forgot-password/send-otp")
    public ResponseEntity<?> sendForgotPasswordOtp(@RequestBody Map<String, String> payload) {
        String memberID = payload.get("memberID");
        Map<String, Object> response = new HashMap<>();

        if (memberID == null || memberID.isEmpty()) {
            response.put("success", false);
            response.put("message", "Member ID is required");
            return ResponseEntity.badRequest().body(response);
        }

        Optional<User> userOptional = userRepository.findByMemberID(memberID);
        
        if (!userOptional.isPresent()) {
            response.put("success", false);
            response.put("message", "Member ID not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        User user = userOptional.get();
        String email = user.getEmail();
        String taskID = UUID.randomUUID().toString();

        // Create OTP DTO and send via Kafka
        OtpDTO otpDTO = new OtpDTO();
        otpDTO.setTaskID(taskID);
        otpDTO.setEmail(email);
        otpDTO.setType("forgot-password");

        try {
            otpProducer.publishPayload(otpDTO);
            
            // Mask email for response
            String maskedEmail = maskEmail(email);
            
            response.put("success", true);
            response.put("message", "OTP sent to " + maskedEmail);
            response.put("taskID", taskID);
            response.put("email", maskedEmail);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("Error sending OTP: " + e.getMessage());
            response.put("success", false);
            response.put("message", "Failed to send OTP");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Forgot Password - Step 2: Verify OTP
    @PostMapping("/forgot-password/verify-otp")
    public ResponseEntity<?> verifyForgotPasswordOtp(@RequestBody Map<String, String> payload) {
        String taskID = payload.get("taskID");
        String otp = payload.get("otp");
        Map<String, Object> response = new HashMap<>();

        if (taskID == null || otp == null) {
            response.put("success", false);
            response.put("message", "Task ID and OTP are required");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            Otp storedOtp = otpQuery.findById(taskID);
            
            if (storedOtp == null) {
                response.put("success", false);
                response.put("message", "OTP expired or invalid");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            if (storedOtp.getOtp().equals(otp)) {
                response.put("success", true);
                response.put("message", "OTP verified successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Invalid OTP");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        } catch (Exception e) {
            System.out.println("Error verifying OTP: " + e.getMessage());
            response.put("success", false);
            response.put("message", "Error verifying OTP");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Forgot Password - Step 3: Reset Password
    @PostMapping("/forgot-password/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> payload) {
        String memberID = payload.get("memberID");
        String taskID = payload.get("taskID");
        String newPassword = payload.get("password");
        Map<String, Object> response = new HashMap<>();

        if (memberID == null || taskID == null || newPassword == null) {
            response.put("success", false);
            response.put("message", "Member ID, Task ID and Password are required");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            // Verify OTP still exists (as proof of verification)
            Otp storedOtp = otpQuery.findById(taskID);
            if (storedOtp == null) {
                response.put("success", false);
                response.put("message", "Session expired. Please restart the process");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            Optional<User> userOptional = userRepository.findByMemberID(memberID);
            if (!userOptional.isPresent()) {
                response.put("success", false);
                response.put("message", "User not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            User user = userOptional.get();
            
            // Encode and update password
            PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            String encodedPassword = passwordEncoder.encode(newPassword);
            user.setPassword(encodedPassword);
            userRepository.save(user);

            // Delete OTP after successful password reset
            otpQuery.delete(taskID);

            response.put("success", true);
            response.put("message", "Password reset successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("Error resetting password: " + e.getMessage());
            response.put("success", false);
            response.put("message", "Error resetting password");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 2) {
            return email;
        }
        String prefix = email.substring(0, 2);
        String suffix = email.substring(atIndex - 1);
        return prefix + "***" + suffix;
    }


}


// public String generateAuthToken() {
//     final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
//     final int STRING_LENGTH = 32;
//     final int RANDOM_INDICES = 8;

//     SecureRandom random = new SecureRandom();
//     StringBuilder sb = new StringBuilder();
//     for (int i = 0; i < STRING_LENGTH; i++) {
//         sb.append(ALPHANUMERIC.charAt(random.nextInt(ALPHANUMERIC.length())));
//     }

//     StringBuilder otp = new StringBuilder();
//     for (int i = 0; i < RANDOM_INDICES; i++) {
//         otp.append(sb.charAt(random.nextInt(STRING_LENGTH)));
//     }

//     return otp.toString();
// }


// Optional<User> userOptional = userRepository.findByMemberID(payload.getMemberID());
        // Map<String, Object> response = new HashMap<>();

        // PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        // if(!userOptional.isPresent()){
        //     response.put("valid", false);
        //     response.put("message" , "Invalid MemberID");
        //     return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
 
        // } else {
        //     User user = userOptional.get();
        //     if(!passwordEncoder.matches(payload.getPassword(), user.getPassword())){
        //         response.put("valid", false);
        //         response.put("message", "Incorrect Password");
        //         return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        //     }

        //     String token = generateAuthToken();
        //     authRepository.save(new Auth(
        //         token,
        //         payload.getMemberID(),
        //         payload.getPassword()
        //     ));

        //     response.put("token", token);
        //     System.out.println(token);

        //     return ResponseEntity.ok(response);         


//  private final JwtService jwtService;
    
    // private final AuthenticationService authenticationService;

    // public Login(JwtService jwtService, AuthenticationService authenticationService) {
    //     this.jwtService = jwtService;
    //     this.authenticationService = authenticationService;
    // }




 // User authenticatedUser = authenticationService.authenticate(payload);

            // String jwtToken = jwtService.generateToken(authenticatedUser);

            // response.put("jwtToken", jwtToken);
            // response.put("jwtExpiration", jwtService.getExpirationTime());

            // return ResponseEntity.ok(response);


// Login

            // String token = "Hi";

            // response.put("valid", true);
            // response.put("token", token);