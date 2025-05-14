package com.Mazade.project.Core.Servecies;

import com.Mazade.project.Common.DTOs.LoginDTO;
import com.Mazade.project.Common.DTOs.PaginationDTO;
import com.Mazade.project.Common.Entities.Email;
import com.Mazade.project.Common.Entities.Token;
import com.Mazade.project.Common.Entities.User;
import com.Mazade.project.Common.Enums.Role;
import com.Mazade.project.Common.Enums.Status;
import com.Mazade.project.Common.Enums.TokenType;
import com.Mazade.project.Common.Responses.AuthenticationResponse;
import com.Mazade.project.Common.Responses.GeneralResponse;
import com.Mazade.project.Core.Repsitories.EmailRepository;
import com.Mazade.project.Core.Repsitories.TokenRepository;
import com.Mazade.project.Core.Repsitories.UserRepository;
import com.Mazade.project.WebApi.Config.JwtService;
import com.Mazade.project.WebApi.Exceptions.UserNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserRepository repository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final EmailRepository emailRepository;

    @Transactional
    public AuthenticationResponse adduser(User user) throws UserNotFoundException, IOException {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        var savedUser = repository.save(user);
        var jwtToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);
        saveUserToken(savedUser, jwtToken);
        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .message("User " + user.getRole() + " added successfully")
                .build();
    }
//
////    @Transactional
////    public GeneralResponse UpdateUser(UserUpdateDTO userRequest, Long id) throws UserNotFoundException {
////        var user = repository.findById(id)
////                .orElseThrow(() -> new UserNotFoundException("User not found"));
////        user.setAddress(userRequest.getAddress());
////        user.setDateOfBirth(userRequest.getDateOfBirth());
////        user.setFirstName(userRequest.getFirstName());
////        user.setLastName(userRequest.getLastName());
////        user.setPhone(userRequest.getPhone());
////        user.setImage(userRequest.getImage());
////        repository.save(user);
////        return GeneralResponse.builder()
////                .message("User updated successfully")
////                .build();
////    }
//
//
//
//
//    @Transactional
//    public PaginationDTO<User> GetAllUsers(int page, int size, String search, Role role) {
//
//        if(search != null && search.isEmpty()){
//            search = null;
//        }
//        if(role != null && !EnumSet.allOf(Role.class).contains(role)){
//            role = null;
//        }
//        if (page < 1) {
//            page = 1;
//        }
//        PageRequest pageRequest = PageRequest.of(page - 1, size);
//        Page<User> userPage = repository.findAll(pageRequest, search, role);
//
//        PaginationDTO<User> paginationDTO = new PaginationDTO<>();
//        paginationDTO.setTotalElements(userPage.getTotalElements());
//        paginationDTO.setTotalPages(userPage.getTotalPages());
//        paginationDTO.setSize(userPage.getSize());
//        paginationDTO.setNumber(userPage.getNumber() + 1);
//        paginationDTO.setNumberOfElements(userPage.getNumberOfElements());
//        paginationDTO.setContent(userPage.getContent());
//
//        return paginationDTO;
//    }
//
//    @Transactional
//    public Page<User> getAllUsersByRole(Role role, int page, int size) {
//        if (page < 1) {
//            page = 1;
//        }
//        Pageable pageable = PageRequest.of(page - 1, size);
//        return repository.findAllByRole(role, pageable);
//    }
//
//    @Transactional
//    public PaginationDTO<User> getAllDeletedUsers(int page, int size, String search, Role role) {
//        if(search != null && search.isEmpty()){
//            search = null;
//        }
//        if(role != null && !EnumSet.allOf(Role.class).contains(role)){
//            role = null;
//        }
//        if (page < 1) {
//            page = 1;
//        }
//        PageRequest pageRequest = PageRequest.of(page - 1, size);
//        Page<User> userPage = repository.findAllDeleted(pageRequest, search, role);
//
//        PaginationDTO<User> paginationDTO = new PaginationDTO<>();
//        paginationDTO.setTotalElements(userPage.getTotalElements());
//        paginationDTO.setTotalPages(userPage.getTotalPages());
//        paginationDTO.setSize(userPage.getSize());
//        paginationDTO.setNumber(userPage.getNumber() + 1);
//        paginationDTO.setNumberOfElements(userPage.getNumberOfElements());
//        paginationDTO.setContent(userPage.getContent());
//
//        return paginationDTO;
//    }
//    public GeneralResponse restoreUser(Long id) throws UserNotFoundException {
//        var user = repository.findDeletedById(id)
//                .orElseThrow(() -> new UserNotFoundException("User not found"));
//        user.setStatus(Status.ACTIVE);
//        repository.save(user);
//        return GeneralResponse.builder()
//                .message("User restored successfully")
//                .build();
//    }
//
//
//
//
//
    @Transactional
    public AuthenticationResponse authenticate(LoginDTO request) throws UserNotFoundException {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        var user = repository.findByEmail(request.getEmail(),Status.ACTIVE)
                .orElseThrow(
                        () -> new UserNotFoundException("User not found")
                );
        var jwtToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);
        revokeAllUserTokens(user);
        saveUserToken(user, jwtToken);
        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .message("User LoggedIn successfully")
                .build();
    }

    private void saveUserToken(User user, String jwtToken) {
        var token = Token.builder()
                .user(user)
                .token(jwtToken)
                .tokenType(TokenType.BEARER)
                .expired(false)
                .revoked(false)
                .build();
        tokenRepository.save(token);
    }
//
//
    private void revokeAllUserTokens(User user) {
        var validUserTokens = tokenRepository.findAllValidTokenByUser(user.getId());
        if (validUserTokens.isEmpty())
            return;
        validUserTokens.forEach(token -> {
            token.setExpired(true);
            token.setRevoked(true);
        });
        tokenRepository.saveAll(validUserTokens);
    }

//
//
//    @Transactional
//    public GeneralResponse resetPassword(String email, String password) throws UserNotFoundException {
//        var user = repository.findByEmail(email)
//                .orElseThrow(() -> new UserNotFoundException("User not found"));
//        user.setPassword(passwordEncoder.encode(password));
//         repository.save(user);
////        var jwtToken = jwtService.generateToken(user);
////        var refreshToken = jwtService.generateRefreshToken(user);
////        saveUserToken(savedUser, jwtToken);
//
//        return GeneralResponse.builder()
//                .message("Password reset successfully")
//                .build();
//    }
//
//    @Transactional
//    public void sendVerificationCode(String email) throws UserNotFoundException, MessagingException {
//        var userEmail = repository.findByEmail(email)
//                .orElseThrow(() -> new UserNotFoundException("User not found"));
//
//        String verificationCode = UUID.randomUUID().toString();
//        Email emailEntity = Email.builder()
//                .email(email)
//                .verificationCode(verificationCode)
//                .verified(false)
//                .build();
//        emailRepository.save(emailEntity);
//        String verificationUrl = "http://localhost:8080/resetPasswordPage?verificationCode=" + verificationCode + "&email=" + email;
//        emailService.sendVerificationEmail(email, "Email Verification", verificationUrl);
//    }
//
//    @Transactional
//    public GeneralResponse verifyCodeAndResetPassword(String email, String verificationCode, String newPassword) throws UserNotFoundException {
//        Email emailEntity = emailRepository.findByEmail(email)
//                .orElseThrow(() -> new UserNotFoundException("email not found"));
//        if (emailEntity.getVerificationCode().equals(verificationCode)) {
//            emailEntity.setVerified(true);
//            emailRepository.save(emailEntity);
//        } else {
//            throw new UserNotFoundException("Invalid verification code ");
//        }
//        return resetPassword(email, newPassword);
//    }
//    public void refreshToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
//        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
//        final String refreshToken;
//        final String userEmail;
//        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
//            return;
//        }
//        refreshToken = authHeader.substring(7);
//        userEmail = jwtService.extractUsername(refreshToken);
//        if (userEmail != null) {
//            var user = this.repository.findByEmail(userEmail)
//                    .orElseThrow();
//            if (jwtService.isTokenValid(refreshToken, user)) {
//                var accessToken = jwtService.generateToken(user);
//                revokeAllUserTokens(user);
//                saveUserToken(user, accessToken);
//                var authResponse = AuthenticationResponse.builder()
//                        .accessToken(accessToken)
//                        .refreshToken(refreshToken)
//                        .build();
//                new ObjectMapper().writeValue(response.getOutputStream(), authResponse);
//            }
//        }
//    }
//
//    public String extractToken(HttpServletRequest request) {
//        String authHeader = request.getHeader("Authorization");
//        if (authHeader != null && authHeader.startsWith("Bearer ")) {
//            return authHeader.substring(7);
//        }
//        return null;
//    }
//
//    public User extractUserFromToken(String token) {
//        String username = jwtService.extractUsername(token);
//        return repository.findByEmail(username).orElse(null);
//    }
//
//    @Transactional
//    public AuthenticationResponse ChangePassword(String email, String oldPassword, String newPassword) throws UserNotFoundException {
//        var user = repository.findByEmail(email)
//                .orElseThrow(() -> new UserNotFoundException("User not found"));
//        if (passwordEncoder.matches(oldPassword, user.getPassword())) {
//            user.setPassword(passwordEncoder.encode(newPassword));
//            var savedUser = repository.save(user);
//            var jwtToken = jwtService.generateToken(user);
//            var refreshToken = jwtService.generateRefreshToken(user);
//            saveUserToken(savedUser, jwtToken);
//            return AuthenticationResponse.builder()
//                    .accessToken(jwtToken)
//                    .refreshToken(refreshToken)
//                    .message("Password changed successfully")
//                    .build();
//        } else {
//            throw new UserNotFoundException("Invalid old password");
//        }
//    }
//
//
//    @Transactional
//    public boolean expiredToken(Long id, String token)  {
//        boolean userToken = tokenRepository.findValidTokenByUserAndToken(id, token).isPresent();
//
//        if(userToken){
//            return false;
//        }
//        return true;
//    }

}
