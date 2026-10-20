package com.masar.backend.controller;

import com.masar.backend.dto.cv.CvLinkResponse;
import com.masar.backend.service.CvService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * The CV of the logged-in job seeker. All endpoints need a valid JWT and the
 * JOB_SEEKER role, and they only ever touch the caller's own CV (the owner comes
 * from the token, never from the request).
 */
@RestController
@RequestMapping("/api/cvs")
@PreAuthorize("hasRole('JOB_SEEKER')")
public class CvController {

    private final CvService cvService;

    public CvController(CvService cvService) {
        this.cvService = cvService;
    }

    /**
     * POST /api/cvs (multipart, field "file"): uploads a PDF, replacing the old CV. Returns 201.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> upload(Authentication auth, @RequestPart("file") MultipartFile file) {
        cvService.upload(auth.getName(), file);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * GET /api/cvs/me/link: a temporary link to open my CV.
     */
    @GetMapping("/me/link")
    public ResponseEntity<CvLinkResponse> link(Authentication auth) {
        return ResponseEntity.ok(cvService.createLink(auth.getName()));
    }

    /**
     * DELETE /api/cvs/me: deletes my CV. Returns 204.
     */
    @DeleteMapping("/me")
    public ResponseEntity<Void> delete(Authentication auth) {
        cvService.delete(auth.getName());
        return ResponseEntity.noContent().build();
    }
}
