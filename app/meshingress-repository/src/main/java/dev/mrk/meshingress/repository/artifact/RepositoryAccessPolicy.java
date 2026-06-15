package dev.mrk.meshingress.repository.artifact;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RepositoryAccessPolicy {

    public void require(RepositoryRequestContext context, RepositoryAction action) {
        if (isAllowed(context, action)) {
            return;
        }
        throw new RepositoryAccessDeniedException("repository role is not allowed to " + action.name().toLowerCase(Locale.ROOT));
    }

    private boolean isAllowed(RepositoryRequestContext context, RepositoryAction action) {
        Set<String> roles = roles(context);
        if (roles.contains("admin")) {
            return true;
        }
        return switch (action) {
            case READ -> !roles.isEmpty();
            case UPLOAD -> roles.contains("uploader");
            case ASSESS, APPROVE -> roles.contains("reviewer");
            case PUBLISH -> roles.contains("publisher");
        };
    }

    private Set<String> roles(RepositoryRequestContext context) {
        if (context == null || context.role() == null) {
            return Set.of();
        }
        return Arrays.stream(context.role().split("[,\\s]+"))
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());
    }
}
