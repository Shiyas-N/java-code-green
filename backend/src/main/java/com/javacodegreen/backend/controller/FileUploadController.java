package com.javacodegreen.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;

@RestController
@RequestMapping("/api")
public class FileUploadController {

    private static final String UPLOAD_DIR = "uploads/";

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("No file selected");
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
                        .body("Compilation failed:\n" + compileOutput);
            }

            // --- Run the file (only if compilation succeeded) ---
            String className = file.getOriginalFilename().replace(".java", "");
            Process runProcess = new ProcessBuilder(
                    "java", "-cp", uploadPath.toAbsolutePath().toString(), className
            ).redirectErrorStream(true).start();

            String runOutput = new String(runProcess.getInputStream().readAllBytes());
            int runExit = runProcess.waitFor();

            if (runExit != 0) {
                return ResponseEntity.internalServerError()
                        .body("Execution failed:\n" + runOutput);
            }

            return ResponseEntity.ok("✅ File compiled and executed successfully!\nOutput:\n" + runOutput);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
}
