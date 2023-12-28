package com.dws.challenge.exception;

public class InvalidMoneyTransferRequest extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public InvalidMoneyTransferRequest(String msg) {
		super(msg);
	}

}
