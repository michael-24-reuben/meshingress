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
public class ParentCommentNode {
    private String id;
    private String text;
    private Long createdAt;
    private Boolean didReportAsSpam;
    private CommentOwner owner;
    private Boolean viewerHasLiked;
    private CountConnection edgeLikedBy;
    private Boolean isRestrictedPending;
    private EdgeThreadedComments edgeThreadedComments;
}
