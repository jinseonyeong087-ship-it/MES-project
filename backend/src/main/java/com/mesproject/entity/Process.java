package com.mesproject.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "process")
public class Process {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "process_code", nullable = false, unique = true)
    private String processCode;

    @Column(name = "process_name", nullable = false)
    private String processName;

    public Long getId() { return id; }
    public String getProcessCode() { return processCode; }
    public String getProcessName() { return processName; }
}
