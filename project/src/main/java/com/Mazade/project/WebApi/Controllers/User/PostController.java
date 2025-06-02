package com.Mazade.project.WebApi.Controllers.User;

import com.Mazade.project.Common.DTOs.PaginationDTO;
import com.Mazade.project.Common.Entities.AuctionBidTracker;
import com.Mazade.project.Common.Entities.Post;
import com.Mazade.project.Common.Entities.User;
import com.Mazade.project.Common.Enums.AuctionStatus;
import com.Mazade.project.Common.Enums.Category;
import com.Mazade.project.Common.Enums.Status;
import com.Mazade.project.Core.Servecies.AuctionBidTrackerService;
import com.Mazade.project.Core.Servecies.AuthenticationService;
import com.Mazade.project.Core.Servecies.PostService;
import com.Mazade.project.Core.Servecies.WebSocketService;
import com.Mazade.project.WebApi.Exceptions.UserNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;

    private final AuthenticationService authenticationService;

    private final AuctionBidTrackerService auctionBidTrackerService;

    private final WebSocketService webSocketService;


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
    @PostMapping("/")
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

    @Operation(summary = "Update a post's status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Post status updated successfully",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"id\":1,\"status\":\"COMPLETED\"}"))),
            @ApiResponse(responseCode = "404", description = "Post not found",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"status\":404,\"message\":\"Post not found\"}"))),
            @ApiResponse(responseCode = "400", description = "Invalid status value",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"status\":400,\"message\":\"Invalid status value\"}"))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/status/{postId}")
    public ResponseEntity<?> updatePostStatus(
            @PathVariable Long postId,
            @RequestParam Status status) {
        try {
            Post updatedPost = postService.updatePostStatus(postId, status);
            return ResponseEntity.ok(updatedPost);
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", 404, "message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", 400, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", 500, "message", "Internal server error"));
        }
    }


    @Operation(summary = "Increase post's final price")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Final price increased successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Post.class),
                            examples = @ExampleObject(value = "{\"id\":1,\"title\":\"iPhone 14 Pro Max\",\"finalPrice\":550.0}"))),
            @ApiResponse(responseCode = "404", description = "Post not found",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"status\":404,\"message\":\"Post not found with id: 1\"}"))),
            @ApiResponse(responseCode = "400", description = "Invalid amount value",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"status\":400,\"message\":\"Amount must be greater than zero\"}"))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{postId}/increase-price")
    public ResponseEntity<?> increasePostFinalPrice(
            @PathVariable Long postId,
            @RequestParam double amount,
            HttpServletRequest request) {
        try {
            String token = authenticationService.extractToken(request);
            User user = authenticationService.extractUserFromToken(token);
            Post updatedPost = postService.increasePostFinalPrice(postId, amount);

            if (user != null && updatedPost.getAuction().getStatus() == AuctionStatus.IN_PROGRESS) {
                AuctionBidTracker tracker = auctionBidTrackerService.trackBid(
                        updatedPost.getAuction(), updatedPost, user.getId(), amount);

                // Broadcast updates via WebSocket
                webSocketService.notifyBidUpdate(updatedPost, user.getId(), amount);
                webSocketService.notifyBidTrackerUpdate(postId, tracker);
            }

            return ResponseEntity.ok(updatedPost);
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", 404, "message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", 400, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", 500, "message", "Internal server error"));
        }
    }

    @Operation(summary = "Get all posts by user ID with pagination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Posts retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PaginationDTO.class),
                            examples = @ExampleObject(value = "{\"totalElements\":10,\"totalPages\":1,\"size\":10,\"number\":1,\"numberOfElements\":10,\"content\":[...]}"))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"status\":404,\"message\":\"User not found with id: 1\"}"))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getPostsByUserId(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Category category) {
        try {
            PaginationDTO<Post> paginatedPosts = postService.getPostsByUserId(userId, page, size, category);
            return ResponseEntity.ok(paginatedPosts);
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", 404, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", 500, "message", "Internal server error"));
        }
    }

    @Operation(summary = "Get all posts won by a user with pagination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Posts retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PaginationDTO.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"status\":404,\"message\":\"User not found with id: 1\"}"))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/userWon/{userId}")
    public ResponseEntity<?> getPostsWonByUserId(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Category category) {
        try {
            PaginationDTO<Post> paginatedPosts = postService.getWonPostsByUserId(userId, page, size, category);
            return ResponseEntity.ok(paginatedPosts);
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", 404, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", 500, "message", "Internal server error"));
        }
    }

    @Operation(summary = "Get waiting posts for active auction with pagination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Posts retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PaginationDTO.class))),
            @ApiResponse(responseCode = "404", description = "Auction not found",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"status\":404,\"message\":\"Auction not found\"}"))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/auction-waiting/{auctionId}/")
    public ResponseEntity<?> getWaitingPostsForActiveAuction(
            @PathVariable Long auctionId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Category category) {
        try {
            PaginationDTO<Post> paginatedPosts = postService.getWaitingPostsForActiveAuction(auctionId, page, size, category);
            return ResponseEntity.ok(paginatedPosts);
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", 404, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", 500, "message", "Internal server error"));
        }
    }
}
