package se.debageri.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class AssignmentNotFoundException extends RuntimeException {

	public AssignmentNotFoundException(long id) {
		super("Assignment not found with id: " + id);
	}
}
