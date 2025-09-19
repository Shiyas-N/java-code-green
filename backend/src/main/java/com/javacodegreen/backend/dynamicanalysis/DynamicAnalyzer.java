package com.javacodegreen.backend.dynamicanalysis;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public class DynamicAnalyzer {

    private final Path joularjxDir;
    private final Path uploadDir;
    private final String joularjxResultDir;

    public DynamicAnalyzer(Path joularjxDir, Path uploadDir, String joularjxResultDir) {
        this.joularjxDir = joularjxDir;
        this.uploadDir = uploadDir;
        this.joularjxResultDir = joularjxResultDir;
    }

    public Map<String, Object> run(String className) throws IOException, InterruptedException {
        Path joularjxPath = joularjxDir.resolve("joularjx-3.0.1.jar");
        Path joularjxConfig = joularjxDir.resolve("config.properties");

        Process energyProcess = new ProcessBuilder(
                "java",
                "-XX:-Inline", "-Xint",
                "-javaagent:" + joularjxPath.toAbsolutePath(),
                "-Djoularjx.config=" + joularjxConfig.toAbsolutePath(),
                "-cp", uploadDir.toAbsolutePath().toString(),
                className
        ).redirectErrorStream(true).start();

        String output = new String(energyProcess.getInputStream().readAllBytes());
        int exit = energyProcess.waitFor();

        if (exit != 0) {
            throw new RuntimeException("JoularJX run failed: " + output);
        }

        return new JoularJxResultParser(joularjxResultDir).parse(output);
    }
}
