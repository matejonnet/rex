package org.jboss.pnc.scheduler.core.infinispan.protobuf;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.jboss.pnc.scheduler.common.enums.Method;
import org.jboss.pnc.scheduler.common.enums.Mode;
import org.jboss.pnc.scheduler.common.enums.State;
import org.jboss.pnc.scheduler.common.enums.StopFlag;
import org.jboss.pnc.scheduler.model.Header;
import org.jboss.pnc.scheduler.model.Request;
import org.jboss.pnc.scheduler.model.ServerResponse;
import org.jboss.pnc.scheduler.model.Task;

/**
 * Generates .proto schemas and infinispan protobuf marshallers of proto-annotated classes in includeClasses
 */
@AutoProtoSchemaBuilder(schemaPackageName = "org.jboss.pnc.scheduler.model",
        includeClasses = {ServerResponse.class,
                Task.class,
                Header.class,
                Method.class,
                Mode.class,
                State.class,
                StopFlag.class,
                Request.class})
interface TaskProtobufContextInitializer extends SerializationContextInitializer {
}
