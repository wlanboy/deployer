package com.example.deployer.actions;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.yaml.snakeyaml.Yaml;

@Service
public class PlaybookService {

    private static final Logger log = LoggerFactory.getLogger(PlaybookService.class);

    // Tags: comma-separated Ansible tag names (letters, digits, hyphens, underscores)
    private static final Pattern SAFE_TAG_PATTERN = Pattern.compile("^[\\w,\\- ]*$");
    // Host limit: Ansible inventory patterns (alphanumeric, hyphens, dots, colons,
    // wildcards, negation, intersection, commas – but never starts with a dash)
    private static final Pattern SAFE_HOST_PATTERN = Pattern.compile("^[\\w,.:\\-*!&@ ]*$");

    private void validateTags(String value, String name) {
        if (value != null && !value.isBlank()) {
            if (value.stripLeading().startsWith("-")) {
                throw new IllegalArgumentException("Ungültiger Wert für '" + name + "': darf nicht mit einem Bindestrich beginnen");
            }
            if (!SAFE_TAG_PATTERN.matcher(value).matches()) {
                throw new IllegalArgumentException("Ungültiger Wert für '" + name + "': nur Buchstaben, Ziffern, Kommas und Bindestriche erlaubt");
            }
        }
    }

    private void validateHostLimit(String value) {
        if (value != null && !value.isBlank()) {
            if (value.startsWith("-")) {
                throw new IllegalArgumentException("'hostLimit' darf nicht mit einem Bindestrich beginnen");
            }
            if (!SAFE_HOST_PATTERN.matcher(value).matches()) {
                throw new IllegalArgumentException("Ungültiger Wert für 'hostLimit': unerlaubte Zeichen");
            }
        }
    }

    public boolean requiresBecome(String playbookPath) {
        try (FileReader reader = new FileReader(playbookPath)) {
            Yaml yaml = new Yaml();
            List<Map<String, Object>> plays = yaml.load(reader);
            if (plays == null) return false;
            for (Map<String, Object> play : plays) {
                if (Boolean.TRUE.equals(play.get("become"))) return true;
                Object tasks = play.get("tasks");
                if (tasks instanceof List<?> taskList) {
                    for (Object task : taskList) {
                        if (task instanceof Map<?, ?> taskMap && Boolean.TRUE.equals(taskMap.get("become"))) return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            log.warn("Fehler beim Lesen von Playbook '{}': {}", playbookPath, e.getMessage());
            return false;
        }
    }

    public Map<String, Integer> parseStats(String output) {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("ok", 0);
        stats.put("changed", 0);
        stats.put("skipped", 0);
        stats.put("failed", 0);

        Pattern p = Pattern
                .compile("ok=(\\d+)\\s+changed=(\\d+)\\s+unreachable=(\\d+)\\s+failed=(\\d+)\\s+skipped=(\\d+)");
        Matcher m = p.matcher(output);
        if (m.find()) {
            stats.put("ok", Integer.parseInt(m.group(1)));
            stats.put("changed", Integer.parseInt(m.group(2)));
            stats.put("failed", Integer.parseInt(m.group(4)));
            stats.put("skipped", Integer.parseInt(m.group(5)));
        }
        return stats;
    }

    public List<String> extractHosts(String output) {
        List<String> hosts = new ArrayList<>();
        for (String line : output.split("\\R")) {
            if (line.matches("^\\s*\\S+\\s+:.*ok=.*")) {
                String host = line.split(":")[0].trim();
                hosts.add(host);
            }
        }
        return hosts;
    }

    public void runPlaybookStreamed(String playbook,
            String inventory,
            String tags,
            String skipTags,
            String hostLimit,
            String becomePassword,
            ResponseBodyEmitter emitter) throws IOException {

        // #1 Validate user-controlled parameters before passing to ProcessBuilder
        validateTags(tags, "tags");
        validateTags(skipTags, "skipTags");
        validateHostLimit(hostLimit);

        List<String> cmd = new ArrayList<>();
        cmd.add("ansible-playbook");
        cmd.add(playbook);
        cmd.add("-i");
        cmd.add(inventory);

        if (tags != null && !tags.isBlank()) {
            cmd.add("--tags");
            cmd.add(tags);
        }
        if (skipTags != null && !skipTags.isBlank()) {
            cmd.add("--skip-tags");
            cmd.add(skipTags);
        }
        if (hostLimit != null && !hostLimit.isBlank()) {
            cmd.add("--limit");
            cmd.add(hostLimit);
        }

        // Befehl ohne Passwort-Datei für die Ausgabe merken
        String displayCmd = String.join(" ", cmd);

        Path becomeFile = null;
        if (becomePassword != null && !becomePassword.isBlank()) {
            becomeFile = Files.createTempFile("ansible-become-", ".pwd");
            becomeFile.toFile().setReadable(false, false);
            becomeFile.toFile().setReadable(true, true);
            Files.writeString(becomeFile, becomePassword);
            cmd.add("--become-password-file");
            cmd.add(becomeFile.toString());
        }

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.environment().put("ANSIBLE_HOST_KEY_CHECKING", "False");
        pb.redirectErrorStream(true);
        Process process = pb.start();

        emitter.send("$ " + displayCmd + "\n\n");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                emitter.send(line + "\n");
            }
        } catch (Exception e) {
            emitter.send("❌ Fehler: " + e.getMessage());
        } finally {
            // #8 Always delete become password file immediately after reading output
            if (becomeFile != null) {
                Files.deleteIfExists(becomeFile);
            }
        }

        // #8 Wait for process and report non-zero exit codes
        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                emitter.send("\n❌ ansible-playbook beendet mit Exit-Code: " + exitCode + "\n");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            emitter.send("\n❌ Ausführung unterbrochen\n");
        }
    }
}
