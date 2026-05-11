package com.data.personalfinanceinsightai.repository;

import com.data.personalfinanceinsightai.entity.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query(
            "SELECT c FROM Category c WHERE c.user IS NULL OR c.user.id = :userId "
                    + "ORDER BY c.builtIn DESC, c.type ASC, c.name ASC"
    )
    List<Category> findAllVisibleForUser(@Param("userId") Long userId);

    @Query("SELECT c FROM Category c WHERE c.id = :id AND (c.user IS NULL OR c.user.id = :userId)")
    Optional<Category> findVisibleByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
