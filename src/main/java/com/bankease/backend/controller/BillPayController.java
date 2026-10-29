package com.bankease.backend.controller;

import com.bankease.backend.dto.ApiResponse;
import com.bankease.backend.entity.BillCategory;
import com.bankease.backend.entity.BillPayee;
import com.bankease.backend.entity.BillPayment;
import com.bankease.backend.entity.User;
import com.bankease.backend.security.AuthHelper;
import com.bankease.backend.service.BillPaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bills")
public class BillPayController {

    private final BillPaymentService billPaymentService;
    private final AuthHelper authHelper;

    public BillPayController(BillPaymentService billPaymentService, AuthHelper authHelper) {
        this.billPaymentService = billPaymentService;
        this.authHelper = authHelper;
    }

    @GetMapping("/payees")
    public ResponseEntity<ApiResponse<List<BillPayee>>> getPayees(@RequestHeader("Authorization") String authHeader) {
        User user = authHelper.authenticate(authHeader);
        return ResponseEntity.ok(ApiResponse.success("Payees fetched", billPaymentService.getUserPayees(user)));
    }

    @PostMapping("/payees")
    public ResponseEntity<ApiResponse<BillPayee>> addPayee(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> body) {
        User user = authHelper.authenticate(authHeader);
        String payeeName = body.get("payeeName");
        BillCategory category = BillCategory.valueOf(body.get("category"));
        String consumerNumber = body.get("consumerNumber");
        String nickname = body.get("nickname");

        BillPayee payee = billPaymentService.addPayee(user, payeeName, category, consumerNumber, nickname);
        return ResponseEntity.ok(ApiResponse.success("Payee registered successfully.", payee));
    }

    @PostMapping("/pay")
    public ResponseEntity<ApiResponse<BillPayment>> payBill(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> body) {
        User user = authHelper.authenticate(authHeader);
        String accountNumber = (String) body.get("accountNumber");
        String payeeName = (String) body.get("payeeName");
        BillCategory category = BillCategory.valueOf((String) body.get("category"));
        String consumerNumber = (String) body.get("consumerNumber");
        BigDecimal amount = new BigDecimal(body.get("amount").toString());

        BillPayment payment = billPaymentService.payBill(user, accountNumber, payeeName, category, consumerNumber, amount);
        return ResponseEntity.ok(ApiResponse.success("Bill paid successfully.", payment));
    }

    @PostMapping("/schedule")
    public ResponseEntity<ApiResponse<BillPayment>> scheduleBill(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> body) {
        User user = authHelper.authenticate(authHeader);
        String accountNumber = (String) body.get("accountNumber");
        String payeeName = (String) body.get("payeeName");
        BillCategory category = BillCategory.valueOf((String) body.get("category"));
        String consumerNumber = (String) body.get("consumerNumber");
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        LocalDate scheduledDate = LocalDate.parse((String) body.get("scheduledDate"));

        BillPayment scheduled = billPaymentService.scheduleBill(user, accountNumber, payeeName, category, consumerNumber, amount, scheduledDate);
        return ResponseEntity.ok(ApiResponse.success("Bill scheduled successfully for " + scheduledDate, scheduled));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<BillPayment>>> getHistory(@RequestHeader("Authorization") String authHeader) {
        User user = authHelper.authenticate(authHeader);
        return ResponseEntity.ok(ApiResponse.success("Payment history retrieved", billPaymentService.getPaymentHistory(user)));
    }
}

