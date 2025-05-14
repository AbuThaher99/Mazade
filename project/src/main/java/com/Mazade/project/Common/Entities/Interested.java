package com.Mazade.project.Common.Entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Interested")
public class Interested extends BaseEntity{
//    private Long postId;
//    private Long userId;


    @ManyToOne
    @JoinColumn(name = "postId", nullable = false)
    @NotNull(message = "Post is required")
    @JsonBackReference("postInteresteds")
    private Post post;

    @ManyToOne
    @JoinColumn(name = "userId", nullable = false)
    @NotNull(message = "User is required")
    @JsonBackReference("userInteresteds")
    private User user;
}
