package dev.mrk.meshingress.api.tools.annotation.availability;

import java.lang.reflect.Method;

public record AvailabilityValidationContext(Class<?> controllerType, Method handlerMethod) {

    public String location() {
        return controllerType.getName() + "#" + handlerMethod.getName();
    }
}

