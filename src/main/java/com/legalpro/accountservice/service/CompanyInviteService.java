package com.legalpro.accountservice.service;

import com.legalpro.accountservice.dto.CompanyInviteRequestDto;
import com.legalpro.accountservice.entity.Company;
import com.legalpro.accountservice.entity.CompanyInvite;
import com.legalpro.accountservice.repository.CompanyInviteRepository;
import com.legalpro.accountservice.repository.CompanyRepository;
import com.legalpro.accountservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyInviteService {

    private final CompanyInviteRepository inviteRepo;
    private final CompanyRepository companyRepo;
    private final EmailService emailService;

    public CompanyInvite createInvite(CompanyInviteRequestDto request) {

        // Validate company exists
        Company company = companyRepo.findByUuid(request.getCompanyUuid())
                .orElseThrow(() -> new RuntimeException("Invalid company UUID"));

        // Create invite
        CompanyInvite invite = CompanyInvite.builder()
                .email(request.getEmail())
                .companyUuid(company.getUuid())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        inviteRepo.save(invite);

        sendInviteEmail(invite, company);

        return invite;
    }

    public CompanyInvite validateToken(String token) {
        CompanyInvite invite = inviteRepo.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid invite token"));

        if (invite.isUsed()) {
            throw new RuntimeException("Invite already used");
        }

        if (invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Invite expired");
        }

        return invite;
    }

    private void sendInviteEmail(CompanyInvite invite, Company company) {
        String url = "https://lawproject-nu.vercel.app/register/company-invite?token=" + invite.getToken();

        String subject = "Invitation to join " + company.getName();

        String bodyHtml =
                "<p>Hello,</p>" +
                        "<p>You have been invited to join <strong>" + company.getName() + "</strong> as a company member.</p>" +
                        "<p>Click the link below to accept the invitation:</p>" +
                        "<a href=\"" + url + "\">Accept Invitation</a>" +
                        "<p>This invitation will expire in <strong>7 days</strong>.</p>" +
                        "<p>If you didn't expect this invitation, you may ignore this email.</p>";

        emailService.sendEmail(invite.getEmail(), subject, bodyHtml);
    }
}
