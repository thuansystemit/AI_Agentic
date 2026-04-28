package com.darkness.videoplatform.controller;

import com.darkness.videoplatform.dto.AuthResponse;
import com.darkness.videoplatform.dto.LoginRequest;
import com.darkness.videoplatform.dto.RegisterRequest;
import com.darkness.videoplatform.entity.User;
import com.darkness.videoplatform.security.JwtAuthenticationFilter;
import com.darkness.videoplatform.security.JwtTokenProvider;
import com.darkness.videoplatform.service.AuthService;
import com.darkness.videoplatform.service.AuthService.TokenPair;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private User testUser() {
        return User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encoded")
                .build();
    }

    private TokenPair testTokenPair() {
        return new TokenPair("test-access-token", "test-refresh-token", testUser());
    }

    private AuthResponse testAuthResponse() {
        return AuthResponse.builder()
                .user(AuthResponse.UserResponse.builder()
                        .id(1L)
                        .username("testuser")
                        .email("test@example.com")
                        .build())
                .build();
    }

    @Test
    void register_shouldReturn201WithUser() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(testTokenPair());
        when(authService.toResponse(any(User.class))).thenReturn(testAuthResponse());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.username").value("testuser"))
                .andExpect(jsonPath("$.user.email").value("test@example.com"));
    }

    @Test
    void register_shouldReturn400WhenInvalidInput() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("")
                .email("not-an-email")
                .password("12")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_shouldReturn200WithUser() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(testTokenPair());
        when(authService.toResponse(any(User.class))).thenReturn(testAuthResponse());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("test@example.com"));
    }
}
