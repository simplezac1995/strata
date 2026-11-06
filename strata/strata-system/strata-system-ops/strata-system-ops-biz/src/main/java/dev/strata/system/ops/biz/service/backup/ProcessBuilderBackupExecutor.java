// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service.backup;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Production {@link BackupExecutor}: shells out to {@code scripts/backup.sh} (Linux) or
 * {@code scripts/backup.ps1} (Windows) through {@link ProcessBuilder}, inherits the process environment
 * (so {@code STRATA_DB_*} credentials reach the dump tool), and reads the produced file path the script prints on
 * stdout.
 *
 * <p>A hard timeout bounds a hung dump; any non-zero exit or timeout is reported as {@code failed} rather than
 * thrown, so the backup record row can be marked accordingly.
 */
@Component
public class ProcessBuilderBackupExecutor implements BackupExecutor {

    private static final Logger log = LoggerFactory.getLogger(ProcessBuilderBackupExecutor.class);
    private static final long TIMEOUT_SECONDS = 600;

    private final Path scriptDir;

    public ProcessBuilderBackupExecutor(@Value("${strata.backup.script-dir:scripts}") String scriptDir) {
        this.scriptDir = Path.of(scriptDir);
    }

    @Override
    public BackupOutcome run(String type, String requestedPath) {
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        Path script = windows ? scriptDir.resolve("backup.ps1") : scriptDir.resolve("backup.sh");

        ProcessBuilder pb = windows
                ? new ProcessBuilder(
                        "powershell",
                        "-NoProfile",
                        "-ExecutionPolicy",
                        "Bypass",
                        script.toAbsolutePath().toString(),
                        type)
                : new ProcessBuilder("bash", script.toAbsolutePath().toString(), type);
        pb.redirectErrorStream(true);
        if (StringUtils.hasText(requestedPath)) {
            pb.environment().put("BACKUP_DIR", requestedPath);
        }

        try {
            Process process = pb.start();
            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return BackupOutcome.failed("backup timed out after " + TIMEOUT_SECONDS + "s");
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            int exit = process.exitValue();
            if (exit != 0) {
                return BackupOutcome.failed("backup script exited " + exit + ": " + tail(output));
            }
            String produced = lastNonEmptyLine(output);
            if (!StringUtils.hasText(produced)) {
                return BackupOutcome.failed("backup script produced no output path");
            }
            Path file = Path.of(produced);
            long size = Files.exists(file) ? Files.size(file) : 0L;
            return BackupOutcome.ok(file.getFileName().toString(), produced, size);
        } catch (IOException e) {
            log.warn("backup script launch failed: {}", e.getMessage());
            return BackupOutcome.failed("cannot launch backup script: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return BackupOutcome.failed("backup interrupted");
        }
    }

    private static String lastNonEmptyLine(String output) {
        if (output == null || output.isBlank()) {
            return null;
        }
        String[] lines = output.split("\\R");
        for (int i = lines.length - 1; i >= 0; i--) {
            if (!lines[i].isBlank()) {
                return lines[i].trim();
            }
        }
        return null;
    }

    private static String tail(String output) {
        if (output == null) {
            return "";
        }
        String trimmed = output.trim();
        return trimmed.length() > 200 ? trimmed.substring(trimmed.length() - 200) : trimmed;
    }
}
