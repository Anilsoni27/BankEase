package com.bankease.backend.init;

import com.bankease.backend.entity.*;
import com.bankease.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final BillPayeeRepository billPayeeRepository;
    private final LoanRepository loanRepository;
    private final InvestmentRepository investmentRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      AccountRepository accountRepository,
                      TransactionRepository transactionRepository,
                      BillPayeeRepository billPayeeRepository,
                      LoanRepository loanRepository,
                      InvestmentRepository investmentRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.billPayeeRepository = billPayeeRepository;
        this.loanRepository = loanRepository;
        this.investmentRepository = investmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            log.info("Seeding initial BankEase users and sample accounts...");

            // 1. Admin
            User admin = new User("System Admin", "admin@bankease.com", passwordEncoder.encode("admin123"), Role.ADMIN);
            admin.setKycStatus(KycStatus.VERIFIED);
            userRepository.save(admin);

            Account adminAccount = new Account("1009999999", admin, new BigDecimal("50000.00"), AccountType.CURRENT);
            accountRepository.save(adminAccount);

            transactionRepository.save(new Transaction(null, adminAccount, new BigDecimal("50000.00"),
                    TransactionType.DEPOSIT, TransactionStatus.SUCCESS, "Admin initial liquidity reserve"));

            // 2. Customer 1: Rahul
            User customer1 = new User("Rahul Sharma", "rahul@bankease.com", passwordEncoder.encode("rahul123"), Role.CUSTOMER);
            customer1.setKycStatus(KycStatus.VERIFIED);
            userRepository.save(customer1);

            Account account1 = new Account("1001001001", customer1, new BigDecimal("10000.00"), AccountType.SAVINGS);
            accountRepository.save(account1);

            transactionRepository.save(new Transaction(null, account1, new BigDecimal("10000.00"),
                    TransactionType.DEPOSIT, TransactionStatus.SUCCESS, "Welcome bonus / demo opening credit"));

            // 3. Customer 2: Priya
            User customer2 = new User("Priya Patel", "priya@bankease.com", passwordEncoder.encode("priya123"), Role.CUSTOMER);
            customer2.setKycStatus(KycStatus.VERIFIED);
            userRepository.save(customer2);

            Account account2 = new Account("1002002002", customer2, new BigDecimal("10000.00"), AccountType.SAVINGS);
            accountRepository.save(account2);

            transactionRepository.save(new Transaction(null, account2, new BigDecimal("10000.00"),
                    TransactionType.DEPOSIT, TransactionStatus.SUCCESS, "Welcome bonus / demo opening credit"));
        }

        // Seed Sample Bill Payees if empty
        if (billPayeeRepository.count() == 0) {
            userRepository.findByEmail("rahul@bankease.com").ifPresent(rahul -> {
                billPayeeRepository.save(new BillPayee(rahul, "Tata Power - Mumbai", BillCategory.ELECTRICITY, "DEL-98472910", "Home Electricity"));
                billPayeeRepository.save(new BillPayee(rahul, "Airtel Postpaid", BillCategory.MOBILE_RECHARGE, "9876543210", "Personal Mobile"));
                billPayeeRepository.save(new BillPayee(rahul, "Act Fibernet", BillCategory.BROADBAND, "ACT-10928374", "Highspeed WiFi"));
            });
            userRepository.findByEmail("priya@bankease.com").ifPresent(priya -> {
                billPayeeRepository.save(new BillPayee(priya, "Bescom Electricity", BillCategory.ELECTRICITY, "BES-48392019", "Flat Power"));
                billPayeeRepository.save(new BillPayee(priya, "Jio Fiber", BillCategory.BROADBAND, "JIO-77281920", "Home Fiber"));
            });
        }

        // Seed Sample Loan if empty
        if (loanRepository.count() == 0) {
            userRepository.findByEmail("rahul@bankease.com").ifPresent(rahul -> {
                accountRepository.findByAccountNumber("1001001001").ifPresent(acc -> {
                    loanRepository.save(new Loan(rahul, acc, LoanType.PERSONAL, new BigDecimal("100000.00"),
                            new BigDecimal("10.5"), 24, new BigDecimal("4637.60"), "Higher Education & Laptop purchase"));
                });
            });
        }

        // Seed Sample Investment if empty
        if (investmentRepository.count() == 0) {
            userRepository.findByEmail("rahul@bankease.com").ifPresent(rahul -> {
                accountRepository.findByAccountNumber("1001001001").ifPresent(acc -> {
                    investmentRepository.save(new Investment(rahul, acc, InvestmentType.FIXED_DEPOSIT,
                            "BankEase 1-Year Growth FD", new BigDecimal("5000.00"), new BigDecimal("7.50"),
                            12, new BigDecimal("5385.70"), LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(10)));
                });
            });
        }

        log.info("BankEase Full Enterprise Suite Seed Data Ready!");
    }
}
