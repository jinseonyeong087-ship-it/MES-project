package com.mesproject.repo;

import com.mesproject.entity.Process;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessRepository extends JpaRepository<Process, Long> {
    java.util.List<Process> findAllByOrderByIdAsc();
}
