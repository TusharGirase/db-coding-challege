package com.dws.challenge.service;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.MoneyTransferRequest;
import com.dws.challenge.exception.InvalidAccountIdExcption;
import com.dws.challenge.exception.InvalidMoneyTransferRequest;
import com.dws.challenge.repository.AccountsRepository;

import lombok.Getter;

@Service
public class AccountsService {

	@Getter
	private final AccountsRepository accountsRepository;

	private final NotificationService notificationService;

	@Autowired
	public AccountsService(AccountsRepository accountsRepository, NotificationService notificationService) {
		this.accountsRepository = accountsRepository;
		this.notificationService = notificationService;
	}

	public void createAccount(Account account) {
		this.accountsRepository.createAccount(account);
	}

	public Account getAccount(String accountId) {
		return this.accountsRepository.getAccount(accountId);
	}

	public MoneyTransferResult transferMoney(MoneyTransferRequest moneyTransferRequest) {
		Account accountFrom = accountsRepository.getAccount(moneyTransferRequest.getAccountFrom());
		if (accountFrom == null) {
			throw new InvalidAccountIdExcption(moneyTransferRequest.getAccountFrom());
		}
		BigDecimal accountFromUpdatedBalance = accountFrom.getBalance().subtract(moneyTransferRequest.getAmount());

		if (accountFromUpdatedBalance.compareTo(BigDecimal.ZERO) < 0) {
			throw new InvalidMoneyTransferRequest("Requested amount to transfer is overdrafting account.");
		}

		Account accountTo = accountsRepository.getAccount(moneyTransferRequest.getAccountTo());
		if (accountTo == null) {
			throw new InvalidAccountIdExcption(moneyTransferRequest.getAccountTo());
		}

		Object lock1 = accountFrom.getLOCK();
		Object lock2 = accountTo.getLOCK();

		if (accountFrom.hashCode() > accountTo.hashCode()) {
			lock2 = accountFrom.getLOCK();
			lock1 = accountTo.getLOCK();
		}
		synchronized (lock1) {
			synchronized (lock2) {
				BigDecimal accountToUpdatedBalance = accountTo.getBalance().add(moneyTransferRequest.getAmount());
				accountsRepository.updateBalance(accountFrom.getAccountId(), accountFromUpdatedBalance);
				accountsRepository.updateBalance(accountTo.getAccountId(), accountToUpdatedBalance);
			}
		}
		notificationService.notifyAboutTransfer(accountFrom,
				"Your account is debited with amount " + moneyTransferRequest.getAmount());
		notificationService.notifyAboutTransfer(accountTo,
				"Your account is credited with amount " + moneyTransferRequest.getAmount());
		return MoneyTransferResult.SUCCESS;
	}
}
