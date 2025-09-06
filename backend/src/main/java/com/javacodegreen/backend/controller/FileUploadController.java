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
    private static final String JOULARJX_DIR="src/main/java/com/javacodegreen/backend/JoularJX/";

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
                        .body("Enegy operation is failed:\n"+energyOutput);
            }

            return ResponseEntity.ok("File compiled and executed successfully!\nOutput:\n" + energyOutput);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
}
