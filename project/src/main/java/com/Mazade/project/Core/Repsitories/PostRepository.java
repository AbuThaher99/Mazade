package com.Mazade.project.Core.Repsitories;

import com.Mazade.project.Common.Entities.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("SELECT p FROM Post p WHERE " +
            "(:search IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR p.category = :category) " +
            "ORDER BY p.createdDate DESC")
    Page<Post> SortByDate(Pageable pageable, @Param("search") String search, @Param("category") String category);

    @Query("SELECT p FROM Post p WHERE " +
            "(:search IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR p.category = :category) " +
            "ORDER BY p.startPrice ASC")
    Page<Post> SortByPrice(Pageable pageable, @Param("search") String search, @Param("category") String category);

    @Query("SELECT p FROM Post p JOIN p.user u WHERE " +
            "(:search IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR p.category = :category) " +
            "ORDER BY u.rating DESC")
    Page<Post> SortByRating(Pageable pageable, @Param("search") String search, @Param("category") String category);

    @Query("SELECT p FROM Post p WHERE " +
            "(:search IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR p.category = :category)")
    Page<Post> findAllPosts(Pageable pageable, @Param("search") String search, @Param("category") String category);

}
