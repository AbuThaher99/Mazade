package com.Mazade.project.WebApi.Controllers.WhiteList;

import com.Mazade.project.Common.DTOs.PaginationDTO;
import com.Mazade.project.Common.Entities.Post;
import com.Mazade.project.Common.Entities.User;
import com.Mazade.project.Common.Responses.AuthenticationResponse;
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
            @RequestParam(required = false) String category,
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

    @Operation(summary = "Add a new post with multiple images")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Post created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid post data or no images provided",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"status\":400,\"message\":\"Invalid post data or no images provided\"}"))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                    mediaType = "multipart/form-data",
                    schema = @Schema(type = "object"),
                    examples = @ExampleObject(
                            name = "Post with images",
                            summary = "Example of adding a post with images",
                            value = "{\n" +
                                    "    \"title\": \"iPhone 14 Pro Max\",\n" +
                                    "    \"description\": \"Brand new iPhone with 256GB storage\",\n" +
                                    "    \"startPrice\": 500.0,\n" +
                                    "    \"category\": \"ELECTRONICS\",\n" +
                                    "    \"bidStep\": 50.0,\n" +
                                    "    \"status\": \"WAITING\",\n" +
                                    "    \"user\": {\"id\": 1},\n" +
                                    "    \"auction\": {\"id\": 1},\n" +
                                    "  \"media\": link1,link2\n" +
                                    "}"
                    )
            )
    )
    @PostMapping("/posts")
    public ResponseEntity<Post> addPost(
            @RequestPart("post") Post post,
            @RequestPart("images") List<MultipartFile> images) {

        try {
            Post savedPost = postService.addPost(post, images);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedPost);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}