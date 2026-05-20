import dev.mrk.toolspace.instagram.instafetch.FetchPath;
import dev.mrk.toolspace.instagram.instafetch.InstaFetch;
import dev.mrk.toolspace.instagram.instafetch.InstaGraphDataHandler;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public class InstaFetchClientPrintTest {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String SHORT_CODE = "DYWTgB7Ta8p";

    public static void main(String[] args) {
        InstaFetch instafetch = new InstaFetch(
                FetchPath.asUrl("https://www.instagram.com/reel/DWCd2FtkfTj/")
        );

        JsonNode response = instafetch.submitRequest();
//        InstaGraphQLResponseRoot responseRoot = instafetch.submitRequestToDto();
//        InstaGraphQLResponseRoot graphQLResponse = mapper.treeToValue(response, InstaGraphQLResponseRoot.class);

        InstaGraphDataHandler media = InstaGraphDataHandler.from(response);

        media.getMediaUrl();
        media.getVideoUrl();
        media.getCaption();
        media.toSummary();

        System.out.println("Media URL:" + media.getMediaUrl());
        System.out.println("Video URL:" + media.getVideoUrl());
        System.out.println("Caption:" + media.getCaption());
        System.out.println("---");
        System.out.println(media.getRawResponse().toPrettyString());
    }
}
