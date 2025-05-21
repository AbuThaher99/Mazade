package com.Mazade.project.WebApi.Controllers.User;

import com.Mazade.project.Common.DTOs.PaginationDTO;
import com.Mazade.project.Common.Entities.Interested;
import com.Mazade.project.Common.Entities.Post;
import com.Mazade.project.Common.Enums.Category;
import com.Mazade.project.Core.Servecies.InterestedService;
import com.Mazade.project.WebApi.Exceptions.UserNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/interested")
@RequiredArgsConstructor
public class InterestedController {
    private final InterestedService interestedService;

    @Operation(summary = "Add a post to user's interested list")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Post added to interests successfully"),
            @ApiResponse(responseCode = "400", description = "User is already interested in this post",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"status\":400,\"message\":\"User is already interested in this post\"}"))),
            @ApiResponse(responseCode = "404", description = "User or post not found",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"status\":404,\"message\":\"User not found with id: 1\"}"))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/{userId}/{postId}")
    public ResponseEntity<?> addInterested(@PathVariable Long userId, @PathVariable Long postId) {
        try {
            Interested interested = interestedService.addInterested(userId, postId);
            return ResponseEntity.ok(interested);
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

    @Operation(summary = "Remove a post from user's interested list")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Interest removed successfully"),
            @ApiResponse(responseCode = "404", description = "User or post not found",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"status\":404,\"message\":\"Post not found with id: 1\"}"))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/{userId}/{postId}")
    public ResponseEntity<?> removeInterested(@PathVariable Long userId, @PathVariable Long postId) {
        try {
            interestedService.removeInterested(userId, postId);
            return ResponseEntity.ok(Map.of("status", 200, "message", "Interest removed successfully"));
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", 404, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", 500, "message", "Internal server error"));
        }
    }

    @Operation(summary = "Get all posts a user is interested in with pagination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Interested posts retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"status\":404,\"message\":\"User not found with id: 1\"}"))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{userId}")
    public ResponseEntity<?> getInterestedPosts(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Category category) {
        try {
            PaginationDTO<Post> paginatedPosts = interestedService.getInterestedPostsByUserId(userId,category, page, size);
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