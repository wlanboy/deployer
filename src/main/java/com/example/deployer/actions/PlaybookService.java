package com.example.deployer.actions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

@Service
public class PlaybookService {

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
            if (line.matches("^\\s*\\S+\\s+:.*ok=")) {
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
            ResponseBodyEmitter emitter) throws IOException {

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

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                emitter.send(line + "\n");
            }
        } catch (Exception e) {
            emitter.send("❌ Fehler: " + e.getMessage());
        }
    }
}
