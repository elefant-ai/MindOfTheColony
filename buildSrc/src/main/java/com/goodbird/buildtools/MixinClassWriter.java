package com.goodbird.buildtools;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates mixin Java source code from a ForkDiffer.DiffResult.
 */
public class MixinClassWriter {

    /**
     * Write a complete mixin Java file.
     *
     * @param mixinPkg      Package for the generated mixin class
     * @param mixinClassName Name of the generated mixin class
     * @param targetFqcn    Fully qualified name of the target MC class
     * @param diff          The diff result from ForkDiffer
     * @param forkSource    The full fork source (with renamed packages)
     * @param refSource     The full reference source (original packages)
     * @return Complete Java source for the mixin class
     */
    public String writeMixin(String mixinPkg, String mixinClassName, String targetFqcn,
                             ForkDiffer.DiffResult diff, String forkSource, String refSource) {

        StringBuilder sb = new StringBuilder();

        // Package
        sb.append("package ").append(mixinPkg).append(";\n\n");

        // Collect imports from fork source (these will be un-renamed later by the task)
        Set<String> imports = collectImports(forkSource);

        // Also collect imports from reference source (needed for inner class types etc.)
        imports.addAll(collectImports(refSource));

        // Add imports for inner classes of the target (e.g., CitizenSkillHandler.SkillData)
        collectInnerClassImports(refSource, targetFqcn, imports);

        // Add wildcard import for the target's package - classes in the same package as
        // the target don't need explicit imports in the original source, but the mixin
        // is in a different package and needs them (e.g., utility classes, the target itself)
        String targetPkg = targetFqcn.substring(0, targetFqcn.lastIndexOf('.'));
        imports.add(targetPkg + ".*");

        // Add import for superclass if it's in the target's package (no import in source)
        if (diff.superClass != null) {
            String superName = diff.superClass.asString();
            boolean alreadyImported = imports.stream()
                .anyMatch(imp -> imp.endsWith("." + superName));
            if (!alreadyImported) {
                imports.add(targetPkg + "." + superName);
            }
        }

        // Add imports for reference interfaces that are in the target's package (no import in source)
        for (ClassOrInterfaceType iface : diff.referenceInterfaces) {
            String ifaceName = iface.asString();
            boolean alreadyImported = imports.stream()
                .anyMatch(imp -> imp.endsWith("." + ifaceName) || imp.endsWith(".*"));
            if (!alreadyImported) {
                // Interface might be in the api package, search for it in reference imports
                // If not found, assume it's in the target's package
                imports.add(targetPkg + "." + ifaceName);
            }
        }

        // Add mixin framework imports
        imports.add("org.spongepowered.asm.mixin.Mixin");
        if (!diff.changedMethods.isEmpty()) {
            imports.add("org.spongepowered.asm.mixin.Overwrite");
        }
        if (!diff.newFields.isEmpty() || hasPrivateNewMethods(diff)) {
            imports.add("org.spongepowered.asm.mixin.Unique");
        }
        imports.add("org.spongepowered.asm.mixin.Shadow");
        if (!diff.changedConstructors.isEmpty()) {
            imports.add("org.spongepowered.asm.mixin.injection.At");
            imports.add("org.spongepowered.asm.mixin.injection.Inject");
            imports.add("org.spongepowered.asm.mixin.injection.callback.CallbackInfo");
        }

        // Write imports (sorted for deterministic output)
        // Include all imports - the mixin is in a different package and needs them all
        imports.stream().sorted().forEach(imp ->
            sb.append("import ").append(imp).append(";\n"));
        sb.append("\n");

        // @Mixin annotation
        sb.append("@Mixin(value = ").append(targetFqcn).append(".class, remap = false)\n");

        // Class declaration - mixin classes must be abstract (they have @Shadow abstract methods)
        sb.append("public abstract ");
        sb.append("class ").append(mixinClassName);

        // Extends - if target has a superclass, extend it for access to super.method(),
        // inherited methods/fields, and type compatibility through the parent chain.
        // Mixin supports extending the target's superclass.
        boolean hasSuperClass = diff.superClass != null;
        if (hasSuperClass) {
            sb.append(" extends ").append(diff.superClass.asString());
        }

        // Implements - combine reference interfaces + new fork interfaces
        // Reference interfaces give type compatibility (e.g. this as ICitizenData),
        // new interfaces add fork-added behaviors (e.g. IExtendedCitizenData).
        List<String> allInterfaces = new ArrayList<>();
        for (ClassOrInterfaceType t : diff.referenceInterfaces) {
            allInterfaces.add(t.asString());
        }
        for (ClassOrInterfaceType t : diff.newInterfaces) {
            allInterfaces.add(t.asString());
        }
        if (!allInterfaces.isEmpty()) {
            sb.append(" implements ");
            sb.append(String.join(", ", allInterfaces));
        }
        sb.append(" {\n\n");

        // Dummy constructor when extending superclass (never called at runtime -
        // mixin classes are never instantiated, this just satisfies the compiler)
        if (hasSuperClass) {
            int superArgCount = getSuperConstructorArgCount(refSource);
            sb.append("    @SuppressWarnings(\"all\")\n");
            sb.append("    ").append(mixinClassName).append("() {\n");
            sb.append("        super(");
            for (int i = 0; i < superArgCount; i++) {
                if (i > 0) sb.append(", ");
                sb.append("null");
            }
            sb.append(");\n");
            sb.append("    }\n\n");
        }

        // @Shadow fields - generate for all reference fields that are referenced
        Set<String> shadowedFieldNames = new HashSet<>();
        for (FieldDeclaration refField : diff.referenceFields) {
            for (VariableDeclarator var : refField.getVariables()) {
                String fieldName = var.getNameAsString();
                // Check if this field is referenced in any changed/new method
                if (isFieldReferenced(fieldName, diff)) {
                    shadowedFieldNames.add(fieldName);
                    sb.append("    @Shadow\n");
                    // Reconstruct field declaration
                    String modifiers = refField.getModifiers().stream()
                        .map(m -> m.getKeyword().asString())
                        .collect(Collectors.joining(" "));
                    if (!modifiers.isEmpty()) {
                        // For @Shadow, we need to keep final if present (use @Shadow @Final)
                        if (modifiers.contains("final")) {
                            sb.append("    @org.spongepowered.asm.mixin.Final\n");
                            modifiers = modifiers.replace("final", "").trim();
                        }
                    }
                    sb.append("    ").append(modifiers);
                    if (!modifiers.isEmpty()) sb.append(" ");
                    sb.append(var.getType().asString()).append(" ");
                    sb.append(fieldName).append(";\n\n");
                }
            }
        }

        // @Shadow methods - generate for reference methods called from changed/new code
        Set<String> shadowedMethodSigs = new HashSet<>();
        for (MethodDeclaration refMethod : diff.referenceMethods) {
            String sig = methodSig(refMethod);
            // Don't shadow methods we're overwriting
            boolean isOverwritten = diff.changedMethods.stream()
                .anyMatch(m -> methodSig(m).equals(sig));
            // Skip methods with type parameters (generics like <J> getJob(Class<J>))
            boolean hasTypeParams = !refMethod.getTypeParameters().isEmpty();
            if (!isOverwritten && !hasTypeParams && isMethodReferenced(refMethod.getNameAsString(), diff)) {
                shadowedMethodSigs.add(sig);
                boolean isStatic = refMethod.isStatic();
                sb.append("    @Shadow\n");
                sb.append("    ");
                String modifiers = refMethod.getModifiers().stream()
                    .map(m -> m.getKeyword().asString())
                    .filter(m -> !m.equals("native") && !m.equals("synchronized"))
                    .collect(Collectors.joining(" "));
                if (modifiers.contains("public")) {
                    sb.append("public ");
                } else if (modifiers.contains("protected")) {
                    sb.append("protected ");
                }
                if (isStatic) {
                    // Static @Shadow methods must be static with a stub body
                    sb.append("static ");
                    sb.append(refMethod.getType().asString()).append(" ");
                    sb.append(refMethod.getNameAsString()).append("(");
                    sb.append(refMethod.getParameters().stream()
                        .map(p -> {
                            String type = p.getType().asString();
                            if (p.isVarArgs()) type += "...";
                            return type + " " + p.getNameAsString();
                        })
                        .collect(Collectors.joining(", ")));
                    String returnType = refMethod.getType().asString();
                    if (returnType.equals("void")) {
                        sb.append(") { }\n\n");
                    } else {
                        sb.append(") { throw new AssertionError(); }\n\n");
                    }
                } else {
                    sb.append("abstract ");
                    sb.append(refMethod.getType().asString()).append(" ");
                    sb.append(refMethod.getNameAsString()).append("(");
                    sb.append(refMethod.getParameters().stream()
                        .map(p -> {
                            String type = p.getType().asString();
                            if (p.isVarArgs()) type += "...";
                            return type + " " + p.getNameAsString();
                        })
                        .collect(Collectors.joining(", ")));
                    sb.append(");\n\n");
                }
            }
        }

        // @Unique new fields
        for (FieldDeclaration newField : diff.newFields) {
            sb.append("    @Unique\n");
            sb.append("    ").append(newField.toString().trim()).append("\n\n");
        }

        // @Overwrite changed methods
        for (MethodDeclaration method : diff.changedMethods) {
            sb.append("    /**\n");
            sb.append("     * @author MindOfTheColony (auto-generated)\n");
            sb.append("     * @reason Fork modification\n");
            sb.append("     */\n");
            sb.append("    @Overwrite\n");
            // Remove any annotations from the method (they come from MC, not mixin)
            String methodStr = method.removeComment().toString();
            // Remove existing annotations from the method declaration
            for (var ann : method.getAnnotations()) {
                methodStr = methodStr.replace(ann.toString(), "");
            }
            sb.append("    ").append(methodStr.trim()).append("\n\n");
        }

        // Constructor changes -> @Inject at TAIL
        for (ConstructorDeclaration con : diff.changedConstructors) {
            sb.append("    @Inject(method = \"<init>\", at = @At(\"TAIL\"))\n");
            sb.append("    private void onConstructorTail(");
            sb.append(con.getParameters().stream()
                .map(p -> p.getType().asString() + " " + p.getNameAsString())
                .collect(Collectors.joining(", ")));
            if (!con.getParameters().isEmpty()) sb.append(", ");
            sb.append("CallbackInfo ci) {\n");
            sb.append("        // TODO: Extract constructor additions from fork diff\n");
            sb.append("    }\n\n");
        }

        // New methods
        for (MethodDeclaration method : diff.newMethods) {
            boolean isPrivate = method.isPrivate();
            boolean isOverride = method.getAnnotationByName("Override").isPresent();

            if (isPrivate && !isOverride) {
                sb.append("    @Unique\n");
            }
            String methodStr = method.toString();
            // Strip @Override if mixin has no superclass and doesn't implement interfaces
            // that define the method (conservative: only strip when no parent at all)
            if (isOverride && !hasSuperClass && allInterfaces.isEmpty()) {
                methodStr = methodStr.replaceFirst("@Override\\s*\n?\\s*", "");
            }
            sb.append("    ").append(methodStr.trim()).append("\n\n");
        }

        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Extract import statements from Java source, preserving static imports.
     * Returns raw import lines (e.g., "com.foo.Bar" or "static com.foo.Bar.CONST").
     */
    private Set<String> collectImports(String source) {
        Set<String> imports = new LinkedHashSet<>();
        for (String line : source.split("\n")) {
            line = line.trim();
            if (line.startsWith("import ") && line.endsWith(";")) {
                // Keep the full import text after "import " and before ";"
                String imp = line.substring(7, line.length() - 1).trim();
                imports.add(imp);
            }
        }
        return imports;
    }

    /**
     * Check if a field name appears in any changed or new method body.
     */
    private boolean isFieldReferenced(String fieldName, ForkDiffer.DiffResult diff) {
        for (MethodDeclaration m : diff.changedMethods) {
            if (bodyContainsName(m, fieldName)) return true;
        }
        for (MethodDeclaration m : diff.newMethods) {
            if (bodyContainsName(m, fieldName)) return true;
        }
        return false;
    }

    /**
     * Check if a method name is called from any changed or new method body.
     */
    private boolean isMethodReferenced(String methodName, ForkDiffer.DiffResult diff) {
        for (MethodDeclaration m : diff.changedMethods) {
            if (bodyContainsMethodCall(m, methodName)) return true;
        }
        for (MethodDeclaration m : diff.newMethods) {
            if (bodyContainsMethodCall(m, methodName)) return true;
        }
        return false;
    }

    private boolean bodyContainsName(MethodDeclaration m, String name) {
        return m.getBody().map(body -> {
            // Check for NameExpr (simple name references)
            boolean hasNameExpr = body.findAll(NameExpr.class).stream()
                .anyMatch(ne -> ne.getNameAsString().equals(name));
            // Check for FieldAccessExpr (this.name)
            boolean hasFieldAccess = body.findAll(FieldAccessExpr.class).stream()
                .anyMatch(fa -> fa.getNameAsString().equals(name));
            return hasNameExpr || hasFieldAccess;
        }).orElse(false);
    }

    private boolean bodyContainsMethodCall(MethodDeclaration m, String name) {
        return m.getBody().map(body ->
            body.findAll(MethodCallExpr.class).stream()
                .anyMatch(mc -> mc.getNameAsString().equals(name))
        ).orElse(false);
    }

    private boolean hasPrivateNewMethods(ForkDiffer.DiffResult diff) {
        return diff.newMethods.stream().anyMatch(MethodDeclaration::isPrivate);
    }

    private String methodSig(MethodDeclaration m) {
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

    /**
     * Find inner/nested class declarations in the reference source and add imports for them.
     * This allows @Shadow fields/methods to reference inner class types like SkillData.
     */
    private void collectInnerClassImports(String refSource, String targetFqcn, Set<String> imports) {
        JavaParser parser = new JavaParser(new ParserConfiguration()
            .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21));
        var result = parser.parse(refSource);
        if (result.getResult().isEmpty()) return;

        var cu = result.getResult().get();
        cu.getPrimaryType()
            .or(() -> cu.getTypes().isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(cu.getTypes().get(0)))
            .ifPresent(primaryType -> {
                for (var member : primaryType.getMembers()) {
                    if (member instanceof ClassOrInterfaceDeclaration innerClass) {
                        imports.add(targetFqcn + "." + innerClass.getNameAsString());
                    } else if (member instanceof EnumDeclaration innerEnum) {
                        imports.add(targetFqcn + "." + innerEnum.getNameAsString());
                    }
                }
            });
    }

    /**
     * Parse the reference source to find how many arguments the super() call takes.
     * This is used to generate a dummy constructor when the mixin extends a superclass.
     */
    private int getSuperConstructorArgCount(String refSource) {
        JavaParser parser = new JavaParser(new ParserConfiguration()
            .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21));
        var result = parser.parse(refSource);
        if (result.getResult().isEmpty()) return 0;

        var cu = result.getResult().get();
        var primaryType = cu.getPrimaryType()
            .or(() -> cu.getTypes().isEmpty() ? java.util.Optional.empty()
                : java.util.Optional.of(cu.getTypes().get(0)));
        if (primaryType.isEmpty()) return 0;

        for (ConstructorDeclaration con : primaryType.get().getConstructors()) {
            for (var stmt : con.getBody().getStatements()) {
                if (stmt instanceof ExplicitConstructorInvocationStmt superCall) {
                    if (!superCall.isThis()) { // It's a super() call
                        return superCall.getArguments().size();
                    }
                }
            }
        }
        return 0; // No explicit super call -> parent has no-arg constructor
    }
}
