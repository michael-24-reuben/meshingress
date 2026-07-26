package dev.mrk.meshingress.storage;

import dev.mrk.meshingress.storage.web.StorageController;
import dev.mrk.meshingress.storage.web.StorageRouteExceptionHandler;
import dev.mrk.meshingress.storage.workspace.WorkspaceFileRecord;
import dev.mrk.meshingress.storage.workspace.WorkspaceRetrievalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StorageControllerTests {
    private WorkspaceRetrievalService retrieval;
    private MockMvc mvc;
    @BeforeEach void setUp() {
        retrieval = mock(WorkspaceRetrievalService.class);
        mvc = MockMvcBuilders.standaloneSetup(new StorageController(retrieval)).setControllerAdvice(new StorageRouteExceptionHandler()).build();
    }
    @Test void headUsesTheExplicitSessionRequestFileRoute() throws Exception {
        when(retrieval.inspect("session-1", "req-1", "chapters/001.webp")).thenReturn(new WorkspaceFileRecord("chapters/001.webp", "image/webp", 4, "abc"));
        mvc.perform(head("/api/v1/storage/session-1/req-1/files/chapters/001.webp"))
                .andExpect(status().isOk()).andExpect(header().string("Content-Type", "image/webp"))
                .andExpect(header().string("Content-Length", "4"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"001.webp\""));
    }
    @Test void rangeIsRejectedBeforeWorkspaceLookup() throws Exception {
        mvc.perform(get("/api/v1/storage/session-1/req-1/files/note.txt").header("Range", "bytes=0-1")).andExpect(status().isRequestedRangeNotSatisfiable());
    }
}
