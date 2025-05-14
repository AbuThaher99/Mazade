package com.Mazade.project.Common.Enums;

import com.Mazade.project.Common.Entities.*;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "post")
public class Post extends BaseEntity {
    @Column(name = "title", nullable = false)
    @NotNull(message = "Title cannot be blank")
    private String title;

    @Column(name = "description", nullable = false)
    @NotNull(message = "Description cannot be blank")
    private String description;

    @Column(name = "startPrice", nullable = false)
    @NotNull(message = "Price cannot be blank")
    private double startPrice;

    @Column(name = "Category", nullable = false)
    @NotNull(message = "Category cannot be blank")
    private Category category;

    @Column(name = "media", nullable = false, columnDefinition = "TEXT")
    @NotNull(message = "Media cannot be blank")
    private String media;

    @Column(name = "bidStep", nullable = false)
    @NotNull(message = "Bid step cannot be blank")
    private double bidStep;

    @Column(name = "status")
    @JsonIgnore
    @Builder.Default
    private Status status = Status.WAITING;

    @ManyToOne
    @JoinColumn(name = "userId", nullable = false)
    @NotNull(message = "User is required")
    @JsonBackReference("userPosts")
    private User user;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY , orphanRemoval = true)
    @JoinColumn(name = "postId", referencedColumnName = "id")
    @JsonManagedReference("postReacts")
    private List<React> reacts;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY , orphanRemoval = true)
    @JoinColumn(name = "postId", referencedColumnName = "id")
    @JsonManagedReference("postInteresteds")
    private List<Interested> interesteds;

    @OneToOne(mappedBy = "post", cascade = CascadeType.ALL , fetch = FetchType.LAZY)
    @JsonBackReference("postAuction")
    private Auction auction;
}
