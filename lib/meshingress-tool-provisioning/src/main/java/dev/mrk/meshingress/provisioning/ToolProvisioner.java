package dev.mrk.meshingress.provisioning;

public interface ToolProvisioner<R> {
    Class<R> requestType();

    ProvisioningResult provision(R request);
}
