package dev.mrk.toolspace.instagram.instafetch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class XdtShortcodeMedia {
    @JsonProperty("__typename")
    private String typename;

    @JsonProperty("__isXDTGraphMediaInterface")
    private String xdtGraphMediaInterface;

    private String id;
    private String shortcode;
    private String title;
    private String productType;
    private String trackingToken;

    private String thumbnailSrc;
    private String mediaPreview;
    private String displayUrl;
    private List<DisplayResource> displayResources;
    private Dimensions dimensions;

    private Boolean isVideo;
    private String videoUrl;
    private Long videoViewCount;
    private Long videoPlayCount;
    private Double videoDuration;
    private Boolean hasAudio;
    private DashInfo dashInfo;
    private Object encodingStatus;
    private Boolean isPublished;

    private MediaOwner owner;
    private List<CoauthorProducer> coauthorProducers;
    private List<Object> pinnedForUsers;

    private EdgeMediaToCaption edgeMediaToCaption;
    private EdgeMediaToTaggedUser edgeMediaToTaggedUser;
    private EdgeMediaToParentComment edgeMediaToParentComment;
    private EmptyEdgeConnection edgeMediaToHoistedComment;
    private EdgeMediaPreviewComment edgeMediaPreviewComment;
    private EdgeMediaPreviewLike edgeMediaPreviewLike;
    private EmptyEdgeConnection edgeMediaToSponsorUser;
    private EmptyEdgeConnection edgeWebMediaToRelatedMedia;

    private ClipsMusicAttributionInfo clipsMusicAttributionInfo;

    private Long takenAtTimestamp;

    private Boolean canSeeInsightsAsBrand;
    private Boolean captionIsEdited;
    private Boolean hasRankedComments;
    private Boolean likeAndViewCountsDisabled;
    private Boolean commentsDisabled;
    private Boolean commentingDisabledForViewer;

    private Boolean viewerHasLiked;
    private Boolean viewerHasSaved;
    private Boolean viewerHasSavedToCollection;
    private Boolean viewerInPhotoOfYou;
    private Boolean viewerCanReshare;

    private Boolean isAffiliate;
    private Boolean isPaidPartnership;
    private Boolean isAd;

    private Object gatingInfo;
    private Object factCheckOverallRating;
    private Object factCheckInformation;
    private Object sensitivityFrictionInfo;
    private SharingFrictionInfo sharingFrictionInfo;
    private Object accessibilityCaption;
    private Object upcomingEvent;
    private Object location;
    private Object nftAssetInfo;
}
