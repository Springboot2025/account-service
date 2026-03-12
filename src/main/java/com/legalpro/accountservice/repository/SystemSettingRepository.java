package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.entity.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {

    Optional<SystemSetting> findFirstByRemovedAtIsNull();

}