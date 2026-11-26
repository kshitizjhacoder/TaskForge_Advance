package com.kshitiz.taskforge.task.application;

import com.kshitiz.taskforge.common.exception.InvalidRequestException;
import com.kshitiz.taskforge.common.security.CurrentUser;
import com.kshitiz.taskforge.common.storage.FileStorage;
import com.kshitiz.taskforge.project.domain.model.Project;
import com.kshitiz.taskforge.task.application.dto.AttachmentResponse;
import com.kshitiz.taskforge.task.application.dto.UploadedFile;
import com.kshitiz.taskforge.task.domain.model.Task;
import com.kshitiz.taskforge.task.domain.model.TaskAttachment;
import com.kshitiz.taskforge.task.domain.port.TaskAttachmentRepository;
import com.kshitiz.taskforge.user.domain.model.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.unit.DataSize;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TaskAttachmentService}: upload validation and keeping storage and
 * database consistent when one of them fails.
 */
@ExtendWith(MockitoExtension.class)
class TaskAttachmentServiceTest {

    @Mock
    private TaskAttachmentRepository attachmentRepository;
    @Mock
    private TaskService taskService;
    @Mock
    private FileStorage fileStorage;

    private TaskAttachmentService service;
    private final CurrentUser actor = new CurrentUser(UUID.randomUUID(), "a@test.dev", Role.MEMBER);
    private final UUID projectId = UUID.randomUUID();
    private final Task task = Task.create(projectId, "Task", null, actor.id(), null, null, null, null);

    @BeforeEach
    void setUp() {
        var properties = new AttachmentProperties(DataSize.ofMegabytes(1), List.of("text/plain", "application/pdf"));
        service = new TaskAttachmentService(attachmentRepository, taskService, fileStorage, properties);
    }

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    /** Builds an upload of {@code size} bytes with the given type. */
    private static UploadedFile file(String type, long size) {
        return new UploadedFile("notes.txt", type, size, () -> new ByteArrayInputStream(new byte[(int) size]));
    }

    /** Makes the task service report the task as visible to the actor. */
    private void taskIsVisible() {
        when(taskService.loadAccessibleTask(task.getId(), actor))
                .thenReturn(new AccessibleTask(task, Project.create("TF", "P", null, UUID.randomUUID())));
    }

    @Test
    void storesFileThenSavesMetadata() {
        taskIsVisible();
        when(attachmentRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        AttachmentResponse response = service.upload(task.getId(), file("text/plain; charset=UTF-8", 5), actor);

        assertThat(response.contentType()).isEqualTo("text/plain");
        verify(fileStorage).store(eq("tasks/" + task.getId() + "/" + response.id()), any(), eq(5L), eq("text/plain"));
    }

    @Test
    void rejectsDisallowedType() {
        taskIsVisible();

        assertThatThrownBy(() -> service.upload(task.getId(), file("application/x-msdownload", 5), actor))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("not allowed");
        verifyNoInteractions(fileStorage);
    }

    @Test
    void rejectsTooLargeFile() {
        taskIsVisible();

        assertThatThrownBy(() -> service.upload(task.getId(), file("text/plain", 2 * 1024 * 1024), actor))
                .isInstanceOf(InvalidRequestException.class);
        verifyNoInteractions(fileStorage);
    }

    @Test
    void deletesStoredFileIfMetadataSaveFails() {
        taskIsVisible();
        when(attachmentRepository.save(any())).thenThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> service.upload(task.getId(), file("text/plain", 5), actor))
                .isInstanceOf(IllegalStateException.class);
        verify(fileStorage).store(anyString(), any(), anyLong(), anyString());
        verify(fileStorage).delete(anyString());
    }

    @Test
    void deleteRemovesStoredFileOnlyAfterCommit() {
        TaskAttachment attachment = TaskAttachment.upload(task.getId(), "a.txt", "text/plain", 5, actor.id());
        when(attachmentRepository.findById(attachment.getId())).thenReturn(Optional.of(attachment));
        taskIsVisible();
        TransactionSynchronizationManager.initSynchronization();

        service.deleteAttachment(attachment.getId(), actor);

        verify(attachmentRepository).deleteById(attachment.getId());
        verify(fileStorage, never()).delete(anyString());

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(fileStorage).delete(attachment.getStorageKey());
    }
}
