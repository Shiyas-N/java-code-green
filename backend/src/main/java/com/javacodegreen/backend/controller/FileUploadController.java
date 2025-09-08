package com.javacodegreen.backend.controller;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.WhileStmt;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.util.*;
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

    // ================== STATIC ANALYSIS SECTION ==================

    // Rule violation model
    static class RuleViolation {
        String ruleId;
        int line;
        String issue;
        String suggestion;

        RuleViolation(String ruleId, int line, String issue, String suggestion) {
            this.ruleId = ruleId;
            this.line = line;
            this.issue = issue;
            this.suggestion = suggestion;
        }

        public Map<String, Object> toMap() {
            return Map.of(
                    "ruleId", ruleId,
                    "line", line,
                    "issue", issue,
                    "suggestion", suggestion
            );
        }
    }

    // Run static analysis rules
    private List<RuleViolation> runStaticAnalysis(Path filePath) {
        List<RuleViolation> violations = new ArrayList<>();
        try {
            String code = Files.readString(filePath);
            CompilationUnit cu = StaticJavaParser.parse(code);

            // RULE 1: String concatenation in loops
            cu.findAll(ForStmt.class).forEach(loop -> {
                loop.findAll(BinaryExpr.class).forEach(expr -> {
                    if (expr.getOperator() == BinaryExpr.Operator.PLUS) {
                        violations.add(new RuleViolation(
                                "STR_CONCAT_LOOP",
                                expr.getBegin().map(p -> p.line).orElse(-1),
                                "String concatenation inside loop",
                                "Use StringBuilder instead of '+' inside loops."
                        ));
                    }
                });
            });
            cu.findAll(WhileStmt.class).forEach(loop -> {
                loop.findAll(BinaryExpr.class).forEach(expr -> {
                    if (expr.getOperator() == BinaryExpr.Operator.PLUS) {
                        violations.add(new RuleViolation(
                                "STR_CONCAT_LOOP",
                                expr.getBegin().map(p -> p.line).orElse(-1),
                                "String concatenation inside loop",
                                "Use StringBuilder instead of '+' inside loops."
                        ));
                    }
                });
            });

            // RULE 2: Object creation inside loops
            cu.findAll(ForStmt.class).forEach(loop -> {
                loop.findAll(ObjectCreationExpr.class).forEach(obj -> {
                    violations.add(new RuleViolation(
                            "OBJ_IN_LOOP",
                            obj.getBegin().map(p -> p.line).orElse(-1),
                            "Object creation inside loop",
                            "Move object creation outside loop if reusable."
                    ));
                });
            });
            cu.findAll(WhileStmt.class).forEach(loop -> {
                loop.findAll(ObjectCreationExpr.class).forEach(obj -> {
                    violations.add(new RuleViolation(
                            "OBJ_IN_LOOP",
                            obj.getBegin().map(p -> p.line).orElse(-1),
                            "Object creation inside loop",
                            "Move object creation outside loop if reusable."
                    ));
                });
            });

            // RULE 3: File I/O inside loops (simplified - looks for common classes)
            cu.findAll(ObjectCreationExpr.class).forEach(obj -> {
                String type = obj.getType().getNameAsString();
                if (type.equals("FileReader") || type.equals("FileWriter") || type.equals("Scanner")) {
                    violations.add(new RuleViolation(
                            "FILE_IO_LOOP",
                            obj.getBegin().map(p -> p.line).orElse(-1),
                            "File I/O inside loop or frequent creation",
                            "Use Buffered I/O and open files outside loops."
                    ));
                }
            });

        } catch (Exception e) {
            violations.add(new RuleViolation("PARSER_ERROR", -1, "Parsing failed", e.getMessage()));
        }
        return violations;
    }

    // ================== JOULARJX HANDLING ==================

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
                            Path relative = resultDir.relativize(file);
                            List<String> parts = StreamSupport.stream(relative.spliterator(), false)
                                    .map(Path::toString)
                                    .collect(Collectors.toList());

                            String csvContent = Files.readString(file);
                            insertNested(results, parts, csvContent);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        }

        return results;
    }

    // ================== UPLOAD HANDLING ==================

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

            // --- Static Analysis Step ---
            List<RuleViolation> violations = runStaticAnalysis(filePath);

            // --- Compile the file ---
            Process compileProcess = new ProcessBuilder(
                    "javac", filePath.toAbsolutePath().toString()
            ).redirectErrorStream(true).start();

            String compileOutput = new String(compileProcess.getInputStream().readAllBytes());
            int compileExit = compileProcess.waitFor();

            if (compileExit != 0) {
                return ResponseEntity.internalServerError()
                        .body(Map.of("Error", compileOutput, "staticAnalysis", violations.stream().map(RuleViolation::toMap).toList()));
            }

            // --- Run JoularJX ---
            String className = file.getOriginalFilename().replace(".java", "");
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
                        .body(Map.of("Error",energyOutput, "staticAnalysis", violations.stream().map(RuleViolation::toMap).toList()));
            }

            Pattern pattern = Pattern.compile("joularjx-result/(\\S+)/");
            Matcher matcher = pattern.matcher(energyOutput);

            if (!matcher.find()) {
                return ResponseEntity.internalServerError().body(Map.of("Error","No runId found in output", "staticAnalysis", violations.stream().map(RuleViolation::toMap).toList()));
            }
            String runId = matcher.group(1);

            // Parse result directory
            Path resultDir = Paths.get(JOULARJX_RESULT_DIR, runId);
            Map<String, Object> resultData = parseJoularJxResults(resultDir);

            Map<String, Object> response = new HashMap<>();
            response.put("runId", runId);
            response.put("results", resultData);
            response.put("staticAnalysis", violations.stream().map(RuleViolation::toMap).toList());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("Error", e.getMessage()));
        }
    }
}
