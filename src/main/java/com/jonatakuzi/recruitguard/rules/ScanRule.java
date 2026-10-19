package com.jonatakuzi.recruitguard.rules;

import com.jonatakuzi.recruitguard.model.Finding;
import java.util.List;

/**
 * One independent check. Every rule is a Spring component, so Spring hands
 * the full list to ScanService automatically. Adding a new check means adding
 * one new class - nothing else changes (the Open/Closed principle).
 */
public interface ScanRule {

    List<Finding> evaluate(ScanContext context);
}
