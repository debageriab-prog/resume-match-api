package se.debageri.api.util;

import java.io.IOException;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.web.multipart.MultipartFile;

public class StringUtil {

	private StringUtil() {
	}

	public static String extractTextFromPDF(MultipartFile pdf) {
		try {
			byte[] bytes = pdf.getBytes();

			try (PDDocument doc = Loader.loadPDF(bytes)) {
				PDFTextStripper stripper = new PDFTextStripper();
				String text = stripper.getText(doc);
				return text == null ? "" : text.replace("\u0000", "").trim();
			}

		} catch (IOException e) {
			throw new RuntimeException("Failed to read PDF: " + e.getMessage(), e);
		}
	}

	public static String nvl(Object o) {
		return o == null ? "" : String.valueOf(o);
	}

	public static String join(List<String> xs) {
		return xs == null ? "" : String.join(", ", xs);
	}

	public static String safeLimit(String s, int maxChars) {
		if (s == null)
			return "";
		if (s.length() <= maxChars)
			return s;
		return s.substring(0, maxChars);
	}

	public static boolean isBlank(String s) {
		return s == null || s.trim().isEmpty();
	}

	/**
	 * Remove leading code-fence like ```json (case-insensitive) or ``` and a
	 * trailing ``` if present. Trims surrounding whitespace.
	 */
	public static String stripJsonCodeFences(String raw) {
		if (raw == null)
			return null;
		String s = raw.strip();
		s = s.replaceFirst("(?i)^\\s*```(?:json)?[ \\t]*\\r?\\n?", "");
		s = s.replaceFirst("\\r?\\n?\\s*```\\s*$", "");
		return s;
	}
}
