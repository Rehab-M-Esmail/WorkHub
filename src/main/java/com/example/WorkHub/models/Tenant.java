package com.example.WorkHub.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
public class Tenant {
    @Id
    @GeneratedValue
    @Getter
    private Long id;
    @Setter
    @Getter
    @Column(unique = true)
    private String name;
    @Setter
    @Getter
    private String plan; // should be enum instead?
}
