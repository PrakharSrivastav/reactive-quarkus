package com.ps.reactive.repository;

import com.ps.reactive.model.entity.EntityUser;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.mysqlclient.MySQLPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowIterator;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.NotFoundException;
import java.util.List;

@ApplicationScoped
public class UserRepository {

	// sql queries
	private static final String GET_ALL = "select * from user";
	private static final String INSERT_ONE = "insert into user (firstName,lastName,email,password) values(?,?,?,?)";
	private static final String UPDATE_ONE = "update user set email = ?, firstName = ?, lastName = ? where id = ?";
	private static final String GET_ONE_BY_ID = "select * from user where id = ? limit 1";
	private static final String GET_ONE_BY_EMAIL = "select * from user where email = ? limit 1";
	private static final String DELETE_ONE = "delete from user where id = ?";

	private final MySQLPool pool;

	public UserRepository(MySQLPool pool) {
		this.pool = pool;
	}

	// Get list of all the users
	public Uni<List<EntityUser>> getAll() {
		return pool.query(GET_ALL).execute()
			.onItem().transformToMulti(Multi.createFrom()::iterable)
			.onItem().transform(EntityUser::fromRow)
			.collect().asList();
	}

	// create a user
	public Uni<EntityUser> create(final EntityUser user) {
		return pool.withTransaction(pool -> pool.preparedQuery(INSERT_ONE)
			.execute(utilGetCreateTuple(user))
			.onItem().ignore().andSwitchTo(() -> pool.preparedQuery(GET_ONE_BY_EMAIL)
				.execute(Tuple.of(user.getEmail()))
				.onItem().transform(RowSet::iterator)
				.onItem().transform(UserRepository::utilGetUserOrNullFromRow)
			)
		);
	}

	// update an existing user
	public Uni<EntityUser> update(final int id, final EntityUser user) {
		return pool.withTransaction(conn -> conn
			.preparedQuery(GET_ONE_BY_ID).execute(Tuple.of(id))
			.onItem().transform(RowSet::iterator)
			.onItem().transform(UserRepository::utilGetUserOrNullFromRow)
			.onItem().ifNull().failWith(new NotFoundException("user.not.found"))
			.onItem().ifNotNull().transform(dbUser -> utilSetUser(user, dbUser))
			.onItem().transformToUni(dbUser -> conn
				.preparedQuery(UPDATE_ONE).execute(utilGetUpdateTuple(dbUser, id))
				.onItem().ignore().andContinueWithNull()
			)
			.onItem().ignore().andSwitchTo(() -> conn
				.preparedQuery(GET_ONE_BY_ID).execute(Tuple.of(id))
				.onItem().transform(RowSet::iterator)
				.onItem().transform(UserRepository::utilGetUserOrNullFromRow)
			));
	}

	// delete user by id
	public Uni<Void> delete(final int id) {
		return pool.withTransaction(conn -> conn
			.preparedQuery(GET_ONE_BY_ID).execute(Tuple.of(id))
			.onItem().transform(RowSet::iterator)
			.onItem().transform(it -> it.hasNext() ? EntityUser.fromRow(it.next()) : null)
			.onItem().ifNull().failWith(new IllegalArgumentException("User not found"))
			.onItem().ignore().andSwitchTo(() -> conn.preparedQuery(DELETE_ONE).execute(Tuple.of(id))
				.onItem().ignore().andContinueWithNull()
			)
		);
	}

	// get user by id
	public Uni<EntityUser> getOne(final int id) {
		return pool.preparedQuery(GET_ONE_BY_ID)
			.execute(Tuple.of(id))
			.onItem().transform(RowSet::iterator)
			.onItem().transform(UserRepository::utilGetUserOrNullFromRow)
			.onItem().ifNull().failWith(new NotFoundException("user.not.found"));
	}


	private static Tuple utilGetUpdateTuple(final EntityUser u, final int id) {
		return Tuple.of(u.getEmail(), u.getFirstName(), u.getLastName(), id);
	}

	private static EntityUser utilSetUser(final EntityUser user, final EntityUser dbUser) {

		dbUser.setLastName(user.getLastName());
		dbUser.setFirstName(user.getFirstName());
		dbUser.setEmail(user.getEmail());
		return dbUser;
	}

	private static EntityUser utilGetUserOrNullFromRow(RowIterator<Row> i) {
		return i.hasNext() ? EntityUser.fromRow(i.next()) : null;
	}

	private static Tuple utilGetCreateTuple(EntityUser user) {
		return Tuple.of(user.getFirstName(), user.getLastName(), user.getEmail(), user.getPassword());
	}
}
