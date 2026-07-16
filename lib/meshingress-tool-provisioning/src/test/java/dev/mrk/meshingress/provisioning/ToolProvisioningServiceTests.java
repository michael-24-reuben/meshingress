package dev.mrk.meshingress.provisioning;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ToolProvisioningServiceTests {
    @Test
    void reportsUnsupportedRequestWithoutGuessingAProvider() {
        ProvisioningResult result = new ToolProvisioningService(List.of()).provision("unsupported");

        assertThat(result.status()).isEqualTo(ProvisioningStatus.UNSUPPORTED);
        assertThat(result.ready()).isFalse();
    }

    @Test
    void rejectsAmbiguousProviderSelection() {
        ToolProvisioner<String> first = provider("first");
        ToolProvisioner<String> second = provider("second");

        ProvisioningResult result = new ToolProvisioningService(List.of(first, second)).provision("request");

        assertThat(result.status()).isEqualTo(ProvisioningStatus.FAILED);
        assertThat(result.diagnosticSummary()).contains("Multiple provisioners");
    }

    private static ToolProvisioner<String> provider(String name) {
        return new ToolProvisioner<>() {
            @Override
            public Class<String> requestType() {
                return String.class;
            }

            @Override
            public ProvisioningResult provision(String request) {
                return new ProvisioningResult(ProvisioningStatus.READY, null, List.of(), List.of(), name);
            }
        };
    }
}
