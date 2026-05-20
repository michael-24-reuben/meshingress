package dev.mrk.toolspace.instagram.instafetch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class InstagramUser {
    private String id;
    private String username;
    private String fullName;
    private Boolean isVerified;
    private String profilePicUrl;
    private Boolean followedByViewer;
}
