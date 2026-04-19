package com.kworkerharmony.backend.enterprise;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyInviteCodeRepository extends JpaRepository<CompanyInviteCode, Long> {

    Optional<CompanyInviteCode> findByCode(String code);
}
