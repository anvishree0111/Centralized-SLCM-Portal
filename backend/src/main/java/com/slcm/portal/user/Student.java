package com.slcm.portal.user;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="students")
public class Student {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="user_id", nullable=false, unique=true) private User user;
    @Column(name="reg_no", nullable=false, length=30, unique=true) private String registrationNumber;
    @Column(name="full_name", nullable=false, length=120) private String fullName;
    @Column(nullable=false, length=160, unique=true) private String email;
    @Column(nullable=false, length=120) private String program;
    @Column(nullable=false, length=30) private String section;
    @Column(name="focus_mode_enabled", nullable=false) private boolean focusModeEnabled;
    @Column(name="created_at", nullable=false, insertable=false, updatable=false) private Instant createdAt;
    protected Student() {}
    public Long getId(){return id;} public String getRegistrationNumber(){return registrationNumber;} public String getFullName(){return fullName;}
    public String getEmail(){return email;} public String getProgram(){return program;} public String getSection(){return section;}
}
