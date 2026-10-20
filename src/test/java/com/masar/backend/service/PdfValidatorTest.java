package com.masar.backend.service;

import com.masar.backend.exception.InvalidFileException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfValidatorTest {

    private final PdfValidator validator = new PdfValidator();

    private static MockMultipartFile file(String contentType, byte[] content) {
        return new MockMultipartFile("file", "cv.pdf", contentType, content);
    }

    private static byte[] pdf() {
        return "%PDF-1.7 fake body".getBytes(StandardCharsets.US_ASCII);
    }

    @Test
    @DisplayName("a real PDF passes")
    void valid() {
        assertThatCode(() -> validator.validate(file("application/pdf", pdf()))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("no file or an empty file is rejected")
    void empty() {
        assertThatThrownBy(() -> validator.validate(null)).isInstanceOf(InvalidFileException.class);
        assertThatThrownBy(() -> validator.validate(file("application/pdf", new byte[0])))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    @DisplayName("a file over 5MB is rejected")
    void tooLarge() {
        byte[] big = new byte[(int) PdfValidator.MAX_SIZE_BYTES + 1];
        System.arraycopy(pdf(), 0, big, 0, pdf().length);

        assertThatThrownBy(() -> validator.validate(file("application/pdf", big)))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("5MB");
    }

    @Test
    @DisplayName("another content type is rejected")
    void wrongContentType() {
        assertThatThrownBy(() -> validator.validate(file("image/png", pdf())))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    @DisplayName("a file called PDF whose content is not a PDF is rejected")
    void fakePdf() {
        byte[] notPdf = "<html>hello</html>".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> validator.validate(file("application/pdf", notPdf)))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("not a valid PDF");
    }
}
