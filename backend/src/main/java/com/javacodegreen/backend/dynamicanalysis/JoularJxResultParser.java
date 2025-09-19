package com.javacodegreen.backend.dynamicanalysis;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class JoularJxResultParser {

    private final String resultDir;

    public JoularJxResultParser(String resultDir) {
        this.resultDir = resultDir;
    }

    public Map<String, Object> parse(String joularjxOutput) throws IOException {
        Pattern pattern = Pattern.compile("joularjx-result/(\\S+)/");
        Matcher matcher = pattern.matcher(joularjxOutput);
        if (!matcher.find()) {
            throw new RuntimeException("No runId found in JoularJX output");
        }
        String runId = matcher.group(1);

        Path resultPath = Paths.get(resultDir, runId);
        Map<String, Object> results = new HashMap<>();

        try (Stream<Path> paths = Files.walk(resultPath)) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".csv"))
                    .forEach(file -> {
                        try {
                            Path relative = resultPath.relativize(file);
                            List<String> parts = StreamSupport.stream(relative.spliterator(), false)
                                    .map(Path::toString)
                                    .collect(Collectors.toList());

                            String csvContent = Files.readString(file);
                            insertNested(results, parts, csvContent);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
        results.put("runId", runId);
        return results;
    }

    @SuppressWarnings("unchecked")
    private void insertNested(Map<String, Object> map, List<String> keys, String value) {
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i).replace(".csv", "");
            if (i == keys.size() - 1) {
                map.put(key, value);
            } else {
                map = (Map<String, Object>) map.computeIfAbsent(key, k -> new HashMap<>());
            }
        }
    }
}
