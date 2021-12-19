package com.ps.reactive.model.request;

import com.ps.reactive.model.entity.EntityUser;
import com.ps.reactive.model.validator.UserValidationGroup;
import lombok.AllArgsConstructor;
import lombok.Data;

import javax.validation.constraints.*;

@Data
@AllArgsConstructor
public class User {

	@NotNull(groups = UserValidationGroup.Update.class, message = "Missing user id")
	private final int id;

	@NotBlank(message = "First Name is required")
	private final String firstName;
	private final String lastName;

	@NotBlank(message = "Email is required")
	@Email(message = "Email address is invalid")
	private final String email;

	@NotBlank(groups = UserValidationGroup.Add.class, message = "Password is required")
	//@Null(groups = UserValidationGroup.Update.class, message = "Use ChangePassword Endpoint")
	private final String password;


	public final EntityUser toEntity() {
		return EntityUser.builder()
			.firstName(this.firstName)
			.lastName(this.lastName)
			.email(this.email)
			.password(this.password)
			.build();
	}

}
