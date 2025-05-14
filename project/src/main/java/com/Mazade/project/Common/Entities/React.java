package com.Mazade.project.Common.Entities;

import com.Mazade.project.Common.Enums.ReactType;
import com.fasterxml.jackson.annotation.JsonBackReference;
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
@Table(name = "react")
public class React extends BaseEntity {


    @ManyToOne
    @JoinColumn(name = "postId", nullable = false)
    @NotNull(message = "Post is required")
    @JsonBackReference("postReacts")
    private Post post;

    @ManyToOne
    @JoinColumn(name = "userId", nullable = false)
    @NotNull(message = "User is required")
    @JsonBackReference("userReacts")
    private User user;

    @Column(name = "reactType", nullable = false)
    @NotNull(message = "React cannot be blank")
    private ReactType reactType;

}
