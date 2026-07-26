package dev.mrk.meshingress.storage;

import dev.mrk.meshingress.storage.web.DelegatedViewerController;
import dev.mrk.meshingress.storage.web.StorageRouteExceptionHandler;
import dev.mrk.meshingress.storage.workspace.DelegatedViewerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DelegatedViewerControllerTests {
    private DelegatedViewerService viewer;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        viewer = mock(DelegatedViewerService.class);
        mvc = MockMvcBuilders.standaloneSetup(new DelegatedViewerController(viewer))
                .setControllerAdvice(new StorageRouteExceptionHandler()).build();
    }

    @Test
    void streamsJsonInlineWithoutNavigatingToNextcloud() throws Exception {
        byte[] json = "{\"type\":\"open-ink.book/v1\"}".getBytes(StandardCharsets.UTF_8);
        when(viewer.open("capability", "book.json"))
                .thenReturn(new DelegatedViewerService.OpenFile(new ByteArrayInputStream(json), "application/json", json.length, "book.json"));

        var result = mvc.perform(get("/api/v1/storage/delegated/capability/files/book.json"))
                .andExpect(request().asyncStarted())
                .andReturn();
        mvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(content().bytes(json))
                .andExpect(header().string("Content-Disposition", "inline; filename=\"book.json\""))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    void rejectsRangeRequestsBeforeOpeningTheCapability() throws Exception {
        mvc.perform(get("/api/v1/storage/delegated/capability/files/book.json").header("Range", "bytes=0-1"))
                .andExpect(status().isRequestedRangeNotSatisfiable());
    }

    @Test
    void streamsWhenNextcloudDoesNotProvideContentLength() throws Exception {
        when(viewer.open("capability", "book.json"))
                .thenReturn(new DelegatedViewerService.OpenFile(new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)), "application/json", -1, "book.json"));

        var result = mvc.perform(get("/api/v1/storage/delegated/capability/files/book.json"))
                .andExpect(request().asyncStarted())
                .andReturn();
        mvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().string("{}"));
    }
}
