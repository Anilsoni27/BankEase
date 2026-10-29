package com.bankease.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "investments")
public class Investment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InvestmentType investmentType;

    @Column(nullable = false, length = 100)
    private String planName;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal investedAmount;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal interestOrReturnRate;

    @Column(nullable = false)
    private int tenureMonths;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal maturityAmount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal currentValuation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvestmentStatus status = InvestmentStatus.ACTIVE;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate maturityDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Investment() {
    }

    public Investment(User user, Account account, InvestmentType investmentType, String planName,
                      BigDecimal investedAmount, BigDecimal interestOrReturnRate, int tenureMonths,
                      BigDecimal maturityAmount, LocalDate startDate, LocalDate maturityDate) {
        this.user = user;
        this.account = account;
        this.investmentType = investmentType;
        this.planName = planName;
        this.investedAmount = investedAmount;
        this.interestOrReturnRate = interestOrReturnRate;
        this.tenureMonths = tenureMonths;
        this.maturityAmount = maturityAmount;
        this.currentValuation = investedAmount;
        this.startDate = startDate;
        this.maturityDate = maturityDate;
        this.status = InvestmentStatus.ACTIVE;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }

    public InvestmentType getInvestmentType() { return investmentType; }
    public void setInvestmentType(InvestmentType investmentType) { this.investmentType = investmentType; }

    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }

    public BigDecimal getInvestedAmount() { return investedAmount; }
    public void setInvestedAmount(BigDecimal investedAmount) { this.investedAmount = investedAmount; }

    public BigDecimal getInterestOrReturnRate() { return interestOrReturnRate; }
    public void setInterestOrReturnRate(BigDecimal interestOrReturnRate) { this.interestOrReturnRate = interestOrReturnRate; }

    public int getTenureMonths() { return tenureMonths; }
    public void setTenureMonths(int tenureMonths) { this.tenureMonths = tenureMonths; }

    public BigDecimal getMaturityAmount() { return maturityAmount; }
    public void setMaturityAmount(BigDecimal maturityAmount) { this.maturityAmount = maturityAmount; }

    public BigDecimal getCurrentValuation() { return currentValuation; }
    public void setCurrentValuation(BigDecimal currentValuation) { this.currentValuation = currentValuation; }

    public InvestmentStatus getStatus() { return status; }
    public void setStatus(InvestmentStatus status) { this.status = status; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getMaturityDate() { return maturityDate; }
    public void setMaturityDate(LocalDate maturityDate) { this.maturityDate = maturityDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
