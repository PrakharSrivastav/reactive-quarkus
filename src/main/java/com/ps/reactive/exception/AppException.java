package com.ps.reactive.exception;

import lombok.Data;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.io.Serializable;

public class AppException {

	@Data
	private static class ExceptionDescription implements Serializable {
		private final String message;
		private final String status;

		ExceptionDescription(Exception ex, Response.Status status) {
			this.message = ex.getMessage();
			this.status = status.getReasonPhrase();
		}
	}

	@ServerExceptionMapper(NotFoundException.class)
	public Response handleNotFoundException(NotFoundException ex) {
		return Response.status(Response.Status.NOT_FOUND)
			.entity(new ExceptionDescription(ex, Response.Status.NOT_FOUND))
			.build();
	}

}
