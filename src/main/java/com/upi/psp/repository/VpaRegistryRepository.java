package com.upi.psp.repository;

import com.upi.psp.entity.VpaRegistry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VpaRegistryRepository extends JpaRepository<VpaRegistry, String> {
}
