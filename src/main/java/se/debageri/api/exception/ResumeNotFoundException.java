package se.debageri.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResumeNotFoundException extends RuntimeException {

	public ResumeNotFoundException(long id) {
		super("Resume not found with id: " + id);
	}
}
