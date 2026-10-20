package com.masar.backend.dto.cv;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** A temporary link to read the CV. It stops working after expiresInSeconds. */
@Getter
@AllArgsConstructor
public class CvLinkResponse {
    private String url;
    private long expiresInSeconds;
}