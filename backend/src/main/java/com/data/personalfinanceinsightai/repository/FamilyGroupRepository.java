package com.data.personalfinanceinsightai.repository;

import com.data.personalfinanceinsightai.entity.FamilyGroup;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FamilyGroupRepository extends JpaRepository<FamilyGroup, Long> {

    @Query(
            "SELECT g FROM FamilyGroup g WHERE g.id IN "
                    + "(SELECT gm.groupId FROM GroupMember gm WHERE gm.userId = :userId)")
    List<FamilyGroup> findGroupsByUserId(@Param("userId") Long userId);
}
