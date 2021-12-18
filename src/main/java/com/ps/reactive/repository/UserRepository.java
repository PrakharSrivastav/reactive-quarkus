package com.ps.reactive.repository;

import com.ps.reactive.model.entity.EntityUser;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.mysqlclient.MySQLPool;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;

import javax.inject.Singleton;
import java.util.List;

@Singleton
public class UserRepository {

	private static final String GET_ALL = "select * from user";
	private static final String INSERT_ONE = "insert into user (firstName,lastName,email,password) values(?,?,?,?)";
	private static final String UPDATE_ONE = "update user set email = ?, firstName = ?, lastName = ? where id = ?";
	private static final String GET_ONE_BY_ID = "select * from user where id = ? limit 1";
	private static final String DELETE_ONE = "delete from user where id = ?";

	private final MySQLPool pool;

	public UserRepository(MySQLPool pool) {
		this.pool = pool;
	}

	public Uni<List<EntityUser>> getAll() {
		return pool.query(GET_ALL).execute()
			.onItem().transformToMulti(i -> Multi.createFrom().iterable(i))
			.onItem().transform(EntityUser::fromRow)
			.collect().asList();
	}

	public Uni<List<EntityUser>> create(final EntityUser user) {
		return pool.withTransaction(pool -> pool.preparedQuery(INSERT_ONE)
			.execute(Tuple.of(user.getFirstName(), user.getLastName(), user.getEmail(), user.getPassword()))
			.onItem().ignore() // ignore the results from the previous operation
			.andSwitchTo(() -> pool.query(GET_ALL).execute())
			.onItem().transformToMulti(i -> Multi.createFrom().iterable(i))
			.onItem().transform(EntityUser::fromRow)
			.collect().asList()
		);
	}

	public Uni<EntityUser> update(final int id, final EntityUser user) {

		return pool.withTransaction(conn -> conn
			.preparedQuery(GET_ONE_BY_ID).execute(Tuple.of(id))
			.onItem().transform(RowSet::iterator)
			.onItem().transform(it -> it.hasNext() ? EntityUser.fromRow(it.next()) : null)
			.onItem().ifNull().failWith(new IllegalArgumentException("user not found"))
			.onItem().ifNotNull().transform(dbUser -> this.setUser(user, dbUser))
			.onItem().transformToUni(dbUser -> conn
				.preparedQuery(UPDATE_ONE).execute(getUpdateTuple(dbUser, id))
				.onItem().ignore().andContinueWithNull()
			)
			.onItem().ignore().andSwitchTo(() -> conn
				.preparedQuery(GET_ONE_BY_ID).execute(Tuple.of(id))
				.onItem().transform(RowSet::iterator)
				.onItem().transform(it -> it.hasNext() ? EntityUser.fromRow(it.next()) : null)
			));
	}

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

	public Uni<EntityUser> getOne(final int id) {
		return pool.preparedQuery(GET_ONE_BY_ID)
			.execute(Tuple.of(id))
			.onItem().transform(RowSet::iterator)
			.onItem().transform(i -> i.hasNext() ? EntityUser.fromRow(i.next()) : null)
			.onItem().ifNull().failWith(new IllegalArgumentException("invalid user id"));
	}


	private static Tuple getUpdateTuple(final EntityUser u, final int id) {
		return Tuple.of(u.getEmail(), u.getFirstName(), u.getLastName(), id);
	}

	private EntityUser setUser(final EntityUser user, final EntityUser dbUser) {

		dbUser.setLastName(user.getLastName());
		dbUser.setFirstName(user.getFirstName());
		dbUser.setEmail(user.getEmail());
		return dbUser;
	}
}
