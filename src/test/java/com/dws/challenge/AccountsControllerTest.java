package com.dws.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import com.dws.challenge.domain.Account;
import com.dws.challenge.service.AccountsService;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@WebAppConfiguration
class AccountsControllerTest {

	private MockMvc mockMvc;

	@Autowired
	private AccountsService accountsService;

	@Autowired
	private WebApplicationContext webApplicationContext;

	@BeforeEach
	void prepareMockMvc() {
		this.mockMvc = webAppContextSetup(this.webApplicationContext).build();

		// Reset the existing accounts before each test.
		accountsService.getAccountsRepository().clearAccounts();
	}

	@Test
	void createAccount() throws Exception {
		this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

		Account account = accountsService.getAccount("Id-123");
		assertThat(account.getAccountId()).isEqualTo("Id-123");
		assertThat(account.getBalance()).isEqualByComparingTo("1000");
	}

	@Test
	void createDuplicateAccount() throws Exception {
		this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

		this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isBadRequest());
	}

	@Test
	void createAccountNoAccountId() throws Exception {
		this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON).content("{\"balance\":1000}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createAccountNoBalance() throws Exception {
		this.mockMvc.perform(
				post("/v1/accounts").contentType(MediaType.APPLICATION_JSON).content("{\"accountId\":\"Id-123\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createAccountNoBody() throws Exception {
		this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createAccountNegativeBalance() throws Exception {
		this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountId\":\"Id-123\",\"balance\":-1000}")).andExpect(status().isBadRequest());
	}

	@Test
	void createAccountEmptyAccountId() throws Exception {
		this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountId\":\"\",\"balance\":1000}")).andExpect(status().isBadRequest());
	}

	@Test
	void getAccount() throws Exception {
		String uniqueAccountId = "Id-" + System.currentTimeMillis();
		Account account = new Account(uniqueAccountId, new BigDecimal("123.45"));
		this.accountsService.createAccount(account);
		this.mockMvc.perform(get("/v1/accounts/" + uniqueAccountId)).andExpect(status().isOk())
				.andExpect(content().string("{\"accountId\":\"" + uniqueAccountId + "\",\"balance\":123.45}"));
	}

	@Test
	void transferMoney() throws Exception {
		String accountId1 = "Id-123";
		String accountId2 = "Id-456";
		Account account1 = new Account(accountId1, new BigDecimal("123.45"));
		this.accountsService.createAccount(account1);
		Account account2 = new Account(accountId2, new BigDecimal("253.33"));
		this.accountsService.createAccount(account2);

		String payload = "{\"accountFrom\": \"" + accountId1 + "\", \"accountTo\": \"" + accountId2
				+ "\", \"amount\": 100}";
		this.mockMvc
				.perform(post("/v1/accounts/money-transfer/").contentType(MediaType.APPLICATION_JSON).content(payload))
				.andExpect(status().isOk());

		this.mockMvc.perform(get("/v1/accounts/" + accountId1)).andExpect(status().isOk())
				.andExpect(content().string("{\"accountId\":\"" + accountId1 + "\",\"balance\":23.45}"));
		this.mockMvc.perform(get("/v1/accounts/" + accountId2)).andExpect(status().isOk())
				.andExpect(content().string("{\"accountId\":\"" + accountId2 + "\",\"balance\":353.33}"));
	}

	@Test
	void transferMoney_ifInvalidFromAccount() throws Exception {
		String accountId1 = "Id-123";
		String accountId2 = "Id-456";
		Account account1 = new Account(accountId1, new BigDecimal("123.45"));
		this.accountsService.createAccount(account1);
		Account account2 = new Account(accountId2, new BigDecimal("253.33"));
		this.accountsService.createAccount(account2);

		String payload = "{\"accountFrom\": \"" + "Id-44" + "\", \"accountTo\": \"" + accountId2
				+ "\", \"amount\": 100}";
		this.mockMvc
				.perform(post("/v1/accounts/money-transfer/").contentType(MediaType.APPLICATION_JSON).content(payload))
				.andExpect(status().isBadRequest());
	}

	@Test
	void transferMoney_ifInvalidToAccount() throws Exception {
		String accountId1 = "Id-123";
		String accountId2 = "Id-456";
		Account account1 = new Account(accountId1, new BigDecimal("123.45"));
		this.accountsService.createAccount(account1);
		Account account2 = new Account(accountId2, new BigDecimal("253.33"));
		this.accountsService.createAccount(account2);

		String payload = "{\"accountFrom\": \"" + accountId1 + "\", \"accountTo\": \"" + "Id-22"
				+ "\", \"amount\": 100}";
		this.mockMvc
				.perform(post("/v1/accounts/money-transfer/").contentType(MediaType.APPLICATION_JSON).content(payload))
				.andExpect(status().isBadRequest());
	}

	@Test
	void transferMoney_ifInvalidAmount() throws Exception {
		String accountId1 = "Id-123";
		String accountId2 = "Id-456";
		Account account1 = new Account(accountId1, new BigDecimal("123.45"));
		this.accountsService.createAccount(account1);
		Account account2 = new Account(accountId2, new BigDecimal("253.33"));
		this.accountsService.createAccount(account2);

		String payload = "{\"accountFrom\": \"" + accountId1 + "\", \"accountTo\": \"" + "Id-22" + "\", \"amount\": 0}";
		this.mockMvc
				.perform(post("/v1/accounts/money-transfer/").contentType(MediaType.APPLICATION_JSON).content(payload))
				.andExpect(status().isBadRequest());
	}

	@Test
	void transferMoney_ifAmountNegative() throws Exception {
		String accountId1 = "Id-123";
		String accountId2 = "Id-456";
		Account account1 = new Account(accountId1, new BigDecimal("123.45"));
		this.accountsService.createAccount(account1);
		Account account2 = new Account(accountId2, new BigDecimal("253.33"));
		this.accountsService.createAccount(account2);

		String payload = "{\"accountFrom\": \"" + accountId1 + "\", \"accountTo\": \"" + "Id-22"
				+ "\", \"amount\": -55}";
		this.mockMvc
				.perform(post("/v1/accounts/money-transfer/").contentType(MediaType.APPLICATION_JSON).content(payload))
				.andExpect(status().isBadRequest());
	}

	@Test
	void transferMoney_ifOverdraft() throws Exception {
		String accountId1 = "Id-123";
		String accountId2 = "Id-456";
		Account account1 = new Account(accountId1, new BigDecimal("123.45"));
		this.accountsService.createAccount(account1);
		Account account2 = new Account(accountId2, new BigDecimal("253.33"));
		this.accountsService.createAccount(account2);

		String payload = "{\"accountFrom\": \"" + accountId1 + "\", \"accountTo\": \"" + accountId2
				+ "\", \"amount\": 500}";
		this.mockMvc
				.perform(post("/v1/accounts/money-transfer/").contentType(MediaType.APPLICATION_JSON).content(payload))
				.andExpect(status().isBadRequest());
	}
}
