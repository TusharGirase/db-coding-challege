package com.dws.challenge.domain;

import java.math.BigDecimal;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
public class MoneyTransferRequest {

	@NotBlank
	private String accountFrom;

	@NotBlank
	private String accountTo;

	@NotNull
	@Min(value = 1, message = "Amount to transfer should be 1 or more.")
	private BigDecimal amount;

	public MoneyTransferRequest(String accountFrom, String accountTo, BigDecimal amount) {
		super();
		this.accountFrom = accountFrom;
		this.accountTo = accountTo;
		this.amount = amount;
	}

}
