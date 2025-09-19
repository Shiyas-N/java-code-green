package com.javacodegreen.backend.staticanalysis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

public class RuleEngine {
    private List<Rule> rules;

    public RuleEngine() {
        loadRules();
    }

    private void loadRules() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = getClass().getClassLoader().getResourceAsStream("rules/rules.json");
            if (is != null) {
                rules = mapper.readValue(is, new TypeReference<List<Rule>>() {});
            } else {
                rules = Collections.emptyList();
                System.err.println("⚠️ rules.json not found in resources/rules/");
            }
        } catch (Exception e) {
            e.printStackTrace();
            rules = Collections.emptyList();
        }
    }

    public List<Rule> getRules() {
        return rules;
    }
}
