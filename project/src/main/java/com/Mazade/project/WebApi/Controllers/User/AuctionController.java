package com.Mazade.project.WebApi.Controllers.User;

import com.Mazade.project.Common.Entities.Auction;
import com.Mazade.project.Common.Enums.AuctionStatus;
import com.Mazade.project.Core.Servecies.AuctionService;
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

import java.util.Map;

@RestController
@RequestMapping("/auction")
@RequiredArgsConstructor
public class AuctionController {
    private final AuctionService auctionService;

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

}
