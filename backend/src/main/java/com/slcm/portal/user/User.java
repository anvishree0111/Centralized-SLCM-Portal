package com.slcm.portal.user;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, unique=true, length=80) private String username;
    @Column(name="password_hash", nullable=false, length=100) private String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable=false, columnDefinition="enum('STUDENT','ADMIN')") private UserRole role;
    @Column(nullable=false) private boolean enabled;
    @Column(name="created_at", nullable=false, insertable=false, updatable=false) private Instant createdAt;
    @Column(name="updated_at", nullable=false, insertable=false, updatable=false) private Instant updatedAt;
    protected User() {}
    public Long getId(){return id;} public String getUsername(){return username;} public String getPasswordHash(){return passwordHash;}
    public UserRole getRole(){return role;} public boolean isEnabled(){return enabled;}
}
