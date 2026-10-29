package com.bankease.backend.controller;

import com.bankease.backend.dto.ApiResponse;
import com.bankease.backend.entity.Investment;
import com.bankease.backend.entity.InvestmentType;
import com.bankease.backend.entity.User;
import com.bankease.backend.security.AuthHelper;
import com.bankease.backend.service.InvestmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/investments")
@CrossOrigin(origins = "*")
public class InvestmentController {

    private final InvestmentService investmentService;
    private final AuthHelper authHelper;

    public InvestmentController(InvestmentService investmentService, AuthHelper authHelper) {
        this.investmentService = investmentService;
        this.authHelper = authHelper;
    }

    @PostMapping("/calculate-fd")
    public ResponseEntity<ApiResponse<Map<String, Object>>> calculateFd(@RequestBody Map<String, Object> body) {
        BigDecimal principal = new BigDecimal(body.get("principal").toString());
        BigDecimal rate = new BigDecimal(body.get("rate").toString());
        int tenureMonths = Integer.parseInt(body.get("tenureMonths").toString());

        BigDecimal maturityAmount = investmentService.calculateFdMaturity(principal, rate, tenureMonths);
        BigDecimal interestEarned = maturityAmount.subtract(principal);

        return ResponseEntity.ok(ApiResponse.success("FD return estimated.", Map.of(
                "principal", principal,
                "annualRate", rate,
                "tenureMonths", tenureMonths,
                "maturityAmount", maturityAmount,
                "interestEarned", interestEarned
        )));
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Investment>> createInvestment(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> body) {
        User user = authHelper.authenticate(authHeader);
        String accountNumber = (String) body.get("accountNumber");
        InvestmentType type = InvestmentType.valueOf((String) body.get("type"));
        String planName = (String) body.get("planName");
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        BigDecimal returnRate = new BigDecimal(body.get("returnRate").toString());
        int tenureMonths = Integer.parseInt(body.get("tenureMonths").toString());

        Investment inv = investmentService.createInvestment(user, accountNumber, type, planName, amount, returnRate, tenureMonths);
        return ResponseEntity.ok(ApiResponse.success("Investment account opened successfully.", inv));
    }

    @GetMapping("/my-investments")
    public ResponseEntity<ApiResponse<List<Investment>>> getMyInvestments(@RequestHeader("Authorization") String authHeader) {
        User user = authHelper.authenticate(authHeader);
        return ResponseEntity.ok(ApiResponse.success("Investments retrieved", investmentService.getUserInvestments(user)));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<ApiResponse<Investment>> closeInvestment(
            @PathVariable("id") Long id,
            @RequestHeader("Authorization") String authHeader) {
        User user = authHelper.authenticate(authHeader);
        Investment inv = investmentService.closeInvestment(id, user);
        return ResponseEntity.ok(ApiResponse.success("Investment closed and funds credited to your account.", inv));
    }
}
