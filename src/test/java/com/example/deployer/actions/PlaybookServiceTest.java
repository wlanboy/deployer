package com.example.deployer.actions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

class PlaybookServiceTest {

    private final PlaybookService service = new PlaybookService();

    // -------------------------------------------------------------------------
    // requiresBecome
    // -------------------------------------------------------------------------

    @Test
    void requiresBecome_playLevelBecomeTrue_returnsTrue(@TempDir Path tmp) throws IOException {
        Path playbook = tmp.resolve("site.yml");
        Files.writeString(playbook, """
                - name: My play
                  become: true
                  tasks: []
                """);
        assertThat(service.requiresBecome(playbook.toString())).isTrue();
    }

    @Test
    void requiresBecome_taskLevelBecomeTrue_returnsTrue(@TempDir Path tmp) throws IOException {
        Path playbook = tmp.resolve("site.yml");
        Files.writeString(playbook, """
                - name: My play
                  tasks:
                    - name: root task
                      become: true
                      command: whoami
                """);
        assertThat(service.requiresBecome(playbook.toString())).isTrue();
    }

    @Test
    void requiresBecome_noBecome_returnsFalse(@TempDir Path tmp) throws IOException {
        Path playbook = tmp.resolve("site.yml");
        Files.writeString(playbook, """
                - name: My play
                  tasks:
                    - name: regular task
                      command: echo hello
                """);
        assertThat(service.requiresBecome(playbook.toString())).isFalse();
    }

    @Test
    void requiresBecome_fileNotFound_returnsFalse() {
        assertThat(service.requiresBecome("/nonexistent/path/playbook.yml")).isFalse();
    }

    @Test
    void requiresBecome_emptyFile_returnsFalse(@TempDir Path tmp) throws IOException {
        Path playbook = tmp.resolve("empty.yml");
        Files.writeString(playbook, "");
        assertThat(service.requiresBecome(playbook.toString())).isFalse();
    }

    // -------------------------------------------------------------------------
    // parseStats
    // -------------------------------------------------------------------------

    @Test
    void parseStats_validRecapLine_parsesAllValues() {
        String output = "localhost : ok=5 changed=2 unreachable=0 failed=1 skipped=3";
        Map<String, Integer> stats = service.parseStats(output);
        assertThat(stats.get("ok")).isEqualTo(5);
        assertThat(stats.get("changed")).isEqualTo(2);
        assertThat(stats.get("failed")).isEqualTo(1);
        assertThat(stats.get("skipped")).isEqualTo(3);
    }

    @Test
    void parseStats_emptyOutput_returnsAllZeros() {
        Map<String, Integer> stats = service.parseStats("");
        assertThat(stats.get("ok")).isZero();
        assertThat(stats.get("changed")).isZero();
        assertThat(stats.get("failed")).isZero();
        assertThat(stats.get("skipped")).isZero();
    }

    @Test
    void parseStats_noRecapLine_returnsAllZeros() {
        Map<String, Integer> stats = service.parseStats("PLAY [all] ****\nTASK [Gathering Facts]");
        assertThat(stats.get("ok")).isZero();
    }

    // -------------------------------------------------------------------------
    // extractHosts
    // -------------------------------------------------------------------------

    @Test
    void extractHosts_validRecapLine_extractsHostName() {
        String output = "localhost                  : ok=2    changed=1    unreachable=0    failed=0    skipped=0";
        assertThat(service.extractHosts(output)).contains("localhost");
    }

    @Test
    void extractHosts_multipleHosts_extractsAll() {
        String output = """
                host1   : ok=1 changed=0 unreachable=0 failed=0 skipped=0
                host2   : ok=3 changed=2 unreachable=0 failed=0 skipped=0
                """;
        assertThat(service.extractHosts(output)).containsExactlyInAnyOrder("host1", "host2");
    }

    @Test
    void extractHosts_noRecapLines_returnsEmptyList() {
        assertThat(service.extractHosts("PLAY RECAP ***")).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Input validation (via runPlaybookStreamed)
    // Validation runs before ProcessBuilder.start(), so IllegalArgumentException
    // is thrown without requiring ansible-playbook to be installed.
    // -------------------------------------------------------------------------

    @Test
    void runPlaybookStreamed_semicolonInTags_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
            service.runPlaybookStreamed("/p.yml", "/i.yml", "tag1;rm -rf /", null, null, null,
                    new ResponseBodyEmitter()));
    }

    @Test
    void runPlaybookStreamed_shellMetacharInTags_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
            service.runPlaybookStreamed("/p.yml", "/i.yml", "tag$(evil)", null, null, null,
                    new ResponseBodyEmitter()));
    }

    @Test
    void runPlaybookStreamed_dashPrefixInSkipTags_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
            service.runPlaybookStreamed("/p.yml", "/i.yml", null, "--inject", null, null,
                    new ResponseBodyEmitter()));
    }

    @Test
    void runPlaybookStreamed_leadingDashInHostLimit_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
            service.runPlaybookStreamed("/p.yml", "/i.yml", null, null, "-dangerousOption", null,
                    new ResponseBodyEmitter()));
    }

    @Test
    void runPlaybookStreamed_shellMetacharInHostLimit_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
            service.runPlaybookStreamed("/p.yml", "/i.yml", null, null, "host$(cmd)", null,
                    new ResponseBodyEmitter()));
    }

    @Test
    void runPlaybookStreamed_validParams_validationPasses() throws Exception {
        // Valid params must not throw IllegalArgumentException.
        // IOException is expected when ansible-playbook is not installed – that is fine.
        try {
            service.runPlaybookStreamed("/p.yml", "/i.yml", "tag1,tag2", "skip-me", "host1,host2",
                    null, new ResponseBodyEmitter());
        } catch (IllegalArgumentException e) {
            fail("Valid params should not throw IllegalArgumentException: " + e.getMessage());
        } catch (IOException e) {
            // ansible-playbook not installed in test environment – acceptable
        }
    }

    @Test
    void runPlaybookStreamed_nullParams_validationPasses() throws Exception {
        // null tags / skipTags / hostLimit must be accepted
        try {
            service.runPlaybookStreamed("/p.yml", "/i.yml", null, null, null, null,
                    new ResponseBodyEmitter());
        } catch (IllegalArgumentException e) {
            fail("Null params should not throw IllegalArgumentException: " + e.getMessage());
        } catch (IOException e) {
            // ansible-playbook not installed – acceptable
        }
    }
}
