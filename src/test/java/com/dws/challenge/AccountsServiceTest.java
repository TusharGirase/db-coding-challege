package com.dws.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.MoneyTransferRequest;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.exception.InvalidAccountIdExcption;
import com.dws.challenge.exception.InvalidMoneyTransferRequest;
import com.dws.challenge.service.AccountsService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class AccountsServiceTest {

	@Autowired
	private AccountsService accountsService;

	@BeforeEach
	public void setup() {
		accountsService.getAccountsRepository().clearAccounts();
	}

	@Test
	void addAccount() {
		Account account = new Account("Id-123");
		account.setBalance(new BigDecimal(1000));
		this.accountsService.createAccount(account);

		assertThat(this.accountsService.getAccount("Id-123")).isEqualTo(account);
	}

	@Test
	void addAccount_failsOnDuplicateId() {
		String uniqueId = "Id-" + System.currentTimeMillis();
		Account account = new Account(uniqueId);
		this.accountsService.createAccount(account);

		try {
			this.accountsService.createAccount(account);
			fail("Should have failed when adding duplicate account");
		} catch (DuplicateAccountIdException ex) {
			assertThat(ex.getMessage()).isEqualTo("Account id " + uniqueId + " already exists!");
		}
	}

	@Test
	void transferMoney_ifInvalidFromAccount() {

		String accountId1 = "Id-1";
		String accountId2 = "Id-2";
		Account account1 = new Account(accountId1, new BigDecimal("123.45"));
		this.accountsService.createAccount(account1);
		Account account2 = new Account(accountId2, new BigDecimal("253.33"));
		this.accountsService.createAccount(account2);

		try {
			this.accountsService.transferMoney(MoneyTransferRequest.builder().accountFrom("Id-552")
					.accountTo(accountId2).amount(new BigDecimal(200)).build());
			fail("Should have failed when invalid account id passed");
		} catch (InvalidAccountIdExcption ex) {
			assertThat(ex.getMessage()).isEqualTo("Invalid account id provided Id-552");
		}
	}

	@Test
	void transferMoney_ifInvalidToAccount() {

		String accountId1 = "Id-1";
		String accountId2 = "Id-2";
		Account account1 = new Account(accountId1, new BigDecimal("123.45"));
		this.accountsService.createAccount(account1);
		Account account2 = new Account(accountId2, new BigDecimal("253.33"));
		this.accountsService.createAccount(account2);

		try {
			this.accountsService.transferMoney(MoneyTransferRequest.builder().accountFrom(accountId1)
					.accountTo("Id-552").amount(new BigDecimal(100)).build());
			fail("Should have failed when invalid account id passed");
		} catch (InvalidAccountIdExcption ex) {
			assertThat(ex.getMessage()).isEqualTo("Invalid account id provided Id-552");
		}
	}

	@Test
	void transferMoney_ifAccountOverdraft() {

		String accountId1 = "Id-1";
		String accountId2 = "Id-2";
		Account account1 = new Account(accountId1, new BigDecimal("123.45"));
		this.accountsService.createAccount(account1);
		Account account2 = new Account(accountId2, new BigDecimal("253.33"));
		this.accountsService.createAccount(account2);

		try {
			this.accountsService.transferMoney(MoneyTransferRequest.builder().accountFrom(accountId1)
					.accountTo(accountId2).amount(new BigDecimal(200)).build());
			fail("Should have failed when transfer request is overdraft");
		} catch (InvalidMoneyTransferRequest ex) {
			assertThat(ex.getMessage()).isEqualTo("Requested amount to transfer is overdrafting account.");
		}
	}
}
