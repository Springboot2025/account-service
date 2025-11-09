package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.entity.Appointment;
import com.legalpro.accountservice.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    Optional<Appointment> findByUuid(UUID uuid);

    List<Appointment> findByClientUuid(UUID clientUuid);

    List<Appointment> findByLawyerUuid(UUID lawyerUuid);

    List<Appointment> findByClientUuidAndStatus(UUID clientUuid, AppointmentStatus status);

    List<Appointment> findByLawyerUuidAndStatus(UUID lawyerUuid, AppointmentStatus status);

    List<Appointment> findByLawyerUuidAndClientUuid(UUID lawyerUuid, UUID clientUuid);

    List<Appointment> findByClientUuidAndLawyerUuid(UUID clientUuid, UUID lawyerUuid);

    @Query(value = """
        SELECT DISTINCT ON (a.client_uuid) a.*
        FROM appointments a
        WHERE a.lawyer_uuid = :lawyerUuid
        ORDER BY a.client_uuid, a.created_at DESC
    """, nativeQuery = true)
    List<Appointment> findLatestAppointmentsForLawyer(UUID lawyerUuid);

}
