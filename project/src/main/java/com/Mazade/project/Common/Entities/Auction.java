package com.Mazade.project.Common.Entities;
import com.Mazade.project.Common.Enums.Category;
import com.fasterxml.jackson.annotation.JsonBackReference;
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
@Table(name = "auction")
public class Auction extends BaseEntity {

    @Column(name = "winnerId")
    private Long winnerId;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY , orphanRemoval = true)
    @JoinColumn(name = "auctionId", referencedColumnName = "id")
    @JsonManagedReference("postAuction")
    private List<Post> posts;

    @Column(name = "Category", nullable = false)
    @Enumerated(EnumType.STRING)
    private Category category;

    @Column(name = "finalPrice")
    private double finalPrice;

}
