package com.masar.backend.service;

import com.masar.backend.dto.cv.CvLinkResponse;
import com.masar.backend.enums.Role;
import com.masar.backend.exception.InvalidFileException;
import com.masar.backend.exception.ResourceNotFoundException;
import com.masar.backend.exception.StorageException;
import com.masar.backend.model.User;
import com.masar.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CvServiceTest {

    private static final String EMAIL = "ali@mail.com";

    @Mock private UserRepository userRepository;
    @Mock private StorageService storageService;
    @Mock private PdfValidator pdfValidator;

    @InjectMocks
    private CvService cvService;

    private User user;
    private MockMultipartFile file;

    @BeforeEach
    void setUp() {
        user = User.builder().id("u-1").email(EMAIL).fullName("Ali").role(Role.JOB_SEEKER).verified(true).build();
        file = new MockMultipartFile("file", "ali-cv.pdf", "application/pdf",
                "%PDF-1.7 body".getBytes(StandardCharsets.US_ASCII));
    }

    @Nested
    @DisplayName("Upload")
    class Upload {

        @Test
        @DisplayName("first CV: file goes to storage under the user's folder and the path is saved")
        void first() throws Exception {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

            cvService.upload(EMAIL, file);

            ArgumentCaptor<String> path = ArgumentCaptor.forClass(String.class);
            verify(storageService).upload(path.capture(), eq(file.getBytes()), eq("application/pdf"));
            assertThat(path.getValue()).startsWith("u-1/").endsWith(".pdf");
            verify(userRepository).updateCvPath("u-1", path.getValue());
            verify(storageService, never()).delete(anyString());
        }

        @Test
        @DisplayName("replacing: upload new, save the path, then delete the old file (in that order)")
        void replace() {
            user.setCvPath("u-1/old.pdf");
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

            cvService.upload(EMAIL, file);

            InOrder order = inOrder(storageService, userRepository);
            order.verify(storageService).upload(anyString(), any(), anyString());
            order.verify(userRepository).updateCvPath(eq("u-1"), anyString());
            order.verify(storageService).delete("u-1/old.pdf");
        }

        @Test
        @DisplayName("saving the path fails: the new file is removed and the old CV stays")
        void saveFails() {
            user.setCvPath("u-1/old.pdf");
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(userRepository.updateCvPath(eq("u-1"), anyString())).thenThrow(new IllegalStateException("db down"));

            assertThatThrownBy(() -> cvService.upload(EMAIL, file)).isInstanceOf(IllegalStateException.class);

            ArgumentCaptor<String> uploaded = ArgumentCaptor.forClass(String.class);
            verify(storageService).upload(uploaded.capture(), any(), anyString());
            verify(storageService).delete(uploaded.getValue());          // new file cleaned up
            verify(storageService, never()).delete("u-1/old.pdf");       // old one untouched
        }

        @Test
        @DisplayName("failing to delete the old file does not fail the upload")
        void oldFileDeleteFails() {
            user.setCvPath("u-1/old.pdf");
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            doThrow(new StorageException()).when(storageService).delete("u-1/old.pdf");

            assertThatCode(() -> cvService.upload(EMAIL, file)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("invalid file: nothing is stored or saved")
        void invalidFile() {
            doThrow(new InvalidFileException("Only PDF files are allowed")).when(pdfValidator).validate(file);

            assertThatThrownBy(() -> cvService.upload(EMAIL, file)).isInstanceOf(InvalidFileException.class);

            verifyNoInteractions(storageService, userRepository);
        }

        @Test
        @DisplayName("storage failure: the path is not saved")
        void storageFails() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            doThrow(new StorageException()).when(storageService).upload(anyString(), any(), anyString());

            assertThatThrownBy(() -> cvService.upload(EMAIL, file)).isInstanceOf(StorageException.class);

            verify(userRepository, never()).updateCvPath(any(), any());
        }
    }

    @Nested
    @DisplayName("Link")
    class Link {

        @Test
        @DisplayName("returns a signed link that lives 5 minutes")
        void success() {
            user.setCvPath("u-1/cv.pdf");
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(storageService.createSignedUrl("u-1/cv.pdf", Duration.ofMinutes(5))).thenReturn("https://signed");

            CvLinkResponse response = cvService.createLink(EMAIL);

            assertThat(response.getUrl()).isEqualTo("https://signed");
            assertThat(response.getExpiresInSeconds()).isEqualTo(300);
        }

        @Test
        @DisplayName("no CV uploaded yet: not found")
        void noCv() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> cvService.createLink(EMAIL)).isInstanceOf(ResourceNotFoundException.class);
            verifyNoInteractions(storageService);
        }
    }

    @Nested
    @DisplayName("Delete")
    class Delete {

        @Test
        @DisplayName("deletes the file first, then clears the path")
        void success() {
            user.setCvPath("u-1/cv.pdf");
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

            cvService.delete(EMAIL);

            InOrder order = inOrder(storageService, userRepository);
            order.verify(storageService).delete("u-1/cv.pdf");
            order.verify(userRepository).updateCvPath("u-1", null);
        }

        @Test
        @DisplayName("storage failure keeps the path so the user can try again")
        void storageFails() {
            user.setCvPath("u-1/cv.pdf");
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            doThrow(new StorageException()).when(storageService).delete("u-1/cv.pdf");

            assertThatThrownBy(() -> cvService.delete(EMAIL)).isInstanceOf(StorageException.class);

            verify(userRepository, never()).updateCvPath(any(), any());
        }

        @Test
        @DisplayName("no CV uploaded yet: not found")
        void noCv() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> cvService.delete(EMAIL)).isInstanceOf(ResourceNotFoundException.class);
            verifyNoInteractions(storageService);
        }
    }
}
