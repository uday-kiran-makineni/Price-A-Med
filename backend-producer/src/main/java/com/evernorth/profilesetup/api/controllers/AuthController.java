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
import com.evernorth.profilesetup.service.jwt.JwtService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController { 
    @Autowired
    private AuthenticationManager authenticationManager;

    // @Autowired
    // private UserInfoService service; 

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