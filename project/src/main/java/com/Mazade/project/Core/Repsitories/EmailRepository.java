package com.Mazade.project.Core.Repsitories;

import com.Mazade.project.Common.Entities.Email;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmailRepository extends JpaRepository<Email, Long> {
    @Query("SELECT e FROM Email e WHERE e.email = :email and e.verified = false")
    Optional<Email> findByEmail(@Param("email") String email);


}
