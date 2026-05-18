package com.data.personalfinanceinsightai.repository;

import com.data.personalfinanceinsightai.entity.GroupInvitation;
import com.data.personalfinanceinsightai.entity.enums.InvitationStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupInvitationRepository extends JpaRepository<GroupInvitation, Long> {

    Optional<GroupInvitation> findByToken(String token);

    Optional<GroupInvitation> findByGroupIdAndEmailAndStatus(
            Long groupId, String email, InvitationStatus status);

    List<GroupInvitation> findByGroupIdAndStatus(Long groupId, InvitationStatus status);

    List<GroupInvitation> findByEmailAndStatus(String email, InvitationStatus status);
}
