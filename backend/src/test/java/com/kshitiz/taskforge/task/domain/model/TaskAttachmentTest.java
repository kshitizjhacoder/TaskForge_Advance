package com.kshitiz.taskforge.task.domain.model;

import com.kshitiz.taskforge.common.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link TaskAttachment}: file-name cleaning and storage key layout.
 */
class TaskAttachmentTest {

    @ParameterizedTest(name = "''{0}'' becomes ''{1}''")
    @CsvSource({
            "report.pdf, report.pdf",
            "../../etc/passwd, passwd",
            "C:\\Users\\me\\secret.docx, secret.docx",
            "'  spaced name.txt  ', spaced name.txt",
            "folder/, file",
            ".., file"})
    void cleansFileNames(String original, String expected) {
        assertThat(TaskAttachment.cleanFileName(original)).isEqualTo(expected);
    }

    @Test
    void nullNameBecomesFile() {
        assertThat(TaskAttachment.cleanFileName(null)).isEqualTo("file");
    }

    @Test
    void storageKeyUsesOnlyIds() {
        UUID taskId = UUID.randomUUID();

        TaskAttachment attachment = TaskAttachment.upload(taskId, "../evil.txt", "text/plain", 5, UUID.randomUUID());

        assertThat(attachment.getStorageKey()).isEqualTo("tasks/" + taskId + "/" + attachment.getId());
        assertThat(attachment.getFileName()).isEqualTo("evil.txt");
    }

    @Test
    void rejectsEmptyFile() {
        assertThatThrownBy(() -> TaskAttachment.upload(UUID.randomUUID(), "a.txt", "text/plain", 0, UUID.randomUUID()))
                .isInstanceOf(BusinessRuleViolationException.class);
    }
}
