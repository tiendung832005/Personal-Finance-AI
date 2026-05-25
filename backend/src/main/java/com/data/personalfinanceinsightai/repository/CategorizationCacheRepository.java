package com.data.personalfinanceinsightai.repository;

import com.data.personalfinanceinsightai.entity.CategorizationCache;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategorizationCacheRepository extends JpaRepository<CategorizationCache, Long> {
    
    Optional<CategorizationCache> findByDescriptionHash(String descriptionHash);
}
