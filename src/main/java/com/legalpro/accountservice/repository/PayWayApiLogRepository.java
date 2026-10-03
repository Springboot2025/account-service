package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.entity.PayWayApiLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayWayApiLogRepository extends JpaRepository<PayWayApiLog, Long> {
}
