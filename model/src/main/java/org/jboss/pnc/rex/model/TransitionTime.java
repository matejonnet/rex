/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2021-2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.rex.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.jboss.pnc.rex.common.enums.Transition;

import java.time.Instant;

@Builder
@Jacksonized
@AllArgsConstructor(onConstructor_ = {@ProtoFactory})
public class TransitionTime implements Comparable<TransitionTime> {

    @Getter(onMethod_ = {@ProtoField(number = 1)})
    private final Transition transition;

    @Getter(onMethod_ = {@ProtoField(number = 2)})
    private final Instant time;

    @Override
    public int compareTo(TransitionTime other) {
        return this.getTime().compareTo(other.getTime());
    }
}
