package com.ecommerce.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

// ── Auth ──────────────────────────────────────────────────────────────────────
public class AuthDto {

    @Data
    public static class RegisterRequest {
        @NotBlank private String fullName;
        @Email @NotBlank private String email;
        @NotBlank @Size(min = 6) private String password;
    }

    @Data
    public static class LoginRequest {
        @Email @NotBlank private String email;
        @NotBlank private String password;
    }

    @Data
    public static class AuthResponse {
        private String token;
        private String email;
        private String fullName;
        private String role;

        public AuthResponse(String token, String email, String fullName, String role) {
            this.token = token; this.email = email;
            this.fullName = fullName; this.role = role;
        }
    }
}
