package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.*;
import com.legalpro.accountservice.dto.admin.*;
import com.legalpro.accountservice.enums.AdminLawyerStatus;
import com.legalpro.accountservice.enums.AdminSortBy;
import com.legalpro.accountservice.security.CustomUserDetails;
import com.legalpro.accountservice.service.ContactRequestService;
import com.legalpro.accountservice.service.DisputeService;
import com.legalpro.accountservice.service.SuperAdminService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/superadmin")
@PreAuthorize("hasRole('Admin')")
public class SuperAdminController {

    private final SuperAdminService superAdminService;
    private final ContactRequestService contactRequestService;
    private final DisputeService disputeService;

    public SuperAdminController(SuperAdminService superAdminService, ContactRequestService contactRequestService,
                                DisputeService disputeService) {
        this.superAdminService = superAdminService;
        this.contactRequestService = contactRequestService;
        this.disputeService = disputeService;
    }
    @GetMapping("/hello")
    public ResponseEntity<ApiResponse<String>> helloSuperAdmin() {
        ApiResponse<String> response = ApiResponse.success(
                HttpStatus.OK.value(),
                "SuperAdmin API working",
                "Hello SuperAdmin!"
        );
        return ResponseEntity.ok(response);
    }

    /*@GetMapping("/users")
    public ResponseEntity<ApiResponse<List<AccountDto>>> getUsersByType(
            @RequestParam(name = "type") String userType) {

        try {
            List<AccountDto> users = superAdminService.getUsersByType(userType);

            return ResponseEntity.ok(
                    ApiResponse.success(HttpStatus.OK.value(), "Fetched users successfully", users)
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ApiResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage())
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Something went wrong")
            );
        }
    }*/

    @GetMapping("/contacts")
    public ResponseEntity<ApiResponse<?>> listContactRequests() {
        try {
            var list = contactRequestService.listAll();

            return ResponseEntity.ok(
                    ApiResponse.success(
                            HttpStatus.OK.value(),
                            "Fetched contact requests successfully",
                            list
                    )
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(
                            HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "Something went wrong while fetching contact requests"
                    )
            );
        }
    }

    @GetMapping("/disputes")
    public ResponseEntity<ApiResponse<?>> getAllDisputes() {

        var list = disputeService.getAllWithDocumentCount();

        return ResponseEntity.ok(
                ApiResponse.success(
                        200,
                        "Fetched disputes successfully",
                        list
                )
        );
    }

    @GetMapping("/dashboard/summary")
    public ResponseEntity<ApiResponse<AdminDashboardSummaryDto>> getDashboardSummary() {

        AdminDashboardSummaryDto summary = superAdminService.getAdminDashboardSummary();

        return ResponseEntity.ok(
                ApiResponse.success(
                        200,
                        "Dashboard summary fetched successfully",
                        summary
                )
        );
    }

    @GetMapping("/lawyers")
    public ResponseEntity<ApiResponse<?>> getLawyers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) AdminLawyerStatus status,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "NEWEST_FIRST") AdminSortBy sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        var result = superAdminService.getLawyers(
                search, status, category, sort, page, size
        );

        return ResponseEntity.ok(
                ApiResponse.success(200, "Lawyers fetched successfully", result)
        );
    }


    @PutMapping("/lawyers/{lawyerUuid}/status")
    public ResponseEntity<ApiResponse<?>> updateLawyerStatus(
            @PathVariable UUID lawyerUuid,
            @RequestParam AdminLawyerStatus status
    ) {
        superAdminService.updateLawyerStatus(lawyerUuid, status);
        return ResponseEntity.ok(ApiResponse.success(200, "Updated", null));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<AdminUserListResponse>> getUsers(
            @RequestParam(defaultValue = "ALL") String type,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String location,
            @RequestParam(defaultValue = "NEWEST") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        200,
                        "Users fetched successfully",
                        superAdminService.getUsers(type, search, status, location, sort, page, size)
                )
        );
    }

    @GetMapping("/users/summary")
    public ResponseEntity<ApiResponse<AdminUsersSummaryDto>> getUsersSummary() {

        return ResponseEntity.ok(
                ApiResponse.success(
                        200,
                        "User summary fetched successfully",
                        superAdminService.getUsersSummary()
                )
        );
    }

    @GetMapping("/cases")
    public ResponseEntity<ApiResponse<AdminCaseListResponse>> getCases(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "NEWEST") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        200,
                        "Cases fetched successfully",
                        superAdminService.getCases(search, status, type, sort, page, size)
                )
        );
    }

    @GetMapping("/cases/summary")
    public ResponseEntity<ApiResponse<AdminCasesSummaryDto>> getCasesSummary() {

        return ResponseEntity.ok(
                ApiResponse.success(
                        200,
                        "Case summary fetched successfully",
                        superAdminService.getCasesSummary()
                )
        );
    }

    @GetMapping("/subscriptions")
    @PreAuthorize("permitAll()")
    public ResponseEntity<ApiResponse<List<SubscriptionPlanDto>>> getSubscriptions() {

        return ResponseEntity.ok(
                ApiResponse.success(
                        200,
                        "Subscriptions fetched successfully",
                        superAdminService.getSubscriptions()
                )
        );
    }

    @PutMapping("/subscriptions/{uuid}")
    public ResponseEntity<ApiResponse<SubscriptionPlanDto>> updateSubscription(
            @PathVariable UUID uuid,
            @RequestBody UpdateSubscriptionPlanDto dto
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        200,
                        "Subscription updated successfully",
                        superAdminService.updateSubscription(uuid, dto)
                )
        );
    }

    @GetMapping("/system-settings")
    public ResponseEntity<ApiResponse<SystemSettingsDto>> getSystemSettings() {

        return ResponseEntity.ok(
                ApiResponse.success(
                        200,
                        "System settings fetched successfully",
                        superAdminService.getSystemSettings()
                )
        );
    }

    @PutMapping("/system-settings")
    public ResponseEntity<ApiResponse<SystemSettingsDto>> updateSystemSettings(
            @RequestBody UpdateSystemSettingsDto dto
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        200,
                        "System settings updated successfully",
                        superAdminService.updateSystemSettings(dto)
                )
        );
    }

    @GetMapping("/reviews")
    public ResponseEntity<ApiResponse<AdminReviewListResponse>> getReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        200,
                        "Reviews fetched successfully",
                        superAdminService.getReviews(page, size)
                )
        );
    }

    @GetMapping("/reviews/summary")
    public ResponseEntity<ApiResponse<ReviewsSummaryDto>> getReviewsSummary() {

        return ResponseEntity.ok(
                ApiResponse.success(
                        200,
                        "Reviews summary fetched successfully",
                        superAdminService.getReviewsSummary()
                )
        );
    }

    @GetMapping("/subscriptions/summary")
    public ResponseEntity<ApiResponse<AdminSubscriptionsSummaryDto>> getSubscriptionsSummary() {

        return ResponseEntity.ok(
                ApiResponse.success(
                        200,
                        "Subscriptions summary fetched successfully",
                        superAdminService.getSubscriptionsSummary()
                )
        );
    }

    @GetMapping("/subscriptions/plans-summary")
    public ResponseEntity<ApiResponse<AdminSubscriptionPlansSummaryResponse>> getSubscriptionsPlansSummary() {

        return ResponseEntity.ok(
                ApiResponse.success(
                        200,
                        "Subscription plans summary fetched successfully",
                        superAdminService.getSubscriptionsPlansSummary()
                )
        );
    }

    @GetMapping("/subscriptions/recent")
    public ResponseEntity<ApiResponse<List<AdminRecentSubscriberDto>>> getRecentSubscribers() {

        return ResponseEntity.ok(
                ApiResponse.success(
                        200,
                        "Recent subscribers fetched successfully",
                        superAdminService.getRecentSubscribers()
                )
        );
    }

    @GetMapping("/recent-activity")
    public ResponseEntity<ApiResponse<List<ActivityLogDto>>> getRecentActivity(
            @RequestParam(required = false) Integer limit
    ) {
        List<ActivityLogDto> activities =
                superAdminService.getRecentActivities(limit);

        return ResponseEntity.ok(
                ApiResponse.success(
                        200,
                        "Recent activity fetched successfully",
                        activities
                )
        );
    }
}
