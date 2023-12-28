package com.dws.challenge.exception;

public class InvalidAccountIdExcption extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public InvalidAccountIdExcption(String accountId) {
		super("Invalid account id provided " + accountId);
	}

}
