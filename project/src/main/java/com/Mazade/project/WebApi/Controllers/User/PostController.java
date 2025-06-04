package com.Mazade.project.WebApi.Controllers.User;

import com.Mazade.project.Common.DTOs.PaginationDTO;
import com.Mazade.project.Common.Entities.AuctionBidTracker;
import com.Mazade.project.Common.Entities.Post;
import com.Mazade.project.Common.Entities.User;
import com.Mazade.project.Common.Enums.AuctionStatus;
import com.Mazade.project.Common.Enums.Category;
import com.Mazade.project.Common.Enums.Status;
import com.Mazade.project.Core.Repsitories.PostRepository;
import com.Mazade.project.Core.Servecies.*;
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

import static com.Mazade.project.WebApi.Config.JwtService.log;


@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;

    private final AuthenticationService authenticationService;

    private final AuctionBidTrackerService auctionBidTrackerService;

    private final WebSocketService webSocketService;
    private final AuctionTimerService auctionTimerService;

    private final PostRepository postRepository;

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

            // Verify that this post is currently IN_PROGRESS
            Post currentPost = postService.getPostById(postId);
            if (currentPost.getStatus() != Status.IN_PROGRESS) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("status", 400, "message", "This item is not currently active for bidding"));
            }

            Post updatedPost = postService.increasePostFinalPrice(postId, amount);

            if (user != null && updatedPost.getAuction().getStatus() == AuctionStatus.IN_PROGRESS) {
                AuctionBidTracker tracker = auctionBidTrackerService.trackBid(
                        updatedPost.getAuction(), updatedPost, user.getId(), amount);

                // Broadcast updates via WebSocket
                webSocketService.notifyBidUpdate(updatedPost, user.getId(), amount);
                webSocketService.notifyBidTrackerUpdate(postId, tracker);

                // RESTART timer to 30 seconds for the current active post
                if (auctionTimerService.isTimerActive(postId)) {
                    auctionTimerService.restartPostTimer(postId);
                    log.info("🔄 Timer restarted to 30 seconds for post {} due to bid by user {}",
                            postId, user.getId());
                } else {
                    // If no timer was active, start a new one (shouldn't happen in normal flow)
                    auctionTimerService.startPostTimer(postId);
                    log.info("⏰ Started new timer for post {} due to bid by user {}",
                            postId, user.getId());
                }
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

    /**
     * Get current active post in an auction
     */
    @GetMapping("/auction/{auctionId}/current-active")
    public ResponseEntity<?> getCurrentActivePost(
            @PathVariable Long auctionId,
            HttpServletRequest request) { // Add HttpServletRequest for auth
        try {
             String token = authenticationService.extractToken(request);
             User user = authenticationService.extractUserFromToken(token);
             if (user == null) {
                 return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                         .body(Map.of("status", "error", "message", "Authentication required"));
             }

            Long activePostId = auctionTimerService.getCurrentActivePostInAuction(auctionId);

            if (activePostId != null) {
                Post activePost = postService.getPostById(activePostId);
                long remainingSeconds = auctionTimerService.getRemainingTimeSeconds(activePostId);

                return ResponseEntity.ok(Map.of(
                        "activePost", activePost,
                        "remainingSeconds", remainingSeconds,
                        "isTimerActive", auctionTimerService.isTimerActive(activePostId)
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "activePost", null,
                        "message", "No active post in this auction"
                ));
            }
        } catch (Exception e) {
            log.error("Error getting current active post for auction {}: {}", auctionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Failed to get active post"));
        }
    }

    /**
     * Manual endpoint to start next post in sequence (for testing/admin use)
     */
    @PostMapping("/auction/{auctionId}/start-next")
    public ResponseEntity<?> startNextPostInAuction(@PathVariable Long auctionId) {
        try {
            // This is mainly for testing - in normal flow this happens automatically
            Long activePostId = auctionTimerService.getCurrentActivePostInAuction(auctionId);

            if (activePostId != null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("status", "error", "message", "There is already an active post in this auction"));
            }

            // Find next waiting post and start it
            List<Post> waitingPosts = postRepository.findByAuctionIdAndStatusOrderByIdAsc(auctionId, Status.WAITING);

            if (!waitingPosts.isEmpty()) {
                Post nextPost = waitingPosts.get(0);
                auctionTimerService.startNextPostInSequence(auctionId, nextPost.getId());

                return ResponseEntity.ok(Map.of(
                        "status", "success",
                        "message", "Started next post in sequence",
                        "postId", nextPost.getId(),
                        "title", nextPost.getTitle()
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "status", "info",
                        "message", "No more posts waiting in this auction"
                ));
            }

        } catch (Exception e) {
            log.error("Error starting next post in auction {}: {}", auctionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Failed to start next post"));
        }
    }
    @PostMapping("/{postId}/start-timer")
    public ResponseEntity<?> startPostTimer(@PathVariable Long postId) {
        try {
            auctionTimerService.startPostTimer(postId);
            long remainingSeconds = auctionTimerService.getRemainingTimeSeconds(postId);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Timer started for post " + postId,
                    "remainingSeconds", remainingSeconds
            ));
        } catch (Exception e) {
            log.error("Error starting timer for post ID: {}", postId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Failed to start timer"));
        }
    }
    @GetMapping("/{postId}/timer-status")
    public ResponseEntity<?> getTimerStatus(@PathVariable Long postId) {
        try {
            boolean isActive = auctionTimerService.isTimerActive(postId);
            long remainingSeconds = auctionTimerService.getRemainingTimeSeconds(postId);

            return ResponseEntity.ok(Map.of(
                    "postId", postId,
                    "isActive", isActive,
                    "remainingSeconds", remainingSeconds,
                    "totalActiveTimers", auctionTimerService.getActiveTimerCount()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Failed to get timer status"));
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
