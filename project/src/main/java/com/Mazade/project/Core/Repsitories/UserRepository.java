package com.Mazade.project.Core.Repsitories;


import com.Mazade.project.Common.Entities.User;
import com.Mazade.project.Common.Enums.Role;
import com.Mazade.project.Common.Enums.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.status = :status")
    Optional<User> findByEmail(@Param("email") String email, @Param("status") Status status);

//    @Query("SELECT u FROM User u WHERE u.role = :role AND  u.status =  'ACTIVE'")
//    Page<User> findAllByRole(@Param("role") Role role, Pageable pageable);
//
//    @Query("SELECT u FROM User u WHERE  u.status =  'ACTIVE' ")
//    Page<User> findAll(Pageable pageable );
//    @Query("SELECT u FROM User u WHERE  u.status =  'ACTIVE' AND" +
//            " (:search IS NULL OR :search = '' OR u.firstName LIKE %:search% or u.lastName LIKE %:search% or u.email LIKE %:search% or u.phone LIKE %:search% or u.city LIKE %:search% ) and " +
//            "(:role IS NULL OR u.role = :role)")
//    Page<User> findAll(Pageable pageable , @Param("search") String search , @Param("role") Role role);
//    @Query("SELECT u FROM User u WHERE u.status =  'BLOCKED' AND" +
//            " (:search IS NULL OR :search = '' OR u.firstName LIKE %:search% or u.lastName LIKE %:search% or u.email LIKE %:search% or u.phone LIKE %:search% or u.city LIKE %:search% ) and " +
//            "(:role IS NULL OR u.role = :role)")
//    Page<User> findAllDeleted(Pageable pageable , @Param("search") String search , @Param("role") Role role);
//    @Query("SELECT u FROM User u WHERE u.status =  'BLOCKED' AND u.id = :id")
//    Optional<User> findDeletedById(@Param("id") Long id);


}
