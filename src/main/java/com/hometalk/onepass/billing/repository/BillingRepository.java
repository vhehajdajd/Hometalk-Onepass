package com.hometalk.onepass.billing.repository;

import com.hometalk.onepass.billing.dto.BillingMonthlyStats;
import com.hometalk.onepass.billing.dto.UserOldestUnpaidProjection;
import com.hometalk.onepass.billing.entity.Billing;
import com.hometalk.onepass.billing.entity.BillingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BillingRepository extends JpaRepository<Billing, Long> {

    // ─────────────────────────────────────────────────────────
    // 대시보드 - 관리자 특정 월의 '미납 총액' 합계
    // ─────────────────────────────────────────────────────────
    @Query("SELECT SUM(b.totalAmount) FROM Billing b " +
            "WHERE b.billingMonth = :billingMonth AND b.status = :status")
    Long sumTotalAmountByBillingMonthAndStatus(@Param("billingMonth") String billingMonth
            , @Param("status") BillingStatus status);


    // ─────────────────────────────────────────────────────────
    // 입주민 목록 (필터 + 페이징)
    // ─────────────────────────────────────────────────────────

    @Query("""
            SELECT b FROM Billing b
            WHERE b.household.id = :householdId
              AND (:yearFrom IS NULL OR b.billingMonth >= :yearFrom)
              AND (:yearTo   IS NULL OR b.billingMonth <= :yearTo)
              AND (:month    IS NULL OR b.billingMonth  = :month)
              AND (:status   IS NULL OR b.status        = :status)
            ORDER BY b.billingMonth DESC
            """)
    Page<Billing> findByHouseholdIdWithFilter(
            @Param("householdId") Long householdId,
            @Param("yearFrom")    String yearFrom,
            @Param("yearTo")      String yearTo,
            @Param("month")       String month,
            @Param("status")      BillingStatus status,
            Pageable pageable
    );

    // 해당 월 데이터 (전체 세대) 삭제
    List<Billing> findAllByBillingMonth(String billingMonth);

    // 동별 해당 월 데이터 (특정 동 모든 세대) 삭제
    List<Billing> findAllByBillingMonthAndHousehold_Dong(String billingMonth, String dong);

    // 이번 달 청구서 단건
    Optional<Billing> findByHousehold_IdAndBillingMonth(Long householdId, String billingMonth);

    // 가장 최근 고지서 1건 조회 (billingMonth 내림차순)
    Optional<Billing> findTopByHousehold_IdOrderByBillingMonthDesc(Long householdId);

    // 미납 건수
    int countByHousehold_IdAndStatus(Long householdId, BillingStatus status);

    // 최신 미납 1건
    @Query("""
            SELECT b FROM Billing b
            WHERE b.household.id = :householdId
              AND b.status = 'UNPAID'
            ORDER BY b.billingMonth DESC
            LIMIT 1
            """)
    Optional<Billing> findLatestUnpaidByHouseholdId(@Param("householdId") Long householdId);

    // 최신 납부완료 1건 (입주민 요약 카드: 최근 납부일 표시용)
    @Query("""
            SELECT b FROM Billing b
            WHERE b.household.id = :householdId
              AND b.status = 'PAID'
            ORDER BY b.billingMonth DESC
            LIMIT 1
            """)
    Optional<Billing> findLatestPaidByHouseholdId(@Param("householdId") Long householdId);

    // 3개월 이상 미납 중 가장 오래된 건 조회 (입주민 대시보드 우선 노출용)
    Optional<Billing> findTopByHousehold_IdAndStatusAndBillingMonthLessThanEqualOrderByBillingMonthAsc(
            Long householdId, BillingStatus status, String billingMonth);


    // ─────────────────────────────────────────────────────────
    // 관리자 공통 목록 (필터 + 페이징)
    //   - 업로드 화면: status=null → 전체 조회
    //   - 미납 관리:   status=UNPAID → 미납만 조회
    //   - overdueBefore: 해당 월 이전 미납 → "3개월 이상 체납" 필터
    // ─────────────────────────────────────────────────────────

    @Query("""
            SELECT b FROM Billing b
            WHERE (:dong          IS NULL OR b.household.dong = :dong)
              AND (:yearFrom      IS NULL OR b.billingMonth  >= :yearFrom)
              AND (:yearTo        IS NULL OR b.billingMonth  <= :yearTo)
              AND (:month         IS NULL OR b.billingMonth   = :month)
              AND (:monthOnly     IS NULL OR SUBSTRING(b.billingMonth, 6, 2) = :monthOnly)
              AND (:status        IS NULL OR b.status         = :status)
              AND (:overdueBefore IS NULL OR b.billingMonth  <= :overdueBefore)
            ORDER BY b.billingMonth DESC, b.household.dong ASC, b.household.ho ASC
            """)
    Page<Billing> findAllWithAdminFilter(
            @Param("dong")          String dong,
            @Param("yearFrom")      String yearFrom,
            @Param("yearTo")        String yearTo,
            @Param("month")         String month,
            @Param("monthOnly")     String monthOnly,
            @Param("status")        BillingStatus status,
            @Param("overdueBefore") String overdueBefore,
            Pageable pageable
    );

    // ─────────────────────────────────────────────────────────
    // 관리자 통계
    // ─────────────────────────────────────────────────────────

    @Query("SELECT COUNT(DISTINCT b.household.id) FROM Billing b WHERE b.billingMonth = :billingMonth")
    long countDistinctHouseholdByBillingMonth(@Param("billingMonth") String billingMonth);

    long countByBillingMonthAndStatus(String billingMonth, BillingStatus status);

    // ─────────────────────────────────────────────────────────
    // 업로드 중복 확인
    // ─────────────────────────────────────────────────────────

    boolean existsByBillingMonth(String billingMonth);

    // ─────────────────────────────────────────────────────────
    // 납부완료 처리 (status UNPAID → PAID)
    // ─────────────────────────────────────────────────────────

    @Modifying
    @Query("UPDATE Billing b SET b.status = :status WHERE b.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") BillingStatus status);

    // ─────────────────────────────────────────────────────────
    // 전체 기간 미납 통계
    // ─────────────────────────────────────────────────────────

    // 전체 UNPAID billing 건수
    @Query("SELECT COUNT(b) FROM Billing b WHERE b.status = 'UNPAID'")
    long countAllUnpaid();

    // 전체 기간 미납 세대 수 (중복 제거)
    @Query("SELECT COUNT(DISTINCT b.household.id) FROM Billing b WHERE b.status = 'UNPAID'")
    long countDistinctUnpaidHouseholds();

    // 3개월 이상 체납 세대 수
    @Query("""
    SELECT COUNT(DISTINCT b.household.id) FROM Billing b
    WHERE b.status = 'UNPAID'
      AND b.billingMonth <= :overdueBefore
    """)
    long countDistinctOverdueHouseholds(@Param("overdueBefore") String overdueBefore);

    // 필터 기준 납부완료 세대 수
    @Query("""
    SELECT COUNT(b) FROM Billing b
    WHERE (:dong     IS NULL OR b.household.dong = :dong)
      AND (:yearFrom IS NULL OR b.billingMonth  >= :yearFrom)
      AND (:yearTo   IS NULL OR b.billingMonth  <= :yearTo)
      AND (:month    IS NULL OR b.billingMonth   = :month)
      AND b.status = 'PAID'
    """)
    long countPaidWithFilter(
            @Param("dong")     String dong,
            @Param("yearFrom") String yearFrom,
            @Param("yearTo")   String yearTo,
            @Param("month")    String month
    );

    // 필터 기준 미납 세대 수
    @Query("""
    SELECT COUNT(b) FROM Billing b
    WHERE (:dong     IS NULL OR b.household.dong = :dong)
      AND (:yearFrom IS NULL OR b.billingMonth  >= :yearFrom)
      AND (:yearTo   IS NULL OR b.billingMonth  <= :yearTo)
      AND (:month    IS NULL OR b.billingMonth   = :month)
      AND b.status = 'UNPAID'
    """)
    long countUnpaidWithFilter(
            @Param("dong")     String dong,
            @Param("yearFrom") String yearFrom,
            @Param("yearTo")   String yearTo,
            @Param("month")    String month
    );

    // 통계 - 연도만 선택 시 해당 연도 전체 합산
    @Query("SELECT SUM(b.totalAmount) FROM Billing b " +
            "WHERE b.billingMonth >= :from AND b.billingMonth <= :to " +
            "AND b.status = :status")
    Long sumTotalAmountByYearAndStatus(
            @Param("from") String from,
            @Param("to") String to,
            @Param("status") BillingStatus status);

    // BillingRepository.java
    List<Billing> findAllByHouseholdIdOrderByStatusDescBillingMonthAsc(Long householdId);

    // ─────────────────────────────────────────────────────────
    // BillingScheduler용: 유저별 가장 오래된 미납 billing
    // ─────────────────────────────────────────────────────────

    /**
     * 현재 미납 상태(UNPAID)이고 dueDate가 오늘 이전인 billing 중
     * RESIDENT 유저별로 가장 오래된 1건씩 조회한다.
     *
     * 반환 필드
     *   userId        — 알림 발송 대상 user.id
     *   billingId     — notification의 referenceId로 사용 (중복 방지 키)
     *   oldestDueDate — 미납/체납 경과 개월 수 계산 기준
     */
    @Query(value = """
        SELECT
            u.id            AS userId,
            MIN(b.id)       AS billingId,
            MIN(b.due_date) AS oldestDueDate
        FROM billing b
        JOIN household h ON b.household_id = h.id
        JOIN users u     ON u.household_id = h.id
        WHERE b.status   = 'UNPAID'
          AND b.due_date < :today
          AND u.role     = 'RESIDENT'
        GROUP BY u.id
        """, nativeQuery = true)
    List<UserOldestUnpaidProjection> findOldestUnpaidByUser(
            @Param("today") LocalDate today
    );

    // ─────────────────────────────────────────────────────────
    // BillingScheduler용: 특정 월 정산 통계
    // ─────────────────────────────────────────────────────────

    /**
     * 특정 billingMonth의 정산 통계를 집계한다.
     * 매월 1일 09:00 BILLING_MONTHLY_SUMMARY 알림 메시지 내용 구성에 사용.
     *
     * @param billingMonth "yyyy-MM" 형식 (예: "2026-04")
     */
    @Query(value = """
        SELECT
            COUNT(*)                                                              AS totalCount,
            SUM(CASE WHEN b.status = 'PAID'   THEN 1             ELSE 0   END)  AS paidCount,
            SUM(CASE WHEN b.status = 'UNPAID' THEN 1             ELSE 0   END)  AS unpaidCount,
            SUM(b.total_amount)                                                   AS totalAmount,
            SUM(CASE WHEN b.status = 'PAID'   THEN b.total_amount ELSE 0  END)  AS paidAmount,
            SUM(CASE WHEN b.status = 'UNPAID' THEN b.total_amount ELSE 0  END)  AS unpaidAmount
        FROM billing b
        WHERE b.billing_month = :billingMonth
        """, nativeQuery = true)
    BillingMonthlyStats getMonthlyStats(
            @Param("billingMonth") String billingMonth
    );

}