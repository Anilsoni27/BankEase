package com.bankease.backend.init;

import com.bankease.backend.entity.*;
import com.bankease.backend.repository.AccountRepository;
import com.bankease.backend.repository.TransactionRepository;
import com.bankease.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * DataSeeder runs automatically on application startup.
 *
 * Annotations explained:
 * - @Component: Registers this class as a Spring Bean.
 * - CommandLineRunner: A Spring Boot interface with a run() method that executes
 *   once right after the ApplicationContext is fully loaded and before the app accepts requests.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      AccountRepository accountRepository,
                      TransactionRepository transactionRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Only seed sample data if the users table is completely empty
        if (userRepository.count() > 0) {
            log.info("Database already seeded. Skipping initial data creation.");
            return;
        }

        log.info("Seeding initial BankEase users and sample accounts...");

        // --------------------------------------------------------------------
        // 1. Seed System Administrator
        // --------------------------------------------------------------------
        User admin = new User(
                "System Admin",
                "admin@bankease.com",
                passwordEncoder.encode("admin123"),
                Role.ADMIN
        );
        userRepository.save(admin);

        Account adminAccount = new Account(
                "1009999999",
                admin,
                new BigDecimal("50000.00"),
                AccountType.CURRENT
        );
        accountRepository.save(adminAccount);

        transactionRepository.save(new Transaction(
                null,
                adminAccount,
                new BigDecimal("50000.00"),
                TransactionType.DEPOSIT,
                TransactionStatus.SUCCESS,
                "Admin initial liquidity reserve"
        ));

        // --------------------------------------------------------------------
        // 2. Seed Customer 1: Rahul Sharma
        // --------------------------------------------------------------------
        User customer1 = new User(
                "Rahul Sharma",
                "rahul@bankease.com",
                passwordEncoder.encode("rahul123"),
                Role.CUSTOMER
        );
        userRepository.save(customer1);

        Account account1 = new Account(
                "1001001001",
                customer1,
                new BigDecimal("10000.00"),
                AccountType.SAVINGS
        );
        accountRepository.save(account1);

        transactionRepository.save(new Transaction(
                null,
                account1,
                new BigDecimal("10000.00"),
                TransactionType.DEPOSIT,
                TransactionStatus.SUCCESS,
                "Welcome bonus / demo opening credit"
        ));

        // --------------------------------------------------------------------
        // 3. Seed Customer 2: Priya Patel
        // --------------------------------------------------------------------
        User customer2 = new User(
                "Priya Patel",
                "priya@bankease.com",
                passwordEncoder.encode("priya123"),
                Role.CUSTOMER
        );
        userRepository.save(customer2);

        Account account2 = new Account(
                "1002002002",
                customer2,
                new BigDecimal("10000.00"),
                AccountType.SAVINGS
        );
        accountRepository.save(account2);

        transactionRepository.save(new Transaction(
                null,
                account2,
                new BigDecimal("10000.00"),
                TransactionType.DEPOSIT,
                TransactionStatus.SUCCESS,
                "Welcome bonus / demo opening credit"
        ));

        log.info("==================================================================");
        log.info("BankEase Seed Data Ready:");
        log.info("1. Admin:    admin@bankease.com / admin123  (Account: 1009999999)");
        log.info("2. Customer: rahul@bankease.com / rahul123  (Account: 1001001001, Balance: ₹10,000)");
        log.info("3. Customer: priya@bankease.com / priya123  (Account: 1002002002, Balance: ₹10,000)");
        log.info("==================================================================");
    }
}

