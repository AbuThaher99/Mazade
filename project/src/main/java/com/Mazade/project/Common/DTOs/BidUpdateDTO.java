package com.Mazade.project.Common.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BidUpdateDTO {
    private Long postId;
    private double newPrice;
    private String userIdentifier;
    private String timestamp;
}