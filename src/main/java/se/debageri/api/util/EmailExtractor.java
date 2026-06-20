package se.debageri.api.util;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EmailExtractor {

	private static final Pattern EMAIL = Pattern.compile("([a-zA-Z0-9_.+\\-]+@[a-zA-Z0-9\\-]+\\.[a-zA-Z0-9\\-.]+)");

	private EmailExtractor() {
	}

	/** Returns the first email found in text. */
	public static Optional<String> findFirstEmail(String text) {
		if (text == null)
			return Optional.empty();
		Matcher m = EMAIL.matcher(text);
		if (m.find()) {
			return Optional.ofNullable(m.group(1));
		}
		return Optional.empty();
	}
}
