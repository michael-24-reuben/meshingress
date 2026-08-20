package dev.mrk.meshingress.toolcatalog;

import dev.mrk.meshingress.toolmetadata.ToolIcon;
import dev.mrk.meshingress.toolmetadata.ToolLink;
import dev.mrk.meshingress.toolmetadata.ToolProperty;
import dev.mrk.meshingress.toolmetadata.ToolRequirement;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** Public, runtime-facing manifest catalog for active tool modules. */
@RestController
@RequestMapping("/api/v1/tool-modules")
public class ToolModuleCatalogController {

    private static final MediaType MARKDOWN = new MediaType("text", "markdown", StandardCharsets.UTF_8);
    private final ToolModuleCatalog catalog;

    public ToolModuleCatalogController(ToolModuleCatalog catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    public ModuleListResponse list() {
        return new ModuleListResponse(catalog.list().stream().map(this::summary).toList());
    }

    @GetMapping("/{toolId}")
    public ModuleDetailResponse detail(@PathVariable String toolId) {
        return detailFor(toolId);
    }

    @GetMapping(value = "/{toolId}/readme", produces = "text/markdown")
    public ResponseEntity<String> readme(@PathVariable String toolId) {
        ToolModuleCatalog.Entry entry = entry(toolId);
        if (entry.readme().isBlank()) throw notFound("tool module does not declare a README");
        return ResponseEntity.ok().contentType(MARKDOWN).body(entry.readme());
    }

    @GetMapping("/{toolId}/icon")
    public ResponseEntity<Resource> icon(@PathVariable String toolId) {
        ToolModuleCatalog.Entry entry = entry(toolId);
        ToolIcon icon = entry.icon().orElseThrow(() -> notFound("tool module does not declare an icon"));
        Resource resource = entry.iconResource();
        try {
            long length = resource.contentLength();
            if (length < 0 || length > ToolIcon.MAX_BYTES) throw notFound("tool module icon is unavailable");
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(icon.mimeType().value())).contentLength(length).body(resource);
        } catch (Exception exception) {
            throw notFound("tool module icon is unavailable");
        }
    }

    private ModuleDetailResponse detailFor(String toolId) {
        ToolModuleCatalog.Entry entry = entry(toolId);
        var manifest = entry.manifest();
        return new ModuleDetailResponse(
                entry.toolId(),
                entry.source(),
                manifest.metadata().namespace(),
                metadata(entry),
                manifest.properties().stream().map(this::property).toList(),
                manifest.requirements().stream().map(this::requirement).toList(),
                new ReadmeLink(readmeHref(entry.toolId()), "text/markdown", !entry.readme().isBlank())
        );
    }

    private ModuleSummary summary(ToolModuleCatalog.Entry entry) {
        return new ModuleSummary(entry.toolId(), entry.source(), entry.namespace(), entry.manifest().metadata().title(), icon(entry), detailHref(entry.toolId()));
    }

    private ModuleMetadata metadata(ToolModuleCatalog.Entry entry) {
        var metadata = entry.manifest().metadata();
        return new ModuleMetadata(
                metadata.title(), metadata.summary(), metadata.description(),
                metadata.authors().stream().map(author -> new Author(author.name(), author.urls())).toList(),
                metadata.license(), metadata.tags(),
                metadata.links().stream().map(this::link).toList(), icon(entry)
        );
    }

    private IconLink icon(ToolModuleCatalog.Entry entry) {
        return entry.icon().map(icon -> new IconLink(iconHref(entry.toolId()), icon.mimeType().value(), icon.accessibleLabel())).orElse(null);
    }

    private Property property(ToolProperty property) {
        return new Property(property.name(), property.description(), property.defaultValue(), property.valueType(), property.required(), property.secret());
    }

    private Requirement requirement(ToolRequirement requirement) {
        return new Requirement(requirement.kind(), requirement.name(), requirement.description(), requirement.required(), requirement.label(), requirement.license(), requirement.vcs(), requirement.cloneUrl(), requirement.checkoutRef(), requirement.baseUrlProperty(), requirement.credentialProperty(), requirement.scope(), requirement.canonicalIdentity());
    }

    private Link link(ToolLink link) {
        return new Link(link.kind(), link.url(), link.label());
    }

    private ToolModuleCatalog.Entry entry(String toolId) {
        return catalog.findByToolId(toolId).orElseThrow(() -> notFound("tool module was not found"));
    }

    private static ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    private static String detailHref(String id) { return "/api/v1/tool-modules/" + encoded(id); }
    private static String iconHref(String id) { return detailHref(id) + "/icon"; }
    private static String readmeHref(String id) { return detailHref(id) + "/readme"; }
    private static String encoded(String id) { return UriUtils.encodePathSegment(id, StandardCharsets.UTF_8); }

    public record ModuleListResponse(List<ModuleSummary> items) { }
    public record ModuleSummary(String toolId, String source, String namespace, String title, IconLink icon, String href) { }
    public record ModuleDetailResponse(String toolId, String source, String namespace, ModuleMetadata metadata, List<Property> properties, List<Requirement> requirements, ReadmeLink readme) { }
    public record ModuleMetadata(String title, String summary, String description, List<Author> authors, String license, List<String> tags, List<Link> links, IconLink icon) { }
    public record Author(String name, String[] urls) { }
    public record Link(String kind, String url, String label) { }
    public record IconLink(String href, String mediaType, String alt) { }
    public record ReadmeLink(String href, String mediaType, boolean available) { }
    public record Property(String name, String description, String defaultValue, String type, boolean required, boolean secret) { }
    public record Requirement(String kind, String name, String description, boolean required, String label, String license, String vcs, String cloneUrl, String checkoutRef, String baseUrlProperty, String credentialProperty, String scope, String canonicalIdentity) { }
}
