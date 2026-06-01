package com.akash.credential_verification.Controller;

import com.akash.credential_verification.DTO.FileUploadResponse;
import com.akash.credential_verification.Service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.io.IOException;
import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;


@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "Files", description = "Upload and preview files")
public class FileController {

    private final FileService fileService;

    @Operation(
            summary = "Upload a file",
            description = "Uploads a file to IPFS and stores metadata in MongoDB",
            requestBody = @RequestBody(
                    content = @Content(
                            mediaType = "multipart/form-data",
                            schema = @Schema(implementation = FileUploadRequest.class)
                    )
            )
    )
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(
            @RequestPart("file") MultipartFile file
    ) {
        try {
            FileUploadResponse response = fileService.upload(file);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        } catch (IOException exception) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to upload file"));
        }
    }

    @Operation(summary = "Preview a file", description = "Retrieve a file from IPFS by CID")
    @GetMapping("/{cid}/preview")
    public ResponseEntity<?> preview(@PathVariable String cid) {
        try {
            FileService.PreviewFile previewFile = fileService.getPreviewFile(cid);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(previewFile.contentType()))
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + previewFile.fileName() + "\""
                    )
                    .body(previewFile.resource());
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.notFound().build();
        } catch (IOException exception) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to read file"));
        }
    }

    @ExceptionHandler({MultipartException.class, MissingServletRequestPartException.class})
    public ResponseEntity<Map<String, String>> handleMultipartErrors(Exception exception) {
        return ResponseEntity.badRequest().body(
                Map.of("error", "Send the request as multipart/form-data with a file part named 'file'")
        );
    }

    private static class FileUploadRequest {
        @Schema(type = "string", format = "binary")
        public MultipartFile file;
    }
}
