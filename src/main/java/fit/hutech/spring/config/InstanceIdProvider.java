package fit.hutech.spring.config;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class InstanceIdProvider {
    private final String instanceId = UUID.randomUUID().toString();

    public String getInstanceId() {
        return instanceId;
    }
}

