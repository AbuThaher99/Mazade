package com.Mazade.project.WebApi.Controllers.User;

import com.Mazade.project.Common.DTOs.PaginationDTO;
import com.Mazade.project.Common.Entities.Auction;
import com.Mazade.project.Common.Entities.AuctionBidTracker;
import com.Mazade.project.Common.Enums.AuctionStatus;
import com.Mazade.project.Common.Enums.Category;
import com.Mazade.project.Core.Servecies.AuctionBidTrackerService;
import com.Mazade.project.Core.Servecies.AuctionService;
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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auction")
@RequiredArgsConstructor
public class AuctionController {
    private final AuctionService auctionService;
    private final AuctionBidTrackerService auctionBidTrackerService;

    @Operation(summary = "Update an auction's status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Auction status updated successfully",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"id\":1,\"status\":\"IN_PROGRESS\"}"))),
            @ApiResponse(responseCode = "404", description = "Auction not found",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"status\":404,\"message\":\"Auction not found\"}"))),
            @ApiResponse(responseCode = "400", description = "Invalid status value",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"status\":400,\"message\":\"Invalid status value\"}"))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/status/{auctionId}")
    public ResponseEntity<?> updateAuctionStatus(
            @PathVariable Long auctionId,
            @RequestParam AuctionStatus status) {
        try {
            Auction updatedAuction = auctionService.updateAuctionStatus(auctionId, status);
            return ResponseEntity.ok(updatedAuction);
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

    @Operation(summary = "Get all auctions with pagination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Auctions retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PaginationDTO.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/")
    public ResponseEntity<PaginationDTO<Auction>> getAllAuctions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) AuctionStatus status) {
        try {
            PaginationDTO<Auction> auctions = auctionService.getAllAuctions(page, size, status);
            return ResponseEntity.ok(auctions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }
    @Operation(summary = "Get auctions by status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Auctions retrieved successfully",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/status")
    public ResponseEntity<List<Auction>> getAuctionsByStatus(
            @RequestParam AuctionStatus status) {
        try {
            List<Auction> auctions = auctionService.getAuctionsByStatus(status);
            return ResponseEntity.ok(auctions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @Operation(summary = "Get auction by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Auction retrieved successfully",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Auction not found",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"status\":404,\"message\":\"Auction not found\"}"))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{auctionId}")
    public ResponseEntity<?> getAuctionById(@PathVariable Long auctionId) {
        try {
            Auction auction = auctionService.getAuctionById(auctionId);
            return ResponseEntity.ok(auction);
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", 404, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", 500, "message", "Internal server error"));
        }
    }

    @Operation(summary = "Get auction by category and status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Auction retrieved successfully",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Auction not found",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"status\":404,\"message\":\"Auction not found\"}"))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/category-status")
    public ResponseEntity<?> getAuctionByCategoryAndStatus(
            @RequestParam Category category,
            @RequestParam AuctionStatus status) {
        try {
            Auction auction = auctionService.getAuctionByCategoryAndStatus(category, status);
            if (auction == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("status", 404, "message", "No auction found with the specified category and status"));
            }
            return ResponseEntity.ok(auction);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", 500, "message", "Internal server error"));
        }
    }


    @Operation(summary = "Get all bid trackers for a post with pagination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bid trackers retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PaginationDTO.class))),
            @ApiResponse(responseCode = "404", description = "Post not found",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"status\":404,\"message\":\"Post not found\"}"))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/trackerPost/{postId}")
    public ResponseEntity<?> getBidTrackersByPostId(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            PaginationDTO<AuctionBidTracker> bidTrackers = auctionBidTrackerService.getBidTrackersByPostId(postId, page, size);
            return ResponseEntity.ok(bidTrackers);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", 500, "message", "Internal server error"));
        }
    }
}
