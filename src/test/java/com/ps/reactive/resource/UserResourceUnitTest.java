package com.ps.reactive.resource;

import com.ps.reactive.model.entity.EntityUser;
import com.ps.reactive.model.request.User;
import com.ps.reactive.repository.UserRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.inject.Inject;
import javax.validation.ValidationException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@QuarkusTest
class UserResourceUnitTest {

	@Inject
	UserResource userResource;

	@InjectMock
	UserRepository userRepository;

	private List<EntityUser> list;

	@BeforeEach
	void setUp() {

		list = new ArrayList<>();
		list.add(new EntityUser(1, "a", "b", "c", "d"));
		list.add(new EntityUser(2, "aa", "bb", "cc", "dd"));
		list.add(new EntityUser(3, "aaa", "bbb", "ccc", "ddd"));
		list.add(new EntityUser(4, "aaaa", "bbbb", "cccc", "dddd"));

		Uni<List<EntityUser>> users = Uni.createFrom().item(list);

		Mockito.when(userRepository.getAll()).thenReturn(users);
		Mockito.when(userRepository.create(Mockito.any(EntityUser.class))).thenReturn(users);
		Mockito.when(userRepository.getOne(1)).thenReturn(Uni.createFrom().item(list.get(0)));
	}

	@Test
	@DisplayName("get all users")
	void getAll() {
		var all = userResource.getAll();
		Assertions.assertNotNull(all);

		all.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertTerminated()
			.assertCompleted()
			.assertItem(list)
			.getItem()
			.forEach(i -> Assertions.assertTrue(list.contains(i)));

		Mockito.verify(this.userRepository, Mockito.times(1)).getAll();
	}

	@Test
	@DisplayName("create new user")
	void create() {
		var u = new User(0, "aaaaa", "bbbbb", "cccccc@c.com", "ddddd");
		Uni<List<EntityUser>> listUni = userResource.create(u);

		listUni.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertTerminated()
			.assertCompleted()
			.assertItem(this.list)
			.getItem()
			.forEach(i -> Assertions.assertTrue(list.contains(i)));

		Mockito.verify(this.userRepository, Mockito.times(1)).create(u.toEntity());
	}

	@Test
	@DisplayName("create new user : bad email address error")
	void create_bad_email() {
		var ex = Assertions.assertThrows(ValidationException.class, () -> {
			var u = new User(0, "aaaaa", "bbbbb", "cccccc", "ddddd");
			Uni<List<EntityUser>> listUni = userResource.create(u);
		});
		Assertions.assertEquals("create.user.email: Email address is invalid", ex.getMessage());
	}

	@Test
	@DisplayName("create new user : missing first name error")
	void create_missing_first_name() {
		var ex = Assertions.assertThrows(ValidationException.class, () -> {
			var u = new User(0, "", "bbbbb", "cccccc@c.com", "ddddd");
			Uni<List<EntityUser>> listUni = userResource.create(u);
		});
		Assertions.assertEquals("create.user.firstName: First Name is required", ex.getMessage());
	}

	@Test
	@DisplayName("update user")
	void update() {
		var id = 3;
		var user = new User(3, "fname", "lname", "email@test.com", "a");
		var maskerUser = EntityUser.maskPassword(user.toEntity());

		Mockito
			.when(userRepository.update(3, user.toEntity()))
			.thenReturn(Uni.createFrom().item(maskerUser));

		var uni = userResource.update(user, id);

		Assertions.assertNotNull(uni);

		var actual = uni.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertCompleted()
			.assertTerminated()
			.assertItem(maskerUser)
			.getItem();

		Assertions.assertEquals(user.getEmail(), actual.getEmail());
		Assertions.assertEquals(user.getLastName(), actual.getLastName());
		Assertions.assertEquals(user.getFirstName(), actual.getFirstName());
		Assertions.assertEquals("XXXX", actual.getPassword());

		Mockito.verify(userRepository, Mockito.times(1))
			.update(id, user.toEntity());
	}

	@Test
	void delete() {
		Uni<Void> voidUni = userResource.delete(1);
		Assertions.assertNull(voidUni);

		Mockito.when(userRepository.delete(1)).thenReturn(Uni.createFrom().nullItem());

		Mockito.verify(userRepository, Mockito.times(1)).delete(1);
	}

	@Test
	void getOne() {

		Uni<EntityUser> uni = userResource.getOne(1);

		Assertions.assertNotNull(uni);

		EntityUser item = uni.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertCompleted()
			.assertTerminated()
			.assertItem(list.get(0))
			.getItem();

		Assertions.assertEquals(list.get(0).getEmail(), item.getEmail());
		Assertions.assertEquals(list.get(0).getLastName(), item.getLastName());
		Assertions.assertEquals(list.get(0).getFirstName(), item.getFirstName());
		Assertions.assertEquals(list.get(0).getId(), item.getId());
		Assertions.assertEquals(list.get(0).getPassword(), item.getPassword());
		Assertions.assertEquals("XXXX", item.getPassword());

		Mockito.verify(userRepository, Mockito.times(1)).getOne(1);
	}
}