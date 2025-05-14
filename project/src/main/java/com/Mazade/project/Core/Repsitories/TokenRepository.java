package com.Mazade.project.Core.Repsitories;

import com.Mazade.project.Common.Entities.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Integer> {

    @Query(" SELECT t FROM Token t INNER JOIN t.user u WHERE u.id = :id AND (t.expired = false OR t.revoked = false) ")
    List<Token> findAllValidTokenByUser(Long id);
    Optional<Token> findByToken(String token);
    @Query(" SELECT t FROM Token t INNER JOIN t.user u WHERE u.id = :id AND t.token = :token AND t.expired = false")
    Optional<Token> findValidTokenByUserAndToken(Long id, String token);

    @Query("SELECT t FROM Token t WHERE t.expired = true")
    List<Token> findAllByExpiredTrue();
    @Modifying
    @Transactional
    @Query("delete from Token t where t.id = :id")
    void deleteById(@Param("id") Long id);
}
