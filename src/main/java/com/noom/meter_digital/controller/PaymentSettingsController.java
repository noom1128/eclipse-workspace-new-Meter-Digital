package com.noom.meter_digital.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noom.meter_digital.dto.PropertyProfile;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/settings")
public class PaymentSettingsController {
    private static final Path QR_DIRECTORY = Path.of("uploads").toAbsolutePath().normalize();
    private static final String PNG_FILE = "promptpay-qr.png";
    private static final String JPEG_FILE = "promptpay-qr.jpg";
    private static final Path PROPERTY_PROFILE_FILE = QR_DIRECTORY.resolve("property-profile.json");
    private final ObjectMapper objectMapper;

    public PaymentSettingsController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @GetMapping("/property-profile")
    public PropertyProfile getPropertyProfile() {
        try {
            return Files.exists(PROPERTY_PROFILE_FILE)
                    ? objectMapper.readValue(PROPERTY_PROFILE_FILE.toFile(), PropertyProfile.class)
                    : new PropertyProfile();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not read property profile", e);
        }
    }

    @PostMapping("/property-profile")
    public PropertyProfile savePropertyProfile(@RequestBody PropertyProfile profile) {
        if (blank(profile.getName()) || blank(profile.getAddress()) || blank(profile.getPhone())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name, address, and phone are required");
        }
        try {
            Files.createDirectories(QR_DIRECTORY);
            objectMapper.writeValue(PROPERTY_PROFILE_FILE.toFile(), profile);
            return profile;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not save property profile", e);
        }
    }

    @PostMapping(value = "/promptpay-qr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> uploadPromptPayQr(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please upload an image smaller than 5 MB");
        }
        String contentType = file.getContentType();
        String filename;
        if (MediaType.IMAGE_PNG_VALUE.equals(contentType)) filename = PNG_FILE;
        else if (MediaType.IMAGE_JPEG_VALUE.equals(contentType)) filename = JPEG_FILE;
        else throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PNG and JPEG images are supported");

        try {
            Files.createDirectories(QR_DIRECTORY);
            Files.deleteIfExists(QR_DIRECTORY.resolve(PNG_FILE));
            Files.deleteIfExists(QR_DIRECTORY.resolve(JPEG_FILE));
            Files.copy(file.getInputStream(), QR_DIRECTORY.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            return Map.of("url", "/api/settings/promptpay-qr?v=" + System.currentTimeMillis());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not save the QR image", e);
        }
    }

    @GetMapping("/promptpay-qr")
    public ResponseEntity<Resource> getPromptPayQr() {
        Path qr = Files.exists(QR_DIRECTORY.resolve(PNG_FILE))
                ? QR_DIRECTORY.resolve(PNG_FILE) : QR_DIRECTORY.resolve(JPEG_FILE);
        if (!Files.exists(qr)) return ResponseEntity.notFound().build();
        MediaType contentType = qr.getFileName().toString().endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(contentType)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(new FileSystemResource(qr));
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
}
