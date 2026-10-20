package com.masar.backend.service;

import com.masar.backend.exception.InvalidFileException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Checks an uploaded CV before it is stored: not empty, at most 5MB, declared as a PDF,
 * and the content really starts like a PDF (the content type comes from the client,
 * so on its own it cannot be trusted).
 */
@Component
public class PdfValidator {

    public static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;
    public static final String PDF_CONTENT_TYPE = "application/pdf";

    private static final byte[] PDF_MAGIC = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Please choose a PDF file");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new InvalidFileException("The file is too large, the maximum is 5MB");
        }
        if (!PDF_CONTENT_TYPE.equalsIgnoreCase(file.getContentType())) {
            throw new InvalidFileException("Only PDF files are allowed");
        }
        if (!startsWithPdfMagic(file)) {
            throw new InvalidFileException("The file is not a valid PDF");
        }
    }

    private boolean startsWithPdfMagic(MultipartFile file) {
        try (var in = file.getInputStream()) {
            byte[] head = in.readNBytes(PDF_MAGIC.length);
            return Arrays.equals(head, PDF_MAGIC);
        } catch (IOException e) {
            throw new InvalidFileException("The file could not be read");
        }
    }
}
