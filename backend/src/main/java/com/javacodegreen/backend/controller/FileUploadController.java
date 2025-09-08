package com.javacodegreen.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping("/api")
public class FileUploadController {

    private static final String UPLOAD_DIR = "uploads/";
    private static final String JOULARJX_DIR="src/main/java/com/javacodegreen/backend/JoularJX/";
    private static final String JOULARJX_RESULT_DIR="joularjx-result/";


    private void insertNested(Map<String, Object> map, List<String> keys, String value) {
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i).replace(".csv", ""); // remove .csv
            if (i == keys.size() - 1) {
                map.put(key, value);
            } else {
                map = (Map<String, Object>) map.computeIfAbsent(key, k -> new HashMap<>());
            }
        }
    }

    public Map<String, Object> parseJoularJxResults(Path resultDir) throws IOException {
        Map<String, Object> results = new HashMap<>();

        try (Stream<Path> paths = Files.walk(resultDir)) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".csv"))
                    .forEach(file -> {
                        try {
                            // relative path from resultDir (e.g. all/runtime/methods.csv)
                            Path relative = resultDir.relativize(file);
                            List<String> parts = StreamSupport.stream(relative.spliterator(), false)
                                    .map(Path::toString)
                                    .collect(Collectors.toList());

                            // Read CSV content as String
                            String csvContent = Files.readString(file);

                            // Insert into nested map
                            insertNested(results, parts, csvContent);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        }

        return results;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("Error", "No file selected"));
            }

            // Ensure uploads directory exists
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Save file
            Path filePath = uploadPath.resolve(file.getOriginalFilename());
            Files.write(filePath, file.getBytes());

            // --- Compile the file ---
            Process compileProcess = new ProcessBuilder(
                    "javac", filePath.toAbsolutePath().toString()
            ).redirectErrorStream(true).start();

            String compileOutput = new String(compileProcess.getInputStream().readAllBytes());
            int compileExit = compileProcess.waitFor();

            if (compileExit != 0) {
                return ResponseEntity.internalServerError()
                        .body(Map.of("Error",compileOutput));
            }

            // --- Run the file (only if compilation succeeded) ---
            String className = file.getOriginalFilename().replace(".java", "");
//            Process runProcess = new ProcessBuilder(
//                    "java", "-cp", uploadPath.toAbsolutePath().toString(), className
//            ).redirectErrorStream(true).start();
//
//            String runOutput = new String(runProcess.getInputStream().readAllBytes());
//            int runExit = runProcess.waitFor();
//
//            if (runExit != 0) {
//                return ResponseEntity.internalServerError()
//                        .body("Execution failed:\n" + runOutput);
//            }

            Path joularjxPath = Paths.get(JOULARJX_DIR, "joularjx-3.0.1.jar");
            Path joularjxConfig = Paths.get(JOULARJX_DIR, "config.properties");

            Process energyProcess = new ProcessBuilder(
                    "java",
                    "-javaagent:" + joularjxPath.toAbsolutePath().toString(),
                    "-Djoularjx.config=" + joularjxConfig.toAbsolutePath(),
                    "-cp", uploadPath.toAbsolutePath().toString(),
                    className
            ).redirectErrorStream(true).start();

            String energyOutput=new String(energyProcess.getInputStream().readAllBytes());
            int runEnergyExit=energyProcess.waitFor();

            if (runEnergyExit!=0){
                return ResponseEntity.internalServerError()
                        .body(Map.of("Error",energyOutput));
            }

            Pattern pattern = Pattern.compile("joularjx-result/(\\S+)/");
            Matcher matcher = pattern.matcher(energyOutput);

            if (!matcher.find()) {
                return ResponseEntity.internalServerError().body(Map.of("Error","No runId found in output"));
            }
            String runId = matcher.group(1);

            // Parse result directory
            Path resultDir = Paths.get(JOULARJX_RESULT_DIR, runId);
            Map<String, Object> resultData = parseJoularJxResults(resultDir);

            Map<String, Object> response = new HashMap<>();
            response.put("runId", runId);
            response.put("results", resultData);

            return ResponseEntity.ok(response);

//            return ResponseEntity.ok("File compiled and executed successfully!\nOutput:\n" + energyOutput);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("Error:",e.getMessage()));
        }
    }
}
