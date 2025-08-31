package com.legalpro.accountservice.entity;

import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "account_roles")
public class AccountRole {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    // getters and setters
}
