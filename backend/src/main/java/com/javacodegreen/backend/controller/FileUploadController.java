package com.javacodegreen.backend.controller;

import com.javacodegreen.backend.staticanalysis.StaticAnalyzer;
import com.javacodegreen.backend.staticanalysis.AnalysisResult;
import com.javacodegreen.backend.dynamicanalysis.DynamicAnalyzer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api")
public class FileUploadController {

    private static final String UPLOAD_DIR = "uploads/";

    private final StaticAnalyzer staticAnalyzer = new StaticAnalyzer();
    private final DynamicAnalyzer dynamicAnalyzer = new DynamicAnalyzer(
            Paths.get("src/main/java/com/javacodegreen/backend/JoularJX"),
            Paths.get(UPLOAD_DIR),
            "joularjx-result"
    );

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No file selected"));
            }

            // Ensure uploads directory exists
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Save file
            Path filePath = uploadPath.resolve(file.getOriginalFilename());
            Files.write(filePath, file.getBytes());

            File savedFile = filePath.toFile();
            String originalName = file.getOriginalFilename();
            String className = (originalName == null ? "" : originalName.replaceFirst("\\.(java|class)$", ""));

            // --- Run Static Analysis ---
            AnalysisResult staticResults = staticAnalyzer.analyze(
                    savedFile,       // project root dir
                    originalName,                    // project name (use filename here)
                    UUID.randomUUID().toString()     // fake commit hash
            );

            // --- Run Dynamic Analysis ---

            Process compileProcess = new ProcessBuilder(
                    "javac", filePath.toAbsolutePath().toString()
            ).redirectErrorStream(true).start();

            Map<String, Object> dynamicResults = dynamicAnalyzer.run(className);

            // --- Build combined response ---
            Map<String, Object> response = new HashMap<>();
            response.put("staticAnalysis", staticResults);
            response.put("dynamicAnalysis", dynamicResults);
            response.put("analyzedAt", Instant.now().toString());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
