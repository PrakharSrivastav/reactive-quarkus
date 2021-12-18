package com.ps.reactive.model.validator;

import javax.validation.groups.Default;

public interface UserValidationGroup {
	interface Add extends Default{}
	interface Update extends Default{}
}
