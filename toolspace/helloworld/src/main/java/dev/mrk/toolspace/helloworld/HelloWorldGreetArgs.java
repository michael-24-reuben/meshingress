package dev.mrk.toolspace.helloworld;

import dev.mrk.meshingress.api.tools.annotation.*;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.List;

/**
 * A broad, optional argument fixture for exercising each Studio parameter object type.
 * Only {@code name} is needed to produce a greeting.
 */
@McpInputSchema(description = "Build a greeting with optional tone, audience, timing, and presentation details.")
public final class HelloWorldGreetArgs {

    @McpInputField(description = "Person or group to greet.", required = true)
    @McpTextConstraints(minLength = 1, maxLength = 120)
    public String name;

    @McpInputField(description = "Optional opening phrase, such as Hello or Welcome.", required = false)
    @McpTextConstraints(minLength = 1, maxLength = 160)
    public String text;

    @McpInputField(description = "Optional enthusiasm level used to vary the greeting punctuation.", required = false)
    @McpIntegerConstraints(minimum = 1, maximum = 5, defaultValue = 1)
    @McpNumericSliderConstraints
    public Integer number;

    @McpInputField(description = "Optional JSON persona details to acknowledge.", required = false, format = "json")
    @McpJsonConstraints(minProperties = 1, maxProperties = 12)
    public JsonNode json;

    @McpInputField(description = "Optional audience members to greet alongside the named person.", required = false)
    @McpArrayConstraints(minItems = 1, maxItems = 8, uniqueItems = true)
    public List<String> array;

    @McpInputField(description = "Use a formal greeting when true; use a casual greeting when false.", title = "Boolean value", required = false)
    @McpBooleanConstraints(defaultValue = false, hasDefault = true)
    public Boolean booleanValue;

    @McpInputField(description = "Optional occasion date in YYYY-MM-DD form.", required = false, format = "date")
    @McpDateConstraints(minimum = "1900-01-01", maximum = "2100-12-31")
    public String date;

    @McpInputField(description = "Optional Base64 ceremony seal; its presence adds a sealed note.", required = false, format = "binary")
    @McpBinaryConstraints(contentEncoding = "base64", maxBytes = 4096)
    public String binary;

    @McpInputField(description = "Optional explicit null marker; it requests a deliberate silent beat.", required = false, schemaType = "null", format = "null")
    @McpNullConstraints
    public JsonNode nullValue;

    @McpInputField(description = "Optional URL to cite as the greeting's inspiration.", required = false, format = "uri")
    @McpUrlConstraints(maxLength = 2048, allowedSchemes = {"https", "http"})
    public String url;

    @McpInputField(description = "Optional file reference to mention as an attachment.", required = false, format = "file")
    @McpFileConstraints(maxBytes = 10_485_760)
    public String file;

    @McpInputField(description = "Optional untyped value to acknowledge without interpreting.", required = false, format = "unknown")
    @McpUnknownConstraints(allowedSchemaTypes = {"string", "number", "integer", "boolean", "object", "array", "null"})
    public Object unknown;

    @McpInputField(description = "Optional private sign-off; its value is never returned.", required = false, format = "password")
    @McpSecretConstraints(minLength = 8, maxLength = 256)
    public String secret;

    @McpInputField(description = "Optional greeting style.", required = false)
    @McpEnumConstraints(defaultValue = "sunny")
    public GreetingStyle enumValue;

    @McpInputField(description = "Optional code phrase that turns the greeting into a developer salutation.", required = false, format = "code")
    @McpCodeConstraints(language = "java", minLength = 1, maxLength = 5_000)
    public String code;

    @McpInputField(description = "Optional reply address to include in the greeting.", required = false, format = "email")
    @McpEmailConstraints(maxLength = 254)
    public String email;

    @McpInputField(description = "Optional decimal warmth multiplier used in the tone note.", required = false)
    @McpDecimalConstraints(minimum = "0.0", maximum = "10.0", multipleOf = "0.1", defaultValue = "1.0")
    @McpNumericSliderConstraints
    public BigDecimal decimal;

    @McpInputField(description = "Optional image reference used as the greeting backdrop.", required = false, format = "image")
    @McpImageConstraints(maxBytes = 5_242_880)
    public String image;

    @McpInputField(description = "Optional clock reading that makes the greeting time-aware.", required = false, format = "clock")
    @McpClockConstraints(minimum = "00:00", maximum = "23:59")
    public String clock;

    @McpInputField(description = "Optional meeting time in HH:mm form.", required = false, format = "time")
    @McpTimeConstraints(minimum = "00:00", maximum = "23:59")
    public String time;

    @McpInputField(description = "Optional duration to describe the intended pause or celebration.", required = false, format = "duration")
    @McpDurationConstraints(minimum = "PT0S", maximum = "PT24H")
    public String duration;

    @McpInputField(description = "Optional availability interval to include in the greeting.", required = false, format = "interval")
    @McpIntervalConstraints(minimumDuration = "PT0S", maximumDuration = "PT24H")
    public String interval;

    @McpInputField(description = "Optional color that gives the greeting a visual mood.", required = false, format = "color")
    @McpColorConstraints(allowedFormats = {"hex", "rgb", "hsl"})
    public String color;

    @McpInputField(description = "Optional named location to mention in the greeting.", required = false, format = "location")
    @McpLocationConstraints
    public JsonNode location;

    @McpInputField(description = "Optional geographic coordinates or feature to acknowledge.", required = false, format = "geo")
    @McpGeoConstraints(minimumLatitude = -90, maximumLatitude = 90, minimumLongitude = -180, maximumLongitude = 180)
    public JsonNode geo;

    public enum GreetingStyle {
        sunny,
        cosmic,
        pirate,
        understated
    }
}
