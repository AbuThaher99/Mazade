package com.Mazade.project.WebApi.Controllers.WhiteList;

import com.Mazade.project.Common.DTOs.PaginationDTO;
import com.Mazade.project.Common.Entities.Auction;
import com.Mazade.project.Common.Entities.Post;
import com.Mazade.project.Common.Entities.User;
import com.Mazade.project.Common.Enums.AuctionStatus;
import com.Mazade.project.Common.Enums.Category;
import com.Mazade.project.Common.Enums.Status;
import com.Mazade.project.Common.Responses.AuthenticationResponse;
import com.Mazade.project.Core.Servecies.AuctionService;
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
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/whitelist")
@RequiredArgsConstructor
public class WhiteListController {

    private final AuthenticationService authenticationService;
    private final PostService postService;

    @Operation(summary = "Register a new User")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"password\":\"A12345yuo\",\"firstName\":\"mohammad\",\"lastName\":\"Mashhour\",\"city\":\"Ramallah\",\"phone\":\"0569482508\",\"email\":\"m@gmail.com\",\"gender\":\"MALE\"}"))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"status\":400,\"message\":\"Validation error\",\"errors\":[\"Password cannot be blank\",\"Email cannot be blank\"]}"))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"status\":404,\"message\":\"User not found\"}"))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(type = "object"),
                    examples = @ExampleObject(
                            name = "User Registration",
                            summary = "Example of registering a new user",
                            value = "{\n" +
                                    "  \"password\": \"A12345yuo\",\n" +
                                    "  \"firstName\": \"mohammad\",\n" +
                                    "  \"lastName\": \"Mashhour\",\n" +
                                    "  \"city\": \"Ramallah\",\n" +
                                    "  \"phone\": \"0569482508\",\n" +
                                    "  \"email\": \"m@gmail.com\",\n" +
                                    "  \"gender\": \"MALE\"\n" +
                                    "}"
                    )
            )
    )
    @PostMapping("/Register")
    public ResponseEntity<AuthenticationResponse> register(@RequestBody @Valid User user) {
        try {
            AuthenticationResponse response = authenticationService.adduser(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @Operation(summary = "Get all posts with pagination and filtering")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Posts retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"status\":400,\"message\":\"Invalid parameters\"}"))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/posts")
    public ResponseEntity<PaginationDTO<Post>> getAllPosts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Category category,
            @RequestParam(defaultValue = "false") Boolean sortByDate,
            @RequestParam(defaultValue = "false") Boolean sortByPrice,
            @RequestParam(defaultValue = "false") Boolean sortByRating) {

        try {

            PaginationDTO<Post> posts = postService.getAllPost(page, size, search, category,
                    sortByDate, sortByPrice, sortByRating);
            return ResponseEntity.ok(posts);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @Operation(summary = "Get a post by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Post found successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Post.class),
                            examples = @ExampleObject(value = "{\"id\":1,\"title\":\"iPhone 14 Pro Max\",\"description\":\"Brand new iPhone\",\"status\":\"ACTIVE\"}"))),
            @ApiResponse(responseCode = "404", description = "Post not found",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"status\":404,\"message\":\"Post not found with id: 1\"}"))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{postId}")
    public ResponseEntity<?> getPostById(@PathVariable Long postId) {
        try {
            Post post = postService.getPostById(postId);
            return ResponseEntity.ok(post);
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", 404, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", 500, "message", "Internal server error"));
        }
    }

}