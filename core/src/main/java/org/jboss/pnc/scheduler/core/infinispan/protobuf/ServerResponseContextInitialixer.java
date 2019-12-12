package org.jboss.pnc.scheduler.core.infinispan.protobuf;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.jboss.pnc.scheduler.common.enums.*;
import org.jboss.pnc.scheduler.model.RemoteAPI;
import org.jboss.pnc.scheduler.model.ServerResponse;
import org.jboss.pnc.scheduler.model.Task;

/**
 * Generates .proto schemas and infinispan protobuf marshallers of proto-annotated entities in basePackages
 */
@AutoProtoSchemaBuilder(basePackages = {"org.jboss.pnc.scheduler.model", "org.jboss.pnc.scheduler.common.enums"}, schemaPackageName = "org.jboss.pnc.scheduler.model",
includeClasses = {ServerResponse.class, Task.class, RemoteAPI.class, Mode.class, State.class, StopFlag.class})
interface ServerResponseContextInitialixer extends SerializationContextInitializer {
}
