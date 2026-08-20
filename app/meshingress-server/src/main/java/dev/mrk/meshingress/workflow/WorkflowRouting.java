package dev.mrk.meshingress.workflow;

import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/** Chooses one control port from a typed node result. Ordered cases use first-match-wins semantics. */
public sealed interface WorkflowRouting permits WorkflowRouting.BooleanPorts, WorkflowRouting.Cases {

    String selectPort(JsonNode result);

    List<String> ports();

    record BooleanPorts(String truePort, String falsePort) implements WorkflowRouting {
        public BooleanPorts {
            WorkflowDefinition.requireText(truePort, "true port");
            WorkflowDefinition.requireText(falsePort, "false port");
        }

        @Override
        public String selectPort(JsonNode result) {
            if (!result.isBoolean()) {
                throw new WorkflowValidationException("Boolean routing requires a boolean result");
            }
            return result.asBoolean() ? truePort : falsePort;
        }

        @Override
        public List<String> ports() {
            return List.of(truePort, falsePort);
        }
    }

    record Cases(String subjectPointer, List<RouteCase> cases, String defaultPort) implements WorkflowRouting {
        public Cases {
            subjectPointer = subjectPointer == null ? "" : subjectPointer;
            if (!subjectPointer.isEmpty() && !subjectPointer.startsWith("/")) {
                throw new IllegalArgumentException("routing subject pointer must be empty or start with '/'");
            }
            cases = List.copyOf(Objects.requireNonNull(cases, "routing cases must not be null"));
            WorkflowDefinition.requireText(defaultPort, "default port");
        }

        @Override
        public String selectPort(JsonNode result) {
            JsonNode subject = subjectPointer.isEmpty() ? result : result.at(subjectPointer);
            if (subject.isMissingNode()) {
                throw new WorkflowValidationException("Routing subject does not exist: " + subjectPointer);
            }
            for (RouteCase routeCase : cases) {
                if (routeCase.when().matches(subject)) {
                    return routeCase.port();
                }
            }
            return defaultPort;
        }

        @Override
        public List<String> ports() {
            java.util.ArrayList<String> ports = new java.util.ArrayList<>();
            for (RouteCase routeCase : cases) {
                ports.add(routeCase.port());
            }
            ports.add(defaultPort);
            return List.copyOf(ports);
        }
    }

    record RouteCase(String port, WorkflowPredicate when) {
        public RouteCase {
            WorkflowDefinition.requireText(port, "case port");
            when = Objects.requireNonNull(when, "case predicate must not be null");
        }
    }

    sealed interface WorkflowPredicate permits StringEquals, StringMatches, NumberComparison, ArrayContains {
        boolean matches(JsonNode value);
    }

    record StringEquals(String expected) implements WorkflowPredicate {
        public StringEquals {
            expected = Objects.requireNonNull(expected, "expected string must not be null");
        }

        @Override
        public boolean matches(JsonNode value) {
            return value.isString() && expected.equals(value.asString());
        }
    }

    record StringMatches(String pattern) implements WorkflowPredicate {
        public StringMatches {
            WorkflowDefinition.requireText(pattern, "regex pattern");
        }

        @Override
        public boolean matches(JsonNode value) {
            return value.isString() && Pattern.compile(pattern).matcher(value.asString()).matches();
        }
    }

    record NumberComparison(NumberOperator operator, BigDecimal expected) implements WorkflowPredicate {
        public NumberComparison {
            operator = Objects.requireNonNull(operator, "number operator must not be null");
            expected = Objects.requireNonNull(expected, "expected number must not be null");
        }

        @Override
        public boolean matches(JsonNode value) {
            if (!value.isNumber()) {
                return false;
            }
            int comparison = value.decimalValue().compareTo(expected);
            return switch (operator) {
                case EQ -> comparison == 0;
                case NEQ -> comparison != 0;
                case LT -> comparison < 0;
                case LTE -> comparison <= 0;
                case GT -> comparison > 0;
                case GTE -> comparison >= 0;
            };
        }
    }

    record ArrayContains(JsonNode expected) implements WorkflowPredicate {
        public ArrayContains {
            expected = Objects.requireNonNull(expected, "expected array value must not be null").deepCopy();
        }

        @Override
        public boolean matches(JsonNode value) {
            if (!value.isArray()) {
                return false;
            }
            for (JsonNode item : value) {
                if (expected.equals(item)) {
                    return true;
                }
            }
            return false;
        }
    }

    enum NumberOperator {
        EQ,
        NEQ,
        LT,
        LTE,
        GT,
        GTE
    }
}
