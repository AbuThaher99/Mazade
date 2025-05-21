package com.Mazade.project.WebApi.Controllers.Admin;

import com.Mazade.project.Common.DTOs.PaginationDTO;
import com.Mazade.project.Common.Entities.Post;
import com.Mazade.project.Common.Entities.User;
import com.Mazade.project.Common.Enums.Role;
import com.Mazade.project.Common.Enums.Status;
import com.Mazade.project.Common.Responses.AuthenticationResponse;
import com.Mazade.project.Common.Responses.GeneralResponse;
import com.Mazade.project.Core.Servecies.AuthenticationService;
import com.Mazade.project.Core.Servecies.PostService;
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
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AuthenticationService authenticationService;
    private final PostService postService;

    @Operation(summary = "Get all users with pagination and filtering")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Role role) {
        try {
            PaginationDTO<User> users = authenticationService.GetAllUsers(page, size, search, role);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", 500, "message", "Internal server error"));
        }
    }

    @Operation(summary = "Block a user (mark as BLOCKED)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User blocked successfully",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"message\":\"User blocked successfully\"}"))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"status\":404,\"message\":\"User not found\"}"))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/blockUser/{userId}/")
    public ResponseEntity<?> blockUser(@PathVariable Long userId) {
        try {
            User user = authenticationService.getUserById(userId);
            user.setStatus(Status.BLOCKED);
            authenticationService.saveUser(user);
            return ResponseEntity.ok(Map.of("message", "User blocked successfully"));
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", 404, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", 500, "message", "Internal server error"));
        }
    }

    @Operation(summary = "Approve a post (change status to ACTIVE)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Post approved successfully"),
            @ApiResponse(responseCode = "404", description = "Post not found",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"status\":404,\"message\":\"Post not found\"}"))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/approvePosts/{postId}")
    public ResponseEntity<?> approvePost(@PathVariable Long postId) {
        try {
            Post updatedPost = postService.accepetPost(postId);
            return ResponseEntity.ok(updatedPost);
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", 404, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", 500, "message", "Internal server error"));
        }
    }

    @Operation(summary = "Get all deleted/blocked users with pagination and filtering")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/deletedUsers")
    public ResponseEntity<?> getAllDeletedUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Role role) {
        try {
            PaginationDTO<User> users = authenticationService.getAllDeletedUsers(page, size, search, role);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", 500, "message", "Internal server error"));
        }
    }

    @Operation(summary = "Restore a deleted/blocked user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User restored successfully",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"message\":\"User restored successfully\"}"))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"status\":404,\"message\":\"User not found\"}"))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/usersRestore/{userId}")
    public ResponseEntity<?> restoreUser(@PathVariable Long userId) {
        try {
            GeneralResponse response = authenticationService.restoreUser(userId);
            return ResponseEntity.ok(response);
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", 404, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", 500, "message", "Internal server error"));
        }
    }

    @Operation(summary = "Get all posts waiting for admin approval")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Posts retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PaginationDTO.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"status\":500,\"message\":\"Internal server error\"}")))
    })
    @GetMapping("/posts/pending-approval")
    public ResponseEntity<?> getPostsPendingApproval(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            PaginationDTO<Post> pendingPosts = postService.getPostsWaitingForApproval(page, size);
            return ResponseEntity.ok(pendingPosts);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", 500, "message", "Internal server error"));
        }
    }

    @Operation(summary = "Register a new Admin user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Admin user registered successfully",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"accessToken\":\"token\",\"refreshToken\":\"refreshToken\",\"message\":\"User ADMIN added successfully\"}"))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"status\":400,\"message\":\"Validation error\",\"errors\":[\"Password cannot be blank\",\"Email cannot be blank\"]}"))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not authorized to create admin"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(type = "object"),
                    examples = @ExampleObject(
                            name = "Admin Registration",
                            summary = "Example of registering a new admin user",
                            value = "{\n" +
                                    "  \"password\": \"Admin123\",\n" +
                                    "  \"firstName\": \"Admin\",\n" +
                                    "  \"lastName\": \"User\",\n" +
                                    "  \"city\": \"Jerusalem\",\n" +
                                    "  \"phone\": \"0569482500\",\n" +
                                    "  \"email\": \"admin@mazade.com\",\n" +
                                    "  \"gender\": \"MALE\"\n" +
                                    "}"
                    )
            )
    )
    @PostMapping("/addAdmin")
    public ResponseEntity<AuthenticationResponse> addAdmin(@RequestBody @Valid User user) {
        try {
            AuthenticationResponse response = authenticationService.addAdmin(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}