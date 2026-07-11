package io.github.jvmspec.compatibility.language;

/**
 * Test-only source of truth for final Java constructs and cross-cutting fidelity concerns tracked by
 * the language coverage manifest.
 */
enum JavaLanguageConstruct {
    JAVA8_CLASS("java8-class", "java8"),
    JAVA8_FINAL_CLASS("java8-final-class", "java8"),
    JAVA8_INTERFACE("java8-interface", "java8"),
    JAVA8_ENUM("java8-enum", "java8"),
    JAVA8_ANNOTATION("java8-annotation", "java8"),
    JAVA8_LAMBDA("java8-lambda", "java8"),
    JAVA8_SPEC_LAMBDA_TARGET_INFERENCE("java8-spec-lambda-target-inference", "java8"),
    JAVA8_METHOD_REFERENCE("java8-method-reference", "java8"),
    JAVA8_INTERFACE_DEFAULT_STATIC("java8-interface-default-static", "java8"),
    JAVA8_TYPE_USE_REPEATABLE_ANNOTATIONS("java8-type-use-repeatable-annotations", "java8"),
    JAVA8_NESTED_LOCAL_ANONYMOUS_TYPES("java8-nested-local-anonymous-types", "java8"),
    JAVA8_GENERIC_BOUNDS_WILDCARDS_ARRAYS_VARARGS("java8-generic-bounds-wildcards-arrays-varargs", "java8"),

    JAVA11_LOCAL_VAR("java11-local-var", "java11"),
    JAVA11_VAR_LAMBDA_PARAMETERS("java11-var-lambda-parameters", "java11"),
    JAVA11_PRIVATE_INTERFACE_METHODS("java11-private-interface-methods", "java11"),
    JAVA11_DIAMOND_ANONYMOUS_CLASS("java11-diamond-anonymous-class", "java11"),
    JAVA11_EFFECTIVELY_FINAL_TRY_RESOURCES("java11-effectively-final-try-resources", "java11"),
    JAVA11_MODULE_INFO("java11-module-info", "java11"),
    JAVA11_APIS("java11-apis", "java11"),

    JAVA17_RECORDS("java17-records", "java17"),
    JAVA17_SEALED_TYPES("java17-sealed-types", "java17"),
    JAVA17_TOP_LEVEL_NON_SEALED("java17-top-level-non-sealed", "java17"),
    JAVA17_ABSTRACT_CLASS("java17-abstract-class", "java17"),
    JAVA17_TEXT_BLOCKS("java17-text-blocks", "java17"),
    JAVA17_SWITCH_EXPRESSIONS("java17-switch-expressions", "java17"),
    JAVA17_INSTANCEOF_PATTERNS("java17-instanceof-patterns", "java17"),
    JAVA17_APIS("java17-apis", "java17"),

    JAVA21_RECORD_PATTERNS("java21-record-patterns", "java21"),
    JAVA21_PATTERN_SWITCH("java21-pattern-switch", "java21"),
    JAVA21_GUARDED_WHEN("java21-guarded-when", "java21"),
    JAVA21_RECORD_SEALED_EXHAUSTIVENESS("java21-record-sealed-exhaustiveness", "java21"),
    JAVA21_SEQUENCED_COLLECTIONS("java21-sequenced-collections", "java21"),
    JAVA21_VIRTUAL_THREADS("java21-virtual-threads", "java21"),

    JAVA25_UNNAMED_VARIABLES_PATTERNS("java25-unnamed-variables-patterns", "java25"),
    JAVA25_FLEXIBLE_CONSTRUCTOR_BODIES("java25-flexible-constructor-bodies", "java25"),
    JAVA25_MODULE_IMPORT_DECLARATIONS("java25-module-import-declarations", "java25"),
    JAVA25_COMPACT_SOURCE_INSTANCE_MAIN("java25-compact-source-instance-main", "java25"),
    JAVA25_MARKDOWN_DOC_COMMENTS("java25-markdown-doc-comments", "java25"),
    JAVA25_STREAM_GATHERERS("java25-stream-gatherers", "java25"),

    PARSER_MULTIPLE_TOP_LEVEL_TYPES("parser-multiple-top-level-types", "all"),
    PARSER_COMMENT_LITERAL_OFFSETS("parser-comment-literal-offsets", "all"),

    FIDELITY_CRLF("fidelity-crlf", "all"),
    FIDELITY_NO_FINAL_NEWLINE("fidelity-no-final-newline", "all"),
    FIDELITY_UTF8_BOM("fidelity-utf8-bom", "all"),
    FIDELITY_UNICODE_IDENTIFIERS("fidelity-unicode-identifiers", "all"),
    FIDELITY_FORMATTING_INDENTATION("fidelity-formatting-indentation", "all"),
    FIDELITY_DRY_RUN_PARITY("fidelity-dry-run-parity", "all"),
    FIDELITY_ATOMICITY("fidelity-atomicity", "all"),
    FIDELITY_IDEMPOTENCE("fidelity-idempotence", "all"),
    FIDELITY_DIAGNOSTICS("fidelity-diagnostics", "all");

    private final String id;
    private final String profile;

    JavaLanguageConstruct(String id, String profile) {
        this.id = id;
        this.profile = profile;
    }

    String id() {
        return id;
    }

    String profile() {
        return profile;
    }
}
