package com.goodbird.buildtools;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import java.util.*;

/**
 * Compares reference and fork Java source files at the AST level to find differences.
 */
public class ForkDiffer {

    public static class DiffResult {
        /** Methods that exist in reference but have different bodies in fork. */
        public List<MethodDeclaration> changedMethods = new ArrayList<>();

        /** The reference version of each changed method (for @Shadow extraction). */
        public Map<String, MethodDeclaration> changedMethodOriginals = new HashMap<>();

        /** Methods that exist only in fork (new additions). */
        public List<MethodDeclaration> newMethods = new ArrayList<>();

        /** Fields that exist only in fork. */
        public List<FieldDeclaration> newFields = new ArrayList<>();

        /** All fields from the reference class (candidates for @Shadow). */
        public List<FieldDeclaration> referenceFields = new ArrayList<>();

        /** All methods from the reference class (candidates for @Shadow). */
        public List<MethodDeclaration> referenceMethods = new ArrayList<>();

        /** Interfaces implemented in fork but not in reference. */
        public List<ClassOrInterfaceType> newInterfaces = new ArrayList<>();

        /** Interfaces implemented in the reference class (for @Shadow type compatibility). */
        public List<ClassOrInterfaceType> referenceInterfaces = new ArrayList<>();

        /** Constructors that differ between reference and fork. */
        public List<ConstructorDeclaration> changedConstructors = new ArrayList<>();

        /** New constructors only in fork. */
        public List<ConstructorDeclaration> newConstructors = new ArrayList<>();

        /** Whether the reference class is abstract. */
        public boolean isAbstract = false;

        /** The reference class's superclass (if any). */
        public ClassOrInterfaceType superClass = null;

        public boolean isEmpty() {
            return changedMethods.isEmpty() && newMethods.isEmpty()
                && newFields.isEmpty() && newInterfaces.isEmpty()
                && changedConstructors.isEmpty() && newConstructors.isEmpty();
        }
    }

    private final JavaParser parser;

    public ForkDiffer() {
        ParserConfiguration config = new ParserConfiguration()
            .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
        this.parser = new JavaParser(config);
    }

    public DiffResult diff(String referenceSource, String normalizedForkSource) {
        DiffResult result = new DiffResult();

        var refParseResult = parser.parse(referenceSource);
        if (refParseResult.getProblems().size() > 0) {
            throw new RuntimeException("Parse problems in reference: " + refParseResult.getProblems());
        }
        CompilationUnit refCU = refParseResult.getResult()
            .orElseThrow(() -> new RuntimeException("Failed to parse reference source"));

        var forkParseResult = parser.parse(normalizedForkSource);
        if (forkParseResult.getProblems().size() > 0) {
            throw new RuntimeException("Parse problems in fork: " + forkParseResult.getProblems());
        }
        CompilationUnit forkCU = forkParseResult.getResult()
            .orElseThrow(() -> new RuntimeException("Failed to parse fork source"));

        // Get primary type - try getPrimaryType() first, fall back to first type
        TypeDeclaration<?> refType = refCU.getPrimaryType()
            .or(() -> refCU.getTypes().isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(refCU.getTypes().get(0)))
            .orElseThrow(() -> new RuntimeException("No primary type in reference (types found: " + refCU.getTypes().size() + ")"));
        TypeDeclaration<?> forkType = forkCU.getPrimaryType()
            .or(() -> forkCU.getTypes().isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(forkCU.getTypes().get(0)))
            .orElseThrow(() -> new RuntimeException("No primary type in fork (types found: " + forkCU.getTypes().size() + ")"));

        // Store class metadata
        if (refType instanceof ClassOrInterfaceDeclaration refClass) {
            result.isAbstract = refClass.isAbstract();
            refClass.getExtendedTypes().stream().findFirst().ifPresent(t -> result.superClass = t);
        }

        // Build reference maps
        Map<String, MethodDeclaration> refMethods = new LinkedHashMap<>();
        for (MethodDeclaration m : refType.getMethods()) {
            refMethods.put(methodSignature(m), m);
        }

        Map<String, FieldDeclaration> refFieldsByName = new LinkedHashMap<>();
        for (FieldDeclaration f : refType.getFields()) {
            for (VariableDeclarator v : f.getVariables()) {
                refFieldsByName.put(v.getNameAsString(), f);
            }
        }

        Map<String, ConstructorDeclaration> refConstructors = new LinkedHashMap<>();
        for (ConstructorDeclaration c : refType.getConstructors()) {
            refConstructors.put(constructorSignature(c), c);
        }

        // Store all reference members for @Shadow generation
        result.referenceFields.addAll(refType.getFields());
        result.referenceMethods.addAll(refType.getMethods());

        // Compare methods
        for (MethodDeclaration forkMethod : forkType.getMethods()) {
            String sig = methodSignature(forkMethod);
            MethodDeclaration refMethod = refMethods.get(sig);

            if (refMethod == null) {
                result.newMethods.add(forkMethod);
            } else {
                // Compare method bodies (normalized string comparison)
                String refBody = normalizeBody(refMethod);
                String forkBody = normalizeBody(forkMethod);
                if (!refBody.equals(forkBody)) {
                    // Check if method is final - cannot @Overwrite final methods
                    if (refMethod.isFinal()) {
                        // Skip with warning - these need manual mixin handling
                        continue;
                    }
                    result.changedMethods.add(forkMethod);
                    result.changedMethodOriginals.put(sig, refMethod);
                }
            }
        }

        // Compare fields
        Set<String> forkFieldNames = new HashSet<>();
        for (FieldDeclaration forkField : forkType.getFields()) {
            for (VariableDeclarator v : forkField.getVariables()) {
                forkFieldNames.add(v.getNameAsString());
                if (!refFieldsByName.containsKey(v.getNameAsString())) {
                    result.newFields.add(forkField);
                    break; // Only add the field declaration once
                }
            }
        }

        // Compare constructors
        for (ConstructorDeclaration forkCon : forkType.getConstructors()) {
            String sig = constructorSignature(forkCon);
            ConstructorDeclaration refCon = refConstructors.get(sig);

            if (refCon == null) {
                result.newConstructors.add(forkCon);
            } else {
                String refBody = refCon.getBody().toString();
                String forkBody = forkCon.getBody().toString();
                if (!refBody.equals(forkBody)) {
                    result.changedConstructors.add(forkCon);
                }
            }
        }

        // Compare interfaces
        if (refType instanceof ClassOrInterfaceDeclaration refClass
                && forkType instanceof ClassOrInterfaceDeclaration forkClass) {
            Set<String> refIfaces = new HashSet<>();
            for (ClassOrInterfaceType t : refClass.getImplementedTypes()) {
                refIfaces.add(t.asString());
                result.referenceInterfaces.add(t);
            }
            for (ClassOrInterfaceType t : forkClass.getImplementedTypes()) {
                if (!refIfaces.contains(t.asString())) {
                    result.newInterfaces.add(t);
                }
            }
        }

        return result;
    }

    /**
     * Create a method signature string for comparison (name + parameter types).
     */
    private String methodSignature(MethodDeclaration m) {
        StringBuilder sb = new StringBuilder();
        sb.append(m.getNameAsString()).append("(");
        m.getParameters().forEach(p -> {
            sb.append(p.getType().asString());
            if (p.isVarArgs()) sb.append("...");
            sb.append(",");
        });
        sb.append(")");
        return sb.toString();
    }

    private String constructorSignature(ConstructorDeclaration c) {
        StringBuilder sb = new StringBuilder();
        sb.append("<init>(");
        c.getParameters().forEach(p -> {
            sb.append(p.getType().asString());
            if (p.isVarArgs()) sb.append("...");
            sb.append(",");
        });
        sb.append(")");
        return sb.toString();
    }

    /**
     * Normalize method body for comparison (strip comments, normalize whitespace).
     */
    private String normalizeBody(MethodDeclaration m) {
        return m.getBody()
            .map(body -> body.toString().replaceAll("//.*", "").replaceAll("/\\*.*?\\*/", "").replaceAll("\\s+", " ").trim())
            .orElse("");
    }
}
