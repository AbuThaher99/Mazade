package com.Mazade.project.Common.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginDTO {
    @NotNull(message = "Email cannot be Null")
    @NotBlank(message = "Email cannot be Blank")
    private String Email;
    @NotNull(message = "Password cannot be Null")
    @NotBlank(message = "Password cannot be Blank")
    String password;
}