package com.bankease.backend.controller;

import com.bankease.backend.dto.ApiResponse;
import com.bankease.backend.entity.Loan;
import com.bankease.backend.entity.LoanType;
import com.bankease.backend.entity.User;
import com.bankease.backend.security.AuthHelper;
import com.bankease.backend.service.LoanService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/loans")
@CrossOrigin(origins = "*")
public class LoanController {

    private final LoanService loanService;
    private final AuthHelper authHelper;

    public LoanController(LoanService loanService, AuthHelper authHelper) {
        this.loanService = loanService;
        this.authHelper = authHelper;
    }

    @PostMapping("/calculate-emi")
    public ResponseEntity<ApiResponse<Map<String, Object>>> calculateEmi(@RequestBody Map<String, Object> body) {
        BigDecimal principal = new BigDecimal(body.get("principal").toString());
        BigDecimal annualRate = new BigDecimal(body.get("annualRate").toString());
        int tenureMonths = Integer.parseInt(body.get("tenureMonths").toString());

        BigDecimal monthlyEmi = loanService.calculateEmi(principal, annualRate, tenureMonths);
        BigDecimal totalPayment = monthlyEmi.multiply(BigDecimal.valueOf(tenureMonths));
        BigDecimal totalInterest = totalPayment.subtract(principal);

        Map<String, Object> result = Map.of(
                "principal", principal,
                "annualRate", annualRate,
                "tenureMonths", tenureMonths,
                "monthlyEmi", monthlyEmi,
                "totalPayment", totalPayment,
                "totalInterest", totalInterest
        );

        return ResponseEntity.ok(ApiResponse.success("EMI calculated successfully.", result));
    }

    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<Loan>> applyLoan(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> body) {
        User user = authHelper.authenticate(authHeader);
        String accountNumber = (String) body.get("accountNumber");
        LoanType loanType = LoanType.valueOf((String) body.get("loanType"));
        BigDecimal principal = new BigDecimal(body.get("principal").toString());
        BigDecimal annualRate = new BigDecimal(body.get("annualRate").toString());
        int tenureMonths = Integer.parseInt(body.get("tenureMonths").toString());
        String purpose = (String) body.get("purpose");

        Loan loan = loanService.applyLoan(user, accountNumber, loanType, principal, annualRate, tenureMonths, purpose);
        return ResponseEntity.ok(ApiResponse.success("Loan application submitted for review.", loan));
    }

    @GetMapping("/my-loans")
    public ResponseEntity<ApiResponse<List<Loan>>> getMyLoans(@RequestHeader("Authorization") String authHeader) {
        User user = authHelper.authenticate(authHeader);
        return ResponseEntity.ok(ApiResponse.success("User loans retrieved", loanService.getUserLoans(user)));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<Loan>>> getAllLoans(@RequestHeader("Authorization") String authHeader) {
        User adminUser = authHelper.authenticate(authHeader);
        return ResponseEntity.ok(ApiResponse.success("All loans retrieved", loanService.getAllLoans(adminUser)));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<Loan>> approveLoan(
            @PathVariable("id") Long id,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody(required = false) Map<String, String> body) {
        User adminUser = authHelper.authenticate(authHeader);
        String remarks = (body != null) ? body.get("remarks") : "Approved";
        Loan loan = loanService.approveLoan(id, adminUser, remarks);
        return ResponseEntity.ok(ApiResponse.success("Loan approved and funds disbursed.", loan));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<Loan>> rejectLoan(
            @PathVariable("id") Long id,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody(required = false) Map<String, String> body) {
        User adminUser = authHelper.authenticate(authHeader);
        String remarks = (body != null) ? body.get("remarks") : "Rejected";
        Loan loan = loanService.rejectLoan(id, adminUser, remarks);
        return ResponseEntity.ok(ApiResponse.success("Loan rejected.", loan));
    }
}
