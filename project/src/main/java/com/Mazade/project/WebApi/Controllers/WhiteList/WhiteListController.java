package com.Mazade.project.WebApi.Controllers.WhiteList;

import com.Mazade.project.Common.Entities.User;
import com.Mazade.project.Common.Responses.AuthenticationResponse;
import com.Mazade.project.Core.Servecies.AuthenticationService;
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

import javax.validation.Valid;
import java.io.IOException;

@RestController
@RequestMapping("/whitelist")
@RequiredArgsConstructor
public class WhiteListController {

    private final AuthenticationService authenticationService;

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
}