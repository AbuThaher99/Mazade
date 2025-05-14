package com.Mazade.project.Common.Entities;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "auction")
public class Auction extends BaseEntity {

    @Column(name = "winnerId")
    private Long winnerId;

    @OneToOne
    @JoinColumn(name = "postId")
    @JsonBackReference("postAuction")
    private Post post;

    @Column(name = "finalPrice")
    private double finalPrice;

}
