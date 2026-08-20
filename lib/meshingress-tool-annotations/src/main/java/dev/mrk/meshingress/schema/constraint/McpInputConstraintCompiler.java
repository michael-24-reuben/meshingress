package dev.mrk.meshingress.schema.constraint;

import dev.mrk.meshingress.api.tools.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.lang.reflect.AnnotatedElement;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;

/** Compiles type-specific constraint annotations into standard JSON Schema keywords. */
public final class McpInputConstraintCompiler {

    private McpInputConstraintCompiler() {
    }

    public static void apply(AnnotatedElement element, Class<?> valueType, ObjectNode schema) {
        applyInteger(element.getAnnotation(McpIntegerConstraints.class), valueType, schema);
        applyNumber(element.getAnnotation(McpNumberConstraints.class), valueType, schema);
        applyArray(element.getAnnotation(McpArrayConstraints.class), valueType, schema);
        applyText(element.getAnnotation(McpTextConstraints.class), valueType, schema);
        applyJson(element.getAnnotation(McpJsonConstraints.class), valueType, schema);
        applyBoolean(element.getAnnotation(McpBooleanConstraints.class), valueType, schema);
        applyDate(element.getAnnotation(McpDateConstraints.class), valueType, schema);
        applyBinary(element.getAnnotation(McpBinaryConstraints.class), valueType, schema);
        applyNull(element.getAnnotation(McpNullConstraints.class), element, schema);
        applyUrl(element.getAnnotation(McpUrlConstraints.class), valueType, schema);
        applyUnknown(element.getAnnotation(McpUnknownConstraints.class), valueType, schema);
        applySecret(element.getAnnotation(McpSecretConstraints.class), valueType, schema);
        applyEnum(element.getAnnotation(McpEnumConstraints.class), valueType, schema);
        applyCode(element.getAnnotation(McpCodeConstraints.class), valueType, schema);
        applyEmail(element.getAnnotation(McpEmailConstraints.class), valueType, schema);
        applyDecimal(element.getAnnotation(McpDecimalConstraints.class), valueType, schema);
        applyNumericSlider(element.getAnnotation(McpNumericSliderConstraints.class), valueType, schema);
        applyClock(element.getAnnotation(McpClockConstraints.class), valueType, schema);
        applyTime(element.getAnnotation(McpTimeConstraints.class), valueType, schema);
        applyDuration(element.getAnnotation(McpDurationConstraints.class), valueType, schema);
        applyInterval(element.getAnnotation(McpIntervalConstraints.class), valueType, schema);
        applyColor(element.getAnnotation(McpColorConstraints.class), valueType, schema);
        applyLocation(element.getAnnotation(McpLocationConstraints.class), valueType, schema);
        applyGeo(element.getAnnotation(McpGeoConstraints.class), valueType, schema);
        applyFile(element.getAnnotation(McpFileConstraints.class), valueType, schema);
        applyImage(element.getAnnotation(McpImageConstraints.class), valueType, schema);
    }

    private static void applyInteger(McpIntegerConstraints constraints, Class<?> type, ObjectNode schema) {
        if (constraints == null) {
            return;
        }
        require(isIntegral(type), "@McpIntegerConstraints requires an integral Java type, not " + type.getName());
        require(constraints.minimum() <= constraints.maximum(), "@McpIntegerConstraints minimum must not exceed maximum");
        require(constraints.multipleOf() >= 0, "@McpIntegerConstraints multipleOf must not be negative");
        if (constraints.minimum() != Long.MIN_VALUE) schema.put("minimum", constraints.minimum());
        if (constraints.maximum() != Long.MAX_VALUE) schema.put("maximum", constraints.maximum());
        if (constraints.multipleOf() > 0) schema.put("multipleOf", constraints.multipleOf());
        if (constraints.defaultValue() != Long.MIN_VALUE) schema.put("default", constraints.defaultValue());
    }

    private static void applyNumber(McpNumberConstraints constraints, Class<?> type, ObjectNode schema) {
        if (constraints == null) {
            return;
        }
        require(isNumber(type) && !isIntegral(type), "@McpNumberConstraints requires a non-integral numeric Java type, not " + type.getName());
        require(valid(constraints.minimum()) && valid(constraints.maximum()) && valid(constraints.multipleOf()) && valid(constraints.defaultValue()),
                "@McpNumberConstraints values must be finite numbers or unset");
        if (isSet(constraints.minimum()) && isSet(constraints.maximum())) {
            require(constraints.minimum() <= constraints.maximum(), "@McpNumberConstraints minimum must not exceed maximum");
        }
        if (isSet(constraints.multipleOf())) require(constraints.multipleOf() > 0, "@McpNumberConstraints multipleOf must be greater than zero");
        if (isSet(constraints.minimum())) schema.put("minimum", constraints.minimum());
        if (isSet(constraints.maximum())) schema.put("maximum", constraints.maximum());
        if (isSet(constraints.multipleOf())) schema.put("multipleOf", constraints.multipleOf());
        if (isSet(constraints.defaultValue())) schema.put("default", constraints.defaultValue());
    }

    private static void applyArray(McpArrayConstraints constraints, Class<?> type, ObjectNode schema) {
        if (constraints == null) {
            return;
        }
        require(type.isArray() || Collection.class.isAssignableFrom(type), "@McpArrayConstraints requires an array or Collection Java type, not " + type.getName());
        require(constraints.minItems() >= -1 && constraints.maxItems() >= -1, "@McpArrayConstraints item bounds must be -1 or greater");
        if (constraints.minItems() >= 0 && constraints.maxItems() >= 0) {
            require(constraints.minItems() <= constraints.maxItems(), "@McpArrayConstraints minItems must not exceed maxItems");
        }
        if (constraints.minItems() >= 0) schema.put("minItems", constraints.minItems());
        if (constraints.maxItems() >= 0) schema.put("maxItems", constraints.maxItems());
        if (constraints.uniqueItems()) schema.put("uniqueItems", true);
    }

    private static void applyText(McpTextConstraints constraints, Class<?> type, ObjectNode schema) {
        if (constraints == null) return;
        requireString(type, "@McpTextConstraints");
        applyTextConstraint(constraints.minLength(), constraints.maxLength(), constraints.pattern(), constraints.defaultValue(), constraints.hasDefault(), "@McpTextConstraints", schema);
    }

    private static void applyJson(McpJsonConstraints constraints, Class<?> type, ObjectNode schema) {
        if (constraints == null) return;
        requireJsonLike(type, "@McpJsonConstraints");
        applyObjectBounds(constraints.minProperties(), constraints.maxProperties(), "@McpJsonConstraints", schema);
        schema.put("format", "json");
    }

    private static void applyBoolean(McpBooleanConstraints constraints, Class<?> type, ObjectNode schema) {
        if (constraints == null) return;
        require(type.equals(Boolean.class) || type.equals(Boolean.TYPE), "@McpBooleanConstraints requires a boolean Java type, not " + type.getName());
        if (constraints.hasDefault()) schema.put("default", constraints.defaultValue());
    }

    private static void applyDate(McpDateConstraints constraints, Class<?> type, ObjectNode schema) {
        if (constraints == null) return;
        applyFormatBounds(type, constraints.minimum(), constraints.maximum(), "date", "@McpDateConstraints", schema);
    }

    private static void applyBinary(McpBinaryConstraints constraints, Class<?> type, ObjectNode schema) {
        if (constraints == null) return;
        requireString(type, "@McpBinaryConstraints");
        require(constraints.maxBytes() >= -1, "@McpBinaryConstraints maxBytes must be -1 or greater");
        schema.put("format", "binary");
        new McpFileSchemaConstraint(constraints.contentEncoding(), constraints.contentMediaType(), constraints.maxBytes()).applyTo(schema);
    }

    private static void applyNull(McpNullConstraints constraints, AnnotatedElement element, ObjectNode schema) {
        if (constraints == null) return;
        McpInputField field = element.getAnnotation(McpInputField.class);
        require(field != null && "null".equals(field.schemaType()), "@McpNullConstraints requires @McpInputField(schemaType = \"null\")");
        schema.put("type", "null");
        schema.put("format", "null");
    }

    private static void applyUrl(McpUrlConstraints constraints, Class<?> type, ObjectNode schema) {
        if (constraints == null) return;
        requireString(type, "@McpUrlConstraints");
        applyLengthBounds(constraints.minLength(), constraints.maxLength(), "@McpUrlConstraints", schema);
        putStringArray(schema, "x-mcp-allowedSchemes", constraints.allowedSchemes(), "@McpUrlConstraints allowedSchemes");
        schema.put("format", "uri");
    }

    private static void applyUnknown(McpUnknownConstraints constraints, Class<?> type, ObjectNode schema) {
        if (constraints == null) return;
        require(type.equals(Object.class) || JsonNode.class.isAssignableFrom(type), "@McpUnknownConstraints requires Object or JsonNode, not " + type.getName());
        putStringArray(schema, "x-mcp-allowedSchemaTypes", constraints.allowedSchemaTypes(), "@McpUnknownConstraints allowedSchemaTypes");
        schema.put("format", "unknown");
    }

    private static void applySecret(McpSecretConstraints constraints, Class<?> type, ObjectNode schema) {
        if (constraints == null) return;
        requireString(type, "@McpSecretConstraints");
        applyLengthBounds(constraints.minLength(), constraints.maxLength(), "@McpSecretConstraints", schema);
        schema.put("format", "password");
    }

    private static void applyEnum(McpEnumConstraints constraints, Class<?> type, ObjectNode schema) {
        if (constraints == null) return;
        require(type.isEnum(), "@McpEnumConstraints requires a Java enum type, not " + type.getName());
        if (!constraints.defaultValue().isBlank()) {
            boolean matches = java.util.Arrays.stream(type.getEnumConstants())
                    .map(Enum.class::cast)
                    .anyMatch(value -> value.name().equals(constraints.defaultValue()));
            require(matches, "@McpEnumConstraints defaultValue must name a member of " + type.getName());
            schema.put("default", constraints.defaultValue());
        }
    }

    private static void applyCode(McpCodeConstraints constraints, Class<?> type, ObjectNode schema) {
        if (constraints == null) return;
        requireString(type, "@McpCodeConstraints");
        applyLengthBounds(constraints.minLength(), constraints.maxLength(), "@McpCodeConstraints", schema);
        if (!constraints.language().isBlank()) schema.put("x-mcp-language", constraints.language());
        schema.put("format", "code");
    }

    private static void applyEmail(McpEmailConstraints constraints, Class<?> type, ObjectNode schema) {
        if (constraints == null) return;
        requireString(type, "@McpEmailConstraints");
        applyLengthBounds(constraints.minLength(), constraints.maxLength(), "@McpEmailConstraints", schema);
        putStringArray(schema, "x-mcp-allowedDomains", constraints.allowedDomains(), "@McpEmailConstraints allowedDomains");
        schema.put("format", "email");
    }

    private static void applyDecimal(McpDecimalConstraints constraints, Class<?> type, ObjectNode schema) {
        if (constraints == null) return;
        require(type.equals(BigDecimal.class), "@McpDecimalConstraints requires java.math.BigDecimal, not " + type.getName());
        BigDecimal minimum = decimal(constraints.minimum(), "@McpDecimalConstraints minimum");
        BigDecimal maximum = decimal(constraints.maximum(), "@McpDecimalConstraints maximum");
        BigDecimal multipleOf = decimal(constraints.multipleOf(), "@McpDecimalConstraints multipleOf");
        BigDecimal defaultValue = decimal(constraints.defaultValue(), "@McpDecimalConstraints defaultValue");
        if (minimum != null && maximum != null) require(minimum.compareTo(maximum) <= 0, "@McpDecimalConstraints minimum must not exceed maximum");
        if (multipleOf != null) require(multipleOf.signum() > 0, "@McpDecimalConstraints multipleOf must be greater than zero");
        if (minimum != null) schema.put("minimum", minimum);
        if (maximum != null) schema.put("maximum", maximum);
        if (multipleOf != null) schema.put("multipleOf", multipleOf);
        if (defaultValue != null) schema.put("default", defaultValue);
    }

    private static void applyNumericSlider(McpNumericSliderConstraints constraints, Class<?> type, ObjectNode schema) {
        if (constraints == null) return;
        boolean integral = isIntegral(type);
        require(integral || isNumber(type), "@McpNumericSliderConstraints requires an integral or decimal Java type, not " + type.getName());
        require(schema.path("minimum").isNumber() && schema.path("maximum").isNumber(),
                "@McpNumericSliderConstraints requires a companion numeric constraint with minimum and maximum");
        if (!integral) {
            require(schema.path("multipleOf").isNumber(),
                    "@McpNumericSliderConstraints requires multipleOf for decimal inputs");
        }
        schema.put("x-mcp-control", "slider");
    }

    private static void applyClock(McpClockConstraints constraints, Class<?> type, ObjectNode schema) {
        if (constraints == null) return;
        applyFormatBounds(type, constraints.minimum(), constraints.maximum(), "clock", "@McpClockConstraints", schema);
    }

    private static void applyTime(McpTimeConstraints constraints, Class<?> type, ObjectNode schema) {
        if (constraints == null) return;
        applyFormatBounds(type, constraints.minimum(), constraints.maximum(), "time", "@McpTimeConstraints", schema);
    }

    private static void applyDuration(McpDurationConstraints constraints, Class<?> type, ObjectNode schema) {
        if (constraints == null) return;
        requireString(type, "@McpDurationConstraints");
        Duration minimum = duration(constraints.minimum(), "@McpDurationConstraints minimum");
        Duration maximum = duration(constraints.maximum(), "@McpDurationConstraints maximum");
        if (minimum != null && maximum != null) require(minimum.compareTo(maximum) <= 0, "@McpDurationConstraints minimum must not exceed maximum");
        if (minimum != null) schema.put("x-mcp-formatMinimum", constraints.minimum());
        if (maximum != null) schema.put("x-mcp-formatMaximum", constraints.maximum());
        schema.put("format", "duration");
    }

    private static void applyInterval(McpIntervalConstraints constraints, Class<?> type, ObjectNode schema) {
        if (constraints == null) return;
        requireString(type, "@McpIntervalConstraints");
        Duration minimum = duration(constraints.minimumDuration(), "@McpIntervalConstraints minimumDuration");
        Duration maximum = duration(constraints.maximumDuration(), "@McpIntervalConstraints maximumDuration");
        if (minimum != null && maximum != null) require(minimum.compareTo(maximum) <= 0, "@McpIntervalConstraints minimumDuration must not exceed maximumDuration");
        if (minimum != null) schema.put("x-mcp-minDuration", constraints.minimumDuration());
        if (maximum != null) schema.put("x-mcp-maxDuration", constraints.maximumDuration());
        schema.put("format", "interval");
    }

    private static void applyColor(McpColorConstraints constraints, Class<?> type, ObjectNode schema) {
        if (constraints == null) return;
        requireString(type, "@McpColorConstraints");
        putStringArray(schema, "x-mcp-allowedColorFormats", constraints.allowedFormats(), "@McpColorConstraints allowedFormats");
        schema.put("format", "color");
    }

    private static void applyLocation(McpLocationConstraints constraints, Class<?> type, ObjectNode schema) {
        if (constraints == null) return;
        requireJsonLike(type, "@McpLocationConstraints");
        putStringArray(schema, "x-mcp-allowedCountryCodes", constraints.allowedCountryCodes(), "@McpLocationConstraints allowedCountryCodes");
        schema.put("format", "location");
    }

    private static void applyGeo(McpGeoConstraints constraints, Class<?> type, ObjectNode schema) {
        if (constraints == null) return;
        requireJsonLike(type, "@McpGeoConstraints");
        applyGeoBound("minimumLatitude", constraints.minimumLatitude(), -90, 90, "x-mcp-minLatitude", schema);
        applyGeoBound("maximumLatitude", constraints.maximumLatitude(), -90, 90, "x-mcp-maxLatitude", schema);
        applyGeoBound("minimumLongitude", constraints.minimumLongitude(), -180, 180, "x-mcp-minLongitude", schema);
        applyGeoBound("maximumLongitude", constraints.maximumLongitude(), -180, 180, "x-mcp-maxLongitude", schema);
        if (isSet(constraints.minimumLatitude()) && isSet(constraints.maximumLatitude())) {
            require(constraints.minimumLatitude() <= constraints.maximumLatitude(), "@McpGeoConstraints minimumLatitude must not exceed maximumLatitude");
        }
        if (isSet(constraints.minimumLongitude()) && isSet(constraints.maximumLongitude())) {
            require(constraints.minimumLongitude() <= constraints.maximumLongitude(), "@McpGeoConstraints minimumLongitude must not exceed maximumLongitude");
        }
        schema.put("format", "geo");
    }

    private static void applyFile(McpFileConstraints constraints, Class<?> type, ObjectNode schema) {
        if (constraints != null) {
            require(type.equals(String.class), "@McpFileConstraints currently requires a String Java type, not " + type.getName());
            require(constraints.maxBytes() >= -1, "@McpFileConstraints maxBytes must be -1 or greater");
            schema.put("format", "file");
            new McpFileSchemaConstraint(constraints.contentEncoding(), constraints.contentMediaType(), constraints.maxBytes()).applyTo(schema);
        }
    }

    private static void applyImage(McpImageConstraints constraints, Class<?> type, ObjectNode schema) {
        if (constraints != null) {
            require(type.equals(String.class), "@McpImageConstraints currently requires a String Java type, not " + type.getName());
            require(constraints.maxBytes() >= -1, "@McpImageConstraints maxBytes must be -1 or greater");
            schema.put("format", "image");
            new McpImageSchemaConstraint(constraints.contentEncoding(), constraints.contentMediaType(), constraints.maxBytes()).applyTo(schema);
        }
    }

    private static void applyTextConstraint(int minLength, int maxLength, String pattern, String defaultValue, boolean hasDefault, String name, ObjectNode schema) {
        applyLengthBounds(minLength, maxLength, name, schema);
        new McpTextSchemaConstraint(minLength, maxLength, pattern, defaultValue, hasDefault).applyTo(schema);
    }

    private static void applyLengthBounds(int minLength, int maxLength, String name, ObjectNode schema) {
        require(minLength >= -1 && maxLength >= -1, name + " length bounds must be -1 or greater");
        if (minLength >= 0 && maxLength >= 0) require(minLength <= maxLength, name + " minLength must not exceed maxLength");
        if (minLength >= 0) schema.put("minLength", minLength);
        if (maxLength >= 0) schema.put("maxLength", maxLength);
    }

    private static void applyObjectBounds(int minProperties, int maxProperties, String name, ObjectNode schema) {
        require(minProperties >= -1 && maxProperties >= -1, name + " property bounds must be -1 or greater");
        if (minProperties >= 0 && maxProperties >= 0) require(minProperties <= maxProperties, name + " minProperties must not exceed maxProperties");
        if (minProperties >= 0) schema.put("minProperties", minProperties);
        if (maxProperties >= 0) schema.put("maxProperties", maxProperties);
    }

    private static void applyFormatBounds(Class<?> type, String minimum, String maximum, String format, String name, ObjectNode schema) {
        requireString(type, name);
        if (!minimum.isBlank() && !maximum.isBlank()) require(minimum.compareTo(maximum) <= 0, name + " minimum must not exceed maximum");
        if (!minimum.isBlank()) schema.put("x-mcp-formatMinimum", minimum);
        if (!maximum.isBlank()) schema.put("x-mcp-formatMaximum", maximum);
        schema.put("format", format);
    }

    private static void applyGeoBound(String valueName, double value, double minimum, double maximum, String schemaName, ObjectNode schema) {
        require(valid(value), "@McpGeoConstraints " + valueName + " must be finite or unset");
        if (isSet(value)) {
            require(value >= minimum && value <= maximum, "@McpGeoConstraints " + valueName + " must be between " + minimum + " and " + maximum);
            schema.put(schemaName, value);
        }
    }

    private static void putStringArray(ObjectNode schema, String schemaName, String[] values, String name) {
        if (values.length == 0) return;
        var output = schema.putArray(schemaName);
        for (String value : values) {
            require(value != null && !value.isBlank(), name + " must not contain blank values");
            output.add(value);
        }
    }

    private static BigDecimal decimal(String value, String name) {
        if (value.isBlank()) return null;
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException(name + " must be a valid decimal", exception);
        }
    }

    private static Duration duration(String value, String name) {
        if (value.isBlank()) return null;
        try {
            return Duration.parse(value);
        } catch (RuntimeException exception) {
            throw new IllegalStateException(name + " must be an ISO-8601 duration", exception);
        }
    }

    private static void requireString(Class<?> type, String name) {
        require(type.equals(String.class), name + " requires a String Java type, not " + type.getName());
    }

    private static void requireJsonLike(Class<?> type, String name) {
        require(JsonNode.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type), name + " requires JsonNode or Map, not " + type.getName());
    }

    private static boolean isIntegral(Class<?> type) {
        return type.equals(Integer.class) || type.equals(Integer.TYPE)
                || type.equals(Long.class) || type.equals(Long.TYPE)
                || type.equals(Short.class) || type.equals(Short.TYPE)
                || type.equals(Byte.class) || type.equals(Byte.TYPE);
    }

    private static boolean isNumber(Class<?> type) {
        return Number.class.isAssignableFrom(type)
                || type.equals(Double.TYPE) || type.equals(Float.TYPE);
    }

    private static boolean valid(double value) {
        return Double.isNaN(value) || Double.isFinite(value);
    }

    private static boolean isSet(double value) {
        return !Double.isNaN(value);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
