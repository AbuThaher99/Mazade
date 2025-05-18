package com.Mazade.project.Core.Repsitories;

import com.Mazade.project.Common.Entities.Interested;
import com.Mazade.project.Common.Entities.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterestedRepository extends JpaRepository<Interested, Long> {
    boolean existsByUserIdAndPostId(Long userId, Long postId);

    void deleteByUserIdAndPostId(Long userId, Long postId);

    @Query("SELECT i.post FROM Interested i WHERE i.user.id = :userId")
    List<Post> findPostsByUserId(@Param("userId") Long userId);

    @Query("SELECT i.post FROM Interested i WHERE i.user.id = :userId")
    Page<Post> findPostsByUserIdPaginated(@Param("userId") Long userId, Pageable pageable);
}