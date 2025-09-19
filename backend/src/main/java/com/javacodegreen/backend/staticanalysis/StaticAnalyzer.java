package com.javacodegreen.backend.staticanalysis;

import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.code.*;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;

import java.io.File;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class StaticAnalyzer {

    private final RuleEngine ruleEngine;

    public StaticAnalyzer() {
        this.ruleEngine = new RuleEngine();
    }

    /**
     * Analyze a project directory or single-file parent folder.
     *
     * @param projectPath File path to project source root (e.g., src/main/java or project root)
     * @param projectName user-friendly project name
     * @param commitId    commit identifier
     * @return AnalysisResult containing findings & summary
     */
    public AnalysisResult analyze(File projectPath, String projectName, String commitId) {
        System.out.println("=== StaticAnalyzer DEBUG START ===");
        System.out.println("Analyzing project: " + projectName + " @ " + projectPath.getAbsolutePath());

        Launcher launcher = new Launcher();
        launcher.addInputResource(projectPath.getAbsolutePath());
        launcher.getEnvironment().setNoClasspath(true); // safe when dependencies may be absent
        launcher.buildModel();

        CtModel model = launcher.getModel();
        List<Finding> findings = new ArrayList<>();

        // 1. Debug: Print all types & elements (high level)
        System.out.println(">>> Spoon model contents (high level):");
        model.getAllTypes().forEach(t -> {
            System.out.println(" Type: " + t.getQualifiedName());
        });

        // 1b. Debug: print some elements (optional, verbose)
        System.out.println(">>> Spoon model sample elements (verbose):");
        model.getAllTypes().stream().limit(10).forEach(t -> {
            t.getElements(new TypeFilter<>(CtElement.class)).stream().limit(20).forEach(e ->
                    System.out.println("   Element: " + e.getClass().getSimpleName() + " -> " + safeToStringShort(e))
            );
        });

        // 2. Debug: Print all loaded rules
        List<Rule> rules = ruleEngine.getRules();
        System.out.println(">>> Loaded " + rules.size() + " rules:");
        rules.forEach(r -> System.out.println("   Rule: " + r.getId() + " node=" +
                (r.getMatch() != null ? r.getMatch().get("node") : "null") +
                " severity=" + r.getSeverity()));

        // 3. Apply rules (tolerant names)
        for (Rule rule : rules) {
            Map<String, Object> match = rule.getMatch();
            if (match == null) {
                System.out.println(" Rule " + rule.getId() + " has no match section, skipping.");
                continue;
            }
            String rawNode = (String) match.get("node");
            String node = normalizeNodeName(rawNode);
            System.out.println(" Applying rule " + rule.getId() + " on node type: " + rawNode + " -> normalized=" + node);

            switch (node) {
                case "CtBinaryOperator":
                    findBinaryOperators(rule, model, findings);
                    break;
                case "CtConstructorCall":
                    findConstructorCalls(rule, model, findings);
                    break;
                case "CtInvocation":
                    findInvocations(rule, model, findings);
                    break;
                default:
                    System.out.println(" Unknown/unsupported node type in rule: " + rawNode + " (normalized=" + node + ")");
            }
        }

        // 4. Debug: Print findings
        System.out.println(">>> Findings generated: " + findings.size());
        for (Finding f : findings) {
            System.out.println(" Finding: " + f.getRuleId() +
                    " @ " + f.getFile() + ":" + f.getStartLine() +
                    " -> " + f.getMessage() + " (tags=" + f.getTags() + ")");
        }

        Map<String, Integer> summary = computeSummary(findings);

        AnalysisResult result = new AnalysisResult();
        result.setJobId("job-" + UUID.randomUUID().toString());
        result.setProject(Map.of("name", projectName, "commit", commitId));
        result.setAnalyzedAt(Instant.now());
        result.setFindings(findings);
        result.setSummary(summary);

        System.out.println("=== StaticAnalyzer DEBUG END ===");
        return result;
    }

    // --- finders ---

    @SuppressWarnings("unchecked")
    private void findBinaryOperators(Rule rule, CtModel model, List<Finding> findings) {
        Map<String, Object> match = rule.getMatch();
        List<CtBinaryOperator<?>> ops = model.getElements(new TypeFilter<>(CtBinaryOperator.class));
        System.out.println("  [finder] candidate binary ops count=" + ops.size());

        String expectedOpRaw = (String) match.get("operator"); // could be "+", "PLUS"
        String expectedOp = normalizeOperator(expectedOpRaw); // e.g. "PLUS"
        String operandType = (String) match.get("operandType"); // fully qualified name

        Object ancestorObj = match.get("ancestor");
        List<String> ancestorList = ancestorObj instanceof List ? (List<String>) ancestorObj : Collections.emptyList();

        for (CtBinaryOperator<?> op : ops) {
            // debug each candidate
            String kindName = op.getKind() != null ? op.getKind().name() : "UNKNOWN";
            String opType = op.getType() != null ? op.getType().getQualifiedName() : "unknown";
            String shortOp = safeToStringShort(op);

            System.out.println("    candidate binary op: kind=" + kindName + " type=" + opType + " expr=" + shortOp);

            boolean matches = true;

            // operator check
            if (expectedOp != null && !expectedOp.isEmpty()) {
                if (!expectedOp.equalsIgnoreCase(kindName)) {
                    System.out.println("      reject: operator mismatch (expected=" + expectedOp + ")");
                    matches = false;
                }
            }

            // operand/result type check
            if (matches && operandType != null && !operandType.isEmpty()) {
                if (op.getType() == null || !operandType.equals(op.getType().getQualifiedName())) {
                    System.out.println("      reject: operand/result type mismatch (expected=" + operandType + ", actual=" + opType + ")");
                    matches = false;
                }
            }

            // ancestor check: ensure op is inside one of specified ancestors
            if (matches && ancestorList != null && !ancestorList.isEmpty()) {
                if (!matchesAncestorAny(op, ancestorList)) {
                    System.out.println("      reject: ancestor check failed (expected any of " + ancestorList + ")");
                    matches = false;
                } else {
                    System.out.println("      ancestor check passed");
                }
            }

            if (matches) {
                System.out.println("      ACCEPT -> creating finding for binary op");
                createAndAddFindingForElement(rule, op, findings, Map.of(
                        "snippet", safeToStringShort(op),
                        "astNode", Map.of("type", "CtBinaryOperator", "operator", kindName)
                ));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void findConstructorCalls(Rule rule, CtModel model, List<Finding> findings) {
        Map<String, Object> match = rule.getMatch();
        List<CtConstructorCall<?>> calls = model.getElements(new TypeFilter<>(CtConstructorCall.class));
        System.out.println("  [finder] candidate constructor calls count=" + calls.size());

        Object ancestorObj = match.get("ancestor");
        List<String> ancestorList = ancestorObj instanceof List ? (List<String>) ancestorObj : Collections.emptyList();
        Object typeObj = match.get("type");
        List<String> typeList = typeObj instanceof List ? (List<String>) typeObj : Collections.emptyList();

        for (CtConstructorCall<?> call : calls) {
            String typeSimple = call.getType() != null ? call.getType().getSimpleName() : "UNKNOWN";
            String typeQualified = call.getType() != null ? call.getType().getQualifiedName() : "UNKNOWN";
            String shortCall = safeToStringShort(call);
            System.out.println("    candidate ctor call: type=" + typeQualified + " expr=" + shortCall);

            boolean matches = true;

            // type filter
            if (typeList != null && !typeList.isEmpty()) {
                boolean ok = typeList.stream().anyMatch(t -> t.equals(typeSimple) || t.equals(typeQualified));
                if (!ok) {
                    System.out.println("      reject: type not in rule.type list (expected any of " + typeList + ")");
                    matches = false;
                } else {
                    System.out.println("      type check passed");
                }
            }

            // ancestor check
            if (matches && ancestorList != null && !ancestorList.isEmpty()) {
                if (!matchesAncestorAny(call, ancestorList)) {
                    System.out.println("      reject: ancestor check failed (expected any of " + ancestorList + ")");
                    matches = false;
                } else {
                    System.out.println("      ancestor check passed");
                }
            }

            if (matches) {
                System.out.println("      ACCEPT -> creating finding for constructor call");
                createAndAddFindingForElement(rule, call, findings, Map.of(
                        "snippet", safeToStringShort(call),
                        "astNode", Map.of("type", "CtConstructorCall", "constructor", typeSimple)
                ));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void findInvocations(Rule rule, CtModel model, List<Finding> findings) {
        Map<String, Object> match = rule.getMatch();
        List<CtInvocation<?>> invs = model.getElements(new TypeFilter<>(CtInvocation.class));
        System.out.println("  [finder] candidate invocations count=" + invs.size());

        Object ancestorObj = match.get("ancestor");
        List<String> ancestorList = ancestorObj instanceof List ? (List<String>) ancestorObj : Collections.emptyList();
        Object nameObj = match.get("name");
        String expectedName = nameObj != null ? (String) nameObj : null;

        for (CtInvocation<?> inv : invs) {
            String methodName = inv.getExecutable() != null ? inv.getExecutable().getSimpleName() : "UNKNOWN";
            String shortInv = safeToStringShort(inv);
            System.out.println("    candidate invocation: method=" + methodName + " expr=" + shortInv);

            boolean matches = true;

            if (expectedName != null && !expectedName.isEmpty()) {
                if (!expectedName.equals(methodName)) {
                    System.out.println("      reject: invocation name mismatch (expected=" + expectedName + ")");
                    matches = false;
                } else {
                    System.out.println("      name check passed");
                }
            }

            if (matches && ancestorList != null && !ancestorList.isEmpty()) {
                if (!matchesAncestorAny(inv, ancestorList)) {
                    System.out.println("      reject: ancestor check failed (expected any of " + ancestorList + ")");
                    matches = false;
                } else {
                    System.out.println("      ancestor check passed");
                }
            }

            if (matches) {
                System.out.println("      ACCEPT -> creating finding for invocation");
                createAndAddFindingForElement(rule, inv, findings, Map.of(
                        "snippet", safeToStringShort(inv),
                        "astNode", Map.of("type", "CtInvocation", "method", methodName)
                ));
            }
        }
    }

    // --- helpers ---

    /**
     * Normalize common node names coming from rules.json into canonical Spoon class names used in code.
     * Accepts: "BinaryOperator", "CtBinaryOperator", "BinaryExpr", etc.
     */
    private String normalizeNodeName(String raw) {
        if (raw == null) return "";
        String r = raw.trim();
        r = r.replaceAll("^Ct", ""); // remove leading Ct if present
        r = r.replaceAll("Expr$", "Operator"); // e.g. BinaryExpr -> BinaryOperator
        r = r.replaceAll("Operator$", "Operator");
        // canonical mappings
        switch (r.toLowerCase()) {
            case "binaryoperator":
            case "binaryexpr":
            case "binary":
            case "binop":
                return "CtBinaryOperator";
            case "constructorcall":
            case "ctconstructorcall":
            case "constructor":
                return "CtConstructorCall";
            case "invocation":
            case "methodinvocation":
            case "ctinvocation":
                return "CtInvocation";
            default:
                // allow raw ct names
                if (raw.startsWith("Ct")) return raw;
                return raw;
        }
    }

    /**
     * Normalize operator specification: accept "+", "PLUS", "plus"
     * Return canonical enum name used by Spoon's BinaryOperatorKind e.g. "PLUS"
     */
    private String normalizeOperator(String opRaw) {
        if (opRaw == null) return null;
        String o = opRaw.trim();
        if (o.equals("+")) return "PLUS";
        if (o.equals("-")) return "MINUS";
        if (o.equals("*")) return "MUL";
        if (o.equals("/")) return "DIV";
        // if already a name like PLUS, return uppercase
        return o.toUpperCase();
    }

    /**
     * Matches whether the given element has any parent matching any of the names in ancestorNames.
     * Accepts flexible ancestor names like "CtFor", "ForLoop", "CtLoop", "CtWhile", "For", "While".
     */
    private boolean matchesAncestorAny(CtElement element, List<String> ancestorNames) {
        if (ancestorNames == null || ancestorNames.isEmpty()) return true;
        // make normalized set
        Set<String> normalized = ancestorNames.stream().map(this::normalizeAncestorName).collect(Collectors.toSet());

        CtElement parent = element.getParent();
        while (parent != null) {
            String simple = parent.getClass().getSimpleName(); // e.g., CtForImpl, CtWhileImpl, CtLoopImpl
            String normalizedParent = normalizeAncestorName(simple);
            if (normalized.contains(normalizedParent)) return true;

            // also check interfaces/classes: CtLoop
            if (normalized.contains("CtLoop") && parent.getParent(spoon.reflect.code.CtLoop.class) != null) return true;

            parent = parent.getParent();
        }
        return false;
    }

    private String normalizeAncestorName(String name) {
        if (name == null) return "";
        String n = name.replaceAll("Impl$", ""); // CtForImpl -> CtFor
        n = n.replaceAll("^Ct", "Ct"); // keep Ct prefix
        // map common names:
        switch (n.toLowerCase()) {
            case "forloop":
            case "ctfor":
            case "for":
                return "CtFor";
            case "whileloop":
            case "ctwhile":
            case "while":
                return "CtWhile";
            case "doloop":
            case "ctdo":
            case "do":
                return "CtDo";
            case "ctloop":
            case "loop":
                return "CtLoop";
            default:
                // strip anything after Ct e.g., CtForImpl -> CtFor
                if (n.startsWith("Ct")) return n;
                return n;
        }
    }

    private void createAndAddFindingForElement(Rule rule, CtElement element, List<Finding> findings, Map<String, Object> evidence) {
        Finding f = new Finding();
        f.setId("F-" + UUID.randomUUID().toString());
        f.setRuleId(rule.getId());
        f.setSeverity(rule.getSeverity());
        f.setEnergyScore(scoreFromSeverity(rule.getSeverity()));
        f.setMessage(rule.getDescription());
        String file = element.getPosition() != null && element.getPosition().getFile() != null
                ? element.getPosition().getFile().getPath()
                : "unknown";
        f.setFile(file);
        int start = element.getPosition() != null ? element.getPosition().getLine() : -1;
        int end = element.getPosition() != null ? element.getPosition().getEndLine() : start;
        f.setStartLine(start);
        f.setEndLine(end);
        f.setSuggestion(rule.getSuggestion());
        f.setEvidence(evidence);
        f.setTags(rule.getTags());

        findings.add(f);
    }

    private Map<String, Integer> computeSummary(List<Finding> findings) {
        int total = findings.size();
        int high = (int) findings.stream().filter(f -> "HIGH".equalsIgnoreCase(f.getSeverity())).count();
        int medium = (int) findings.stream().filter(f -> "MEDIUM".equalsIgnoreCase(f.getSeverity())).count();
        int low = (int) findings.stream().filter(f -> "LOW".equalsIgnoreCase(f.getSeverity())).count();
        return Map.of("totalFindings", total, "high", high, "medium", medium, "low", low);
    }

    private double scoreFromSeverity(String severity) {
        if (severity == null) return 0.0;
        return switch (severity.toUpperCase()) {
            case "HIGH" -> 8.0;
            case "MEDIUM" -> 5.0;
            case "LOW" -> 2.5;
            default -> 1.0;
        };
    }

    private String safeToStringShort(CtElement e) {
        try {
            String s = e == null ? "" : e.toString();
            if (s.length() > 200) return s.substring(0, 200) + "...";
            return s.replace("\n", " ").replaceAll("\\s+", " ").trim();
        } catch (Exception ex) {
            return e.getClass().getSimpleName();
        }
    }

    private String safeToString(CtElement e) {
        try {
            return e == null ? "" : e.toString();
        } catch (Exception ex) {
            return e.getClass().getSimpleName();
        }
    }
}
