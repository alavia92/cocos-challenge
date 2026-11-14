package com.cocos.challenge.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	private String email;

	@Column(name = "accountnumber")
	private String accountNumber;

	protected User() {
	}

	public Integer getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getAccountNumber() {
		return accountNumber;
	}

}
