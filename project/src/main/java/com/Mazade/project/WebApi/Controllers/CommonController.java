package com.Mazade.project.WebApi.Controllers;

import com.Mazade.project.Common.DTOs.UserUpdateDTO;
import com.Mazade.project.Common.Responses.AuthenticationResponse;
import com.Mazade.project.Common.Responses.GeneralResponse;
import com.Mazade.project.Core.Servecies.AuthenticationService;
import com.Mazade.project.WebApi.Exceptions.UserNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/common")
@RequiredArgsConstructor
public class CommonController {

    private final AuthenticationService authenticationService;
    @PostMapping("/changePassword")
    public ResponseEntity<AuthenticationResponse> changePassword(@RequestParam String email,
                                                                 @RequestParam String oldPassword,
                                                                 @RequestParam String newPassword) throws UserNotFoundException, UserNotFoundException {
        AuthenticationResponse response = authenticationService.ChangePassword(email, oldPassword, newPassword);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update user details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User updated successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = GeneralResponse.class),
                            examples = @ExampleObject(value = "{\"message\":\"User updated successfully\"}"))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"status\":404,\"message\":\"User not found\"}"))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"status\":400,\"message\":\"Validation failed\",\"errors\":[\"First name cannot be blank\"]}"))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UserUpdateDTO.class),
                    examples = @ExampleObject(
                            name = "User Update",
                            summary = "Example of updating user details",
                            value = "{\n" +
                                    "  \"firstName\": \"Mohammad\",\n" +
                                    "  \"lastName\": \"Mashhour\",\n" +
                                    "  \"phone\": \"0569482508\",\n" +
                                    "  \"city\": \"Ramallah\",\n" +
                                    "  \"gender\": \"MALE\"\n" +
                                    "}"
                    )
            )
    )
    @PatchMapping("/{userId}")
    public ResponseEntity<?> updateUser(
            @PathVariable Long userId,
            @RequestBody UserUpdateDTO userUpdateDTO) {
        try {
            GeneralResponse response = authenticationService.UpdateUser(userUpdateDTO, userId);
            return ResponseEntity.ok(response);
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", 404, "message", e.getMessage()));
        } catch (jakarta.validation.ConstraintViolationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", 400, "message", "Validation failed"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", 500, "message", "Internal server error"));
        }
    }

}
