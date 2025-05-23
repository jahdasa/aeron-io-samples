/*
 * Copyright 2023 Adaptive Financial Consulting
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.aeron.samples.admin.cli;

import io.aeron.samples.cluster.admin.protocol.ConnectClusterEncoder;
import io.aeron.samples.cluster.admin.protocol.MessageHeaderEncoder;
import lombok.Setter;
import org.agrona.ExpandableArrayBuffer;
import picocli.CommandLine;

import static io.aeron.samples.admin.util.EnvironmentUtil.getThisHostName;
import static io.aeron.samples.admin.util.EnvironmentUtil.tryGetClusterHostsFromEnv;
import static io.aeron.samples.admin.util.EnvironmentUtil.tryGetResponsePortFromEnv;

/**
 * Adds a participant to the cluster
 */
@CommandLine.Command(name = "connect", mixinStandardHelpOptions = false,
    description = "Connects to the cluster")
public class ConnectCluster implements Runnable
{
    private final ExpandableArrayBuffer buffer = new ExpandableArrayBuffer(1024);
    private final MessageHeaderEncoder messageHeaderEncoder = new MessageHeaderEncoder();
    private final ConnectClusterEncoder connectClusterEncoder = new ConnectClusterEncoder();

    @Setter
    @CommandLine.ParentCommand
    CliCommands parent;

    @Setter
    String correlationId;

    @SuppressWarnings("all")
    @CommandLine.Option(names = "baseport", description = "The base port to connect to")
    private Integer baseport = 9000;
    @SuppressWarnings("all")
    @CommandLine.Option(names = "hostnames", description = "The cluster address(es) to connect to. " +
        "Multiple addresses can be specified by separating them with a comma.")
    private String hostnames = tryGetClusterHostsFromEnv();
    @SuppressWarnings("all")
    @CommandLine.Option(names = "thishost", description = "The response hostname (defaulted to localhost).")
    private String localhost = getThisHostName();
    @CommandLine.Option(names = "port", description = "The port to use for communication. The default, 0," +
        " will auto-assign a port")
    private Integer port = tryGetResponsePortFromEnv();

    /**
     * sends a connect cluster via the comms channel
     */
    public void run()
    {
        connectClusterEncoder.wrapAndApplyHeader(buffer, 0, messageHeaderEncoder);

        connectClusterEncoder.correlationId(correlationId);
        connectClusterEncoder.baseport(baseport);
        connectClusterEncoder.port(port);
        connectClusterEncoder.clusterHosts(hostnames);
        connectClusterEncoder.localhostName(localhost);

        parent.offerRingBufferMessage(
            buffer, 0, MessageHeaderEncoder.ENCODED_LENGTH + connectClusterEncoder.encodedLength());
    }


}
