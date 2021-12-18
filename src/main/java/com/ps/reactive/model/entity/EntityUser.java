package com.ps.reactive.model.entity;

import io.vertx.mutiny.sqlclient.Row;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EntityUser {

	private int id;
	private String firstName;
	private String lastName;
	private String email;
	private String password;

	public static EntityUser fromRow(Row r) {
		EntityUser u = new EntityUser();
		u.id = r.getInteger("id");
		u.email = r.getString("email");
		u.firstName = r.getString("firstName");
		u.lastName = r.getString("lastName");
		u.password = r.getString("password");
		return u;
	}

	public static EntityUser maskPassword(EntityUser u) {
		u.password = "XXXX";
		return u;
	}
}
