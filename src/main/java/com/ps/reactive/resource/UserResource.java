package com.ps.reactive.resource;

import com.ps.reactive.model.entity.EntityUser;
import com.ps.reactive.model.request.User;
import com.ps.reactive.model.validator.UserValidationGroup;
import com.ps.reactive.repository.UserRepository;
import io.smallrye.mutiny.Uni;

import javax.validation.Valid;
import javax.validation.groups.ConvertGroup;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.stream.Collectors;

@Path("/users")
public class UserResource {

	private UserRepository userRepository;

	public UserResource(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@GET
	@Path("")
	@Produces(MediaType.APPLICATION_JSON)
	public Uni<List<EntityUser>> getAll() {
		return this.userRepository.getAll()
			.onItem().transform(i -> i.parallelStream()
				.map(EntityUser::maskPassword)
				.collect(Collectors.toList())
			);
	}

	@POST
	@Path("")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Uni<List<EntityUser>> create(final @Valid @ConvertGroup(to = UserValidationGroup.Add.class) User user) {
		return Uni.createFrom().item(user)
			.onItem().transform(User::toEntity)
			.onItem().transformToUni(this.userRepository::create)
			.onItem().transform(i -> i.parallelStream()
				.map(EntityUser::maskPassword)
				.collect(Collectors.toList())
			);
	}

	@PUT
	@Path("/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Uni<EntityUser> update(final @Valid @ConvertGroup(to = UserValidationGroup.Update.class) User user,
								  final @PathParam("id") int id) {
		return Uni.createFrom().item(user)
			.onItem().transform(User::toEntity)
			.onItem().transformToUni(u -> this.userRepository.update(id, u))
			.onItem().transform(EntityUser::maskPassword);
	}

	@DELETE
	@Path(("/{id}"))
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Uni<Void> delete(final @PathParam("id") int id) {
		return this.userRepository.delete(id);
	}

	@GET
	@Path("/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Uni<EntityUser> getOne(final @PathParam("id") int id) {
		return this.userRepository.getOne(id)
			.onItem().transform(EntityUser::maskPassword);
	}
}
