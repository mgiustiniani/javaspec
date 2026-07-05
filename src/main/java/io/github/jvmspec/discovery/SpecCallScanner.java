package io.github.jvmspec.discovery;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreeScanner;

import javax.lang.model.element.Modifier;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Language-aware front-end for spec discovery.
 *
 * <p>Parses spec sources with the JDK compiler in parse-only mode (no symbol resolution, so
 * references to not-yet-generated types are fine) and reports the exact call expressions that
 * discovery cares about. Comments, string literals, nesting and multi-line expressions are
 * understood the way the Java language defines them, eliminating the false positives of a
 * text-based scan.</p>
 *
 * <p>{@link #scan(String)} returns {@code null} when no system compiler is available or the
 * source cannot be parsed; callers then fall back to the legacy text-based extraction.</p>
 */
final class SpecCallScanner {
    private static final Set<String> MATCHER_NAMES = new HashSet<String>(Arrays.asList(
            "shouldReturn", "shouldNotReturn", "shouldBe", "shouldNotBe", "shouldEqual",
            "shouldNotEqual", "shouldBeLike", "shouldNotBeLike", "shouldBeEqualTo",
            "shouldNotBeEqualTo", "shouldHaveType", "shouldBeAnInstanceOf",
            "shouldReturnAnInstanceOf", "shouldImplement", "shouldContain", "shouldNotContain",
            "shouldStartWith", "shouldNotStartWith", "shouldEndWith", "shouldNotEndWith",
            "shouldMatchPattern", "shouldNotMatchPattern", "shouldHaveCount", "shouldBeEmpty",
            "shouldNotBeEmpty", "shouldHaveKey", "shouldNotHaveKey", "shouldHaveValue",
            "shouldNotHaveValue"
    ));

    private SpecCallScanner() {
    }

    /** A call event: target name, argument expression texts, and the enclosing spec method. */
    static final class Call {
        final String name;
        final List<String> argumentTexts;
        final String enclosingMethod;

        Call(String name, List<String> argumentTexts, String enclosingMethod) {
            this.name = name;
            this.argumentTexts = argumentTexts;
            this.enclosingMethod = enclosingMethod;
        }
    }

    /** A proxy expectation event: {@code name(args).matcher(expectations)}. */
    static final class Expectation {
        final String name;
        final List<String> argumentTexts;
        final String matcherName;
        final List<String> expectationTexts;
        final String enclosingMethod;

        Expectation(
                String name,
                List<String> argumentTexts,
                String matcherName,
                List<String> expectationTexts,
                String enclosingMethod
        ) {
            this.name = name;
            this.argumentTexts = argumentTexts;
            this.matcherName = matcherName;
            this.expectationTexts = expectationTexts;
            this.enclosingMethod = enclosingMethod;
        }
    }

    /** Declared parameters of a spec method: raw (unresolved) type texts and names. */
    static final class SpecMethodParams {
        final List<String> typeTexts;
        final List<String> names;

        SpecMethodParams(List<String> typeTexts, List<String> names) {
            this.typeTexts = typeTexts;
            this.names = names;
        }
    }

    /** Everything the extraction phase needs, collected in one AST pass. */
    static final class ScanResult {
        final List<Call> constructionCalls = new ArrayList<Call>();
        final List<Call> factoryCalls = new ArrayList<Call>();
        final List<Expectation> proxyExpectations = new ArrayList<Expectation>();
        final List<Expectation> matchSubjectExpectations = new ArrayList<Expectation>();
        final List<Call> throwDuringCalls = new ArrayList<Call>();
        final List<Call> subjectVoidStatements = new ArrayList<Call>();
        final List<Call> setterStatements = new ArrayList<Call>();
        final Map<String, SpecMethodParams> specMethods = new LinkedHashMap<String, SpecMethodParams>();
    }

    /**
     * Parses the source and collects call events. Returns {@code null} when parsing is not
     * possible, signalling the caller to use the legacy text-based extraction.
     */
    static ScanResult scan(String source) {
        CompilationUnitTree unit = parseUnit(source);
        if (unit == null) {
            return null;
        }
        ScanResult result = new ScanResult();
        new CallVisitor(result).scan(unit, null);
        return result;
    }

    /**
     * Parses a single Java source in parse-only mode (no symbol resolution). Returns {@code null}
     * when no system compiler is available or the source cannot be parsed.
     */
    static CompilationUnitTree parseUnit(String source) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            return null;
        }
        try {
            JavaFileObject file = new StringJavaFileObject(source);
            JavaCompiler.CompilationTask task = compiler.getTask(
                    null, null, new SilentDiagnosticListener(), null, null, Arrays.asList(file));
            if (!(task instanceof JavacTask)) {
                return null;
            }
            for (CompilationUnitTree unit : ((JavacTask) task).parse()) {
                return unit;
            }
            return null;
        } catch (Exception ex) {
            return null;
        } catch (LinkageError ex) {
            return null;
        }
    }

    private static final class CallVisitor extends TreeScanner<Void, Void> {
        private final ScanResult result;
        private String currentMethod;

        CallVisitor(ScanResult result) {
            this.result = result;
        }

        @Override
        public Void visitMethod(MethodTree methodTree, Void unused) {
            String previousMethod = currentMethod;
            currentMethod = methodTree.getName().toString();
            if (isPublicVoid(methodTree)) {
                List<String> typeTexts = new ArrayList<String>();
                List<String> names = new ArrayList<String>();
                List<? extends VariableTree> parameters = methodTree.getParameters();
                for (int i = 0; i < parameters.size(); i++) {
                    VariableTree parameter = parameters.get(i);
                    typeTexts.add(parameter.getType().toString());
                    names.add(parameter.getName().toString());
                }
                result.specMethods.put(currentMethod, new SpecMethodParams(typeTexts, names));
            }
            try {
                return super.visitMethod(methodTree, unused);
            } finally {
                currentMethod = previousMethod;
            }
        }

        @Override
        public Void visitExpressionStatement(ExpressionStatementTree statement, Void unused) {
            ExpressionTree expression = statement.getExpression();
            if (expression instanceof MethodInvocationTree) {
                MethodInvocationTree invocation = (MethodInvocationTree) expression;
                ExpressionTree select = invocation.getMethodSelect();
                if (select instanceof MemberSelectTree) {
                    MemberSelectTree memberSelect = (MemberSelectTree) select;
                    String memberName = memberSelect.getIdentifier().toString();
                    if (isSubjectInvocation(memberSelect.getExpression()) && !isIgnoredName(memberName)) {
                        result.subjectVoidStatements.add(
                                new Call(memberName, argumentTexts(invocation), currentMethod));
                    }
                } else if (select instanceof IdentifierTree) {
                    String name = ((IdentifierTree) select).getName().toString();
                    if (isSetterName(name)) {
                        result.setterStatements.add(
                                new Call(name, argumentTexts(invocation), currentMethod));
                    }
                }
            }
            return super.visitExpressionStatement(statement, unused);
        }

        @Override
        public Void visitMethodInvocation(MethodInvocationTree invocation, Void unused) {
            ExpressionTree select = invocation.getMethodSelect();
            if (select instanceof IdentifierTree) {
                String name = ((IdentifierTree) select).getName().toString();
                if ("beConstructedWith".equals(name)) {
                    result.constructionCalls.add(
                            new Call(name, argumentTexts(invocation), currentMethod));
                } else if ("beConstructedThrough".equals(name)
                        || "beConstructedThroughNamed".equals(name)
                        || "beConstructedNamed".equals(name)) {
                    result.factoryCalls.add(
                            new Call(name, argumentTexts(invocation), currentMethod));
                }
            } else if (select instanceof MemberSelectTree) {
                MemberSelectTree memberSelect = (MemberSelectTree) select;
                String memberName = memberSelect.getIdentifier().toString();
                ExpressionTree receiver = memberSelect.getExpression();
                if (MATCHER_NAMES.contains(memberName) && receiver instanceof MethodInvocationTree) {
                    recordExpectation((MethodInvocationTree) receiver, memberName, invocation);
                } else if (memberName.startsWith("during")
                        && memberName.length() > "during".length()
                        && Character.isUpperCase(memberName.charAt("during".length()))
                        && isUnqualifiedInvocationOf(receiver, "shouldThrow")) {
                    String duringTarget = memberName.substring("during".length());
                    result.throwDuringCalls.add(
                            new Call(duringTarget, argumentTexts(invocation), currentMethod));
                }
            }
            return super.visitMethodInvocation(invocation, unused);
        }

        private void recordExpectation(
                MethodInvocationTree receiver,
                String matcherName,
                MethodInvocationTree matcherInvocation
        ) {
            ExpressionTree receiverSelect = receiver.getMethodSelect();
            if (!(receiverSelect instanceof IdentifierTree)) {
                return;
            }
            String receiverName = ((IdentifierTree) receiverSelect).getName().toString();
            if ("match".equals(receiverName)) {
                if (receiver.getArguments().size() != 1) {
                    return;
                }
                ExpressionTree matchArgument = receiver.getArguments().get(0);
                if (!(matchArgument instanceof MethodInvocationTree)) {
                    return;
                }
                MethodInvocationTree subjectCall = (MethodInvocationTree) matchArgument;
                ExpressionTree subjectSelect = subjectCall.getMethodSelect();
                if (!(subjectSelect instanceof MemberSelectTree)) {
                    return;
                }
                MemberSelectTree subjectMemberSelect = (MemberSelectTree) subjectSelect;
                if (!isSubjectInvocation(subjectMemberSelect.getExpression())) {
                    return;
                }
                String subjectMethodName = subjectMemberSelect.getIdentifier().toString();
                if (isIgnoredName(subjectMethodName) || !startsLowerCase(subjectMethodName)) {
                    return;
                }
                result.matchSubjectExpectations.add(new Expectation(
                        subjectMethodName,
                        argumentTexts(subjectCall),
                        matcherName,
                        argumentTexts(matcherInvocation),
                        currentMethod
                ));
                return;
            }
            if (isIgnoredName(receiverName) || !startsLowerCase(receiverName)) {
                return;
            }
            result.proxyExpectations.add(new Expectation(
                    receiverName,
                    argumentTexts(receiver),
                    matcherName,
                    argumentTexts(matcherInvocation),
                    currentMethod
            ));
        }

        private static boolean isSubjectInvocation(ExpressionTree expression) {
            return isUnqualifiedInvocationOf(expression, "subject");
        }

        private static boolean isUnqualifiedInvocationOf(ExpressionTree expression, String name) {
            if (!(expression instanceof MethodInvocationTree)) {
                return false;
            }
            ExpressionTree select = ((MethodInvocationTree) expression).getMethodSelect();
            return select instanceof IdentifierTree
                    && name.equals(((IdentifierTree) select).getName().toString());
        }

        private static boolean isPublicVoid(MethodTree methodTree) {
            if (!methodTree.getModifiers().getFlags().contains(Modifier.PUBLIC)) {
                return false;
            }
            Tree returnType = methodTree.getReturnType();
            return returnType instanceof PrimitiveTypeTree
                    && "void".equals(returnType.toString());
        }

        private static boolean isSetterName(String name) {
            return name.startsWith("set")
                    && name.length() > 3
                    && Character.isUpperCase(name.charAt(3));
        }

        private static boolean isIgnoredName(String methodName) {
            return "match".equals(methodName)
                    || "subject".equals(methodName)
                    || methodName.startsWith("should")
                    || methodName.startsWith("beConstructed")
                    || "matcherRegistry".equals(methodName);
        }

        private static boolean startsLowerCase(String name) {
            return name.length() > 0 && Character.isLowerCase(name.charAt(0));
        }

        private static List<String> argumentTexts(MethodInvocationTree invocation) {
            List<String> texts = new ArrayList<String>();
            List<? extends ExpressionTree> arguments = invocation.getArguments();
            for (int i = 0; i < arguments.size(); i++) {
                texts.add(arguments.get(i).toString());
            }
            return texts;
        }
    }

    private static final class StringJavaFileObject extends SimpleJavaFileObject {
        private final String source;

        StringJavaFileObject(String source) {
            super(URI.create("string:///ParsedSpec.java"), Kind.SOURCE);
            this.source = source;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }

    private static final class SilentDiagnosticListener
            implements javax.tools.DiagnosticListener<JavaFileObject> {
        public void report(javax.tools.Diagnostic<? extends JavaFileObject> diagnostic) {
            // Parse-only scan: resolution diagnostics are expected and irrelevant.
        }
    }
}
