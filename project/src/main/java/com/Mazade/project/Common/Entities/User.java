package com.Mazade.project.Common.Entities;

import com.Mazade.project.Common.Enums.Gender;
import com.Mazade.project.Common.Enums.Role;
import com.Mazade.project.Common.Enums.Status;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User extends BaseEntity implements UserDetails {

    @Column(name = "password", nullable = false)
    @NotNull(message = "Password cannot be blank")
    private String password;

    @Column(name = "firstName", nullable = false)
    @NotNull(message = "First name cannot be blank")
    private String firstName;

    @Column(name = "lastName", nullable = false)
    @NotNull(message = "Last name cannot be blank")
    private String lastName;

    @Column(name = "city" , nullable = false)
    @NotNull(message = "Address cannot be blank")
    private String city;

    @Column(name = "phone" , nullable = false)
    @NotNull(message = "Phone cannot be blank")
    private String phone;

    @Column(name = "email", unique = true, nullable = false)
    @NotNull(message = "Email cannot be blank")
    private String email;

    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    @OneToMany(mappedBy = "user")
    @JsonIgnore
    private List<Token> tokens;

    @Column(name = "gender" , nullable = false)
    @Enumerated(EnumType.STRING)
    @NotNull(message = "gender cannot be blank")
    private Gender gender;

    @Column(name = "ratign")
    private double rating;

    @Column(name = "status", columnDefinition = "VARCHAR(10) CHECK (status IN ('ACTIVE', 'BLOCKED'))")
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "paymentToken")
    private String paymentToken;

    @Column(name = "lahzaCustomerId")
    private String lahzaCustomerId;

    @Column(name = "last4")
    private int last4;

    @Column(name = "brand")
    private String brand;
    @Column(name = "lastLogin")
    private Date lastLogin;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY , orphanRemoval = true)
    @JoinColumn(name = "userId", referencedColumnName = "id")
    @JsonManagedReference("userPosts")
    private List<Post> posts;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY , orphanRemoval = true)
    @JoinColumn(name = "userId", referencedColumnName = "id")
    @JsonManagedReference("userBid")
    private List<AutoBid> autoBids;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY , orphanRemoval = true)
    @JoinColumn(name = "userId", referencedColumnName = "id")
    @JsonManagedReference("userInteresteds")
    private List<Interested> interesteds;


    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return role.getAuthorities();
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    @JsonIgnore
    public String getUsername() {
        return email; // Use email as the username
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isEnabled() {
        return true;
    }
}
