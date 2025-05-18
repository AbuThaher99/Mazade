package com.Mazade.project.Common.DTOs;

import com.Mazade.project.Common.Enums.Gender;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserUpdateDTO {
    @NotNull(message = "First name cannot be blank")
    private String firstName;
    @NotNull(message = "Last name cannot be blank")
    private String lastName;
    @NotNull(message = "Phone cannot be blank")
    private String phone;
    @NotNull(message = "City cannot be blank")
    private String city;
    @NotNull(message = "Gender cannot be blank")
    private Gender gender;
}
