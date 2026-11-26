package com.kshitiz.taskforge.task.adapter.rest;

import com.kshitiz.taskforge.common.security.CurrentUser;
import com.kshitiz.taskforge.task.application.TaskAttachmentService;
import com.kshitiz.taskforge.task.application.dto.AttachmentDownload;
import com.kshitiz.taskforge.task.application.dto.AttachmentResponse;
import com.kshitiz.taskforge.task.application.dto.UploadedFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * REST endpoints for task attachments. Uploads are {@code multipart/form-data} with a
 * {@code file} part; downloads are streamed through the API after a permission check.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Task attachments")
public class TaskAttachmentController {

    private final TaskAttachmentService attachmentService;

    /**
     * Uploads a file to a task.
     *
     * @param taskId task ID
     * @param file   the {@code file} part of the multipart request
     * @param actor  the logged-in user (recorded as uploader)
     * @return 201 Created with the attachment's metadata (400 bad type, 413 too large)
     */
    @PostMapping(path = "/api/tasks/{taskId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Attach a file to a task")
    public ResponseEntity<AttachmentResponse> upload(@PathVariable UUID taskId,
                                                     @RequestPart("file") MultipartFile file,
                                                     @AuthenticationPrincipal CurrentUser actor) {
        UploadedFile upload = new UploadedFile(file.getOriginalFilename(), file.getContentType(), file.getSize(),
                file::getInputStream);
        return ResponseEntity.status(HttpStatus.CREATED).body(attachmentService.upload(taskId, upload, actor));
    }

    /**
     * Lists a task's attachments.
     *
     * @param taskId task ID
     * @param actor  the logged-in user
     * @return attachments
     */
    @GetMapping("/api/tasks/{taskId}/attachments")
    @Operation(summary = "List a task's attachments")
    public List<AttachmentResponse> listAttachments(@PathVariable UUID taskId,
                                                    @AuthenticationPrincipal CurrentUser actor) {
        return attachmentService.listAttachments(taskId, actor);
    }

    /**
     * Downloads an attachment. Always sent as a download ("attachment" disposition) so the
     * browser never renders an uploaded file inside the app's own origin.
     *
     * @param id    attachment ID
     * @param actor the logged-in user
     * @return the file's bytes with name, type and length headers
     */
    @GetMapping("/api/attachments/{id}/content")
    @Operation(summary = "Download an attachment")
    public ResponseEntity<InputStreamResource> download(@PathVariable UUID id,
                                                        @AuthenticationPrincipal CurrentUser actor) {
        AttachmentDownload download = attachmentService.download(id, actor);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(download.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(download.contentType()))
                .contentLength(download.sizeBytes())
                .body(new InputStreamResource(download.content()));
    }

    /**
     * Deletes an attachment.
     *
     * @param id    attachment ID
     * @param actor the logged-in user
     * @return 204 No Content (403 if not allowed)
     */
    @DeleteMapping("/api/attachments/{id}")
    @Operation(summary = "Delete an attachment")
    public ResponseEntity<Void> deleteAttachment(@PathVariable UUID id, @AuthenticationPrincipal CurrentUser actor) {
        attachmentService.deleteAttachment(id, actor);
        return ResponseEntity.noContent().build();
    }
}
