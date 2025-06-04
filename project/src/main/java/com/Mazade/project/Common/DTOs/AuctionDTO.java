package com.Mazade.project.Common.DTOs;

import com.Mazade.project.Common.Enums.AuctionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuctionDTO {
    private Long id;
    private String category;
    private AuctionStatus status;
    private int postCount;
    private String createdDate;
}
