package com.Mazade.project.Common.Entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
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
@Table(name = "autoBid")
public class AutoBid extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "postId", nullable = false)
    @NotNull(message = "Post is required")
    @JsonBackReference("postBid")
    private Post post;

    @ManyToOne
    @JoinColumn(name = "userId", nullable = false)
    @NotNull(message = "User is required")
    @JsonBackReference("userBid")
    private User user;

    @Column(name = "limitPrice", nullable = false)
    @NotNull(message = "Limit price cannot be blank")
    private double limitPrice;

    @Column(name = "myBid", nullable = false)
    @NotNull(message = "Bid cannot be blank")
    private double myBid;

    @Column(name = "isActive")
    @JsonIgnore
    @Builder.Default
    private boolean isActive = true;



}
