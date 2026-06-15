package dev.mrk.meshingress.codeqlscope;

import java.util.ArrayList;
import java.util.List;

public class CodeQlScopeQueryGenerator {
    public String generateQuery(List<ScopeRule> rules) {
        List<String> clauses = new ArrayList<>();
        for (ScopeRule rule : rules) {
            for (ScopeMatcher matcher : rule.matchers()) {
                if (matcher.type() != ScopeMatcherType.CODEQL_CALL) {
                    continue;
                }
                JavaOwner owner = JavaOwner.parse(matcher.owner());
                clauses.add("""
                        (
                          scope = "%s" and
                          ruleId = "%s" and
                          ma.getMethod().getDeclaringType().hasQualifiedName("%s", "%s") and
                          ma.getMethod().getName().matches("%s")
                        )""".formatted(
                        escape(rule.scope()),
                        escape(rule.id()),
                        escape(owner.packageName()),
                        escape(owner.typeName()),
                        escape(matcher.namePattern())
                ));
            }
        }

        String whereClause = clauses.isEmpty()
                ? "false"
                : String.join("\nor\n", clauses);

        return """
                /**
                 * Generated Meshingress scope inference query.
                 * Source of truth: repository scope catalog.
                 */
                import java

                from MethodAccess ma, string scope, string ruleId
                where
                %s
                select ma, scope + " inferred by " + ruleId
                """.formatted(whereClause);
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private record JavaOwner(String packageName, String typeName) {
        private static JavaOwner parse(String owner) {
            int split = owner.lastIndexOf('.');
            if (split < 0) {
                return new JavaOwner("", owner);
            }
            return new JavaOwner(owner.substring(0, split), owner.substring(split + 1));
        }
    }
}
