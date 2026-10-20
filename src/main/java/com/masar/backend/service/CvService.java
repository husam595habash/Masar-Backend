package com.masar.backend.service;

import com.masar.backend.dto.cv.CvLinkResponse;
import com.masar.backend.exception.InvalidFileException;
import com.masar.backend.exception.ResourceNotFoundException;
import com.masar.backend.exception.StorageException;
import com.masar.backend.model.User;
import com.masar.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

/**
 * Upload, read and delete the CV of the logged-in user. Each user has one CV: uploading
 * a new one replaces the old one. The PDF goes to the storage and only its path is kept,
 * in User.cvPath.
 */
@Service
public class CvService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CvService.class);

    /** How long a read link works. */
    private static final Duration LINK_TTL = Duration.ofMinutes(5);

    private final UserRepository userRepository;
    private final StorageService storageService;
    private final PdfValidator pdfValidator;

    public CvService(UserRepository userRepository,
                     StorageService storageService,
                     PdfValidator pdfValidator) {
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.pdfValidator = pdfValidator;
    }

    /**
     * Stores the file and saves its path. Order: upload the new file, save the path, and
     * only then delete the old file. If saving fails the new file is removed, so the user
     * keeps the old CV and no file is left behind.
     */
    public void upload(String email, MultipartFile file) {
        pdfValidator.validate(file);
        User user = findUser(email);
        String oldPath = user.getCvPath();   // the update below changes the database, not this object
        byte[] content = readBytes(file); // return the file as bytes array

        String newPath = user.getId() + "/" + UUID.randomUUID() + ".pdf";
        storageService.upload(newPath, content, PdfValidator.PDF_CONTENT_TYPE);

        try {
            userRepository.updateCvPath(user.getId(), newPath);
        } catch (RuntimeException e) {
            deleteQuietly(newPath);   // the path was not saved, so do not leave the new file orphaned
            throw e;
        }

        if (oldPath != null) {
            deleteQuietly(oldPath);
        }
    }

    /** A temporary link to open the CV. */
    public CvLinkResponse createLink(String email) {
        String path = requireCvPath(findUser(email));
        String url = storageService.createSignedUrl(path, LINK_TTL);
        return new CvLinkResponse(url, LINK_TTL.toSeconds());
    }

    /**
     * Deletes the file first, then the path. If the storage fails the path stays, so the
     * user can try again and the file is never lost track of.
     */
    public void delete(String email) {
        User user = findUser(email);
        String path = requireCvPath(user);
        storageService.delete(path);
        userRepository.updateCvPath(user.getId(), null);
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private String requireCvPath(User user) {
        if (user.getCvPath() == null) {
            throw new ResourceNotFoundException("You have not uploaded a CV yet");
        }
        return user.getCvPath();
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new InvalidFileException("The file could not be read");
        }
    }

    /** Cleanup that must not hide the real result: a failure is only logged. */
    private void deleteQuietly(String path) {
        try {
            storageService.delete(path);
        } catch (StorageException e) {
            log.warn("Could not delete the file {} from storage, it stays orphaned", path);
        }
    }
}
