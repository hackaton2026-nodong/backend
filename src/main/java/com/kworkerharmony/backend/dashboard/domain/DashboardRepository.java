package com.kworkerharmony.backend.dashboard.domain;

import com.kworkerharmony.backend.dashboard.entity.Dashboard;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DashboardRepository extends JpaRepository<Dashboard, String> {
}
