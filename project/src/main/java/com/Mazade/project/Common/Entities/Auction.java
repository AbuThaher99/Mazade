package com.Mazade.project.Common.Entities;
import com.Mazade.project.Common.Converters.CategoryMapConverter;
import com.Mazade.project.Common.Enums.Category;
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
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "auction")
public class Auction extends BaseEntity {

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY , orphanRemoval = true)
    @JoinColumn(name = "auctionId", referencedColumnName = "id")
    @JsonManagedReference("postAuction")
    private List<Post> posts;

    @Column(name = "Category", nullable = false, columnDefinition = "TEXT")
    @Convert(converter = CategoryMapConverter.class)
    private Map<Category, Integer> category;

    @Column(name = "isFinished")
    @JsonIgnore
    @Builder.Default
    private boolean isFinished = false;

}
