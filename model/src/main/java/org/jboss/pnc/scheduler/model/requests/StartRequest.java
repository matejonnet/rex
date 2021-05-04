package org.jboss.pnc.scheduler.model.requests;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Jacksonized
@Builder
@Getter
public class StartRequest {

    private final String callback;

    private final Object payload;
}
