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

package io.aeron.samples.infra;

import io.aeron.ExclusivePublication;
import io.aeron.Image;
import io.aeron.cluster.codecs.CloseReason;
import io.aeron.cluster.service.ClientSession;
import io.aeron.cluster.service.Cluster;
import io.aeron.cluster.service.ClusteredService;
import io.aeron.logbuffer.Header;
import io.aeron.samples.domain.auctions.Auctions;
import io.aeron.samples.domain.participants.Participants;
import org.agrona.DirectBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The clustered service for the auction application.
 */
public class AppClusteredService implements ClusteredService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AppClusteredService.class);

    private final ClientSessions clientSessions = new ClientSessions();
    private final SessionMessageContextImpl context = new SessionMessageContextImpl(clientSessions);
    private final ClusterClientResponder clusterClientResponder = new ClusterClientResponderImpl(context);
    private final TimerManager timerManager = new TimerManager(context);

    private final Participants participants = new Participants(clusterClientResponder);
    private final Auctions auctions = new Auctions(
        context,
        participants,
        clusterClientResponder,
        timerManager);

    private final SnapshotManager snapshotManager = new SnapshotManager(auctions, participants, context);
    private final SbeDemuxer sbeDemuxer = new SbeDemuxer(participants, auctions, clusterClientResponder);

    @Override
    public void onStart(final Cluster cluster, final Image snapshotImage)
    {
        snapshotManager.setIdleStrategy(cluster.idleStrategy());
        context.setIdleStrategy(cluster.idleStrategy());
        timerManager.setCluster(cluster);
        if (snapshotImage != null)
        {
            snapshotManager.loadSnapshot(snapshotImage);
        }
    }

    @Override
    public void onSessionOpen(final ClientSession session, final long timestamp)
    {
        LOGGER.info("Client session opened, session: {}, timestamp: {}", session, timestamp);
        context.setClusterTime(timestamp);
        clientSessions.addSession(session);
    }

    @Override
    public void onSessionClose(final ClientSession session, final long timestamp, final CloseReason closeReason)
    {
        LOGGER.info(
            "Client session closed, session: {}, timestamp: {}, closeReason: {}",
            session,
            timestamp,
            closeReason);

        context.setClusterTime(timestamp);
        clientSessions.removeSession(session);
    }

    @Override
    public void onSessionMessage(
        final ClientSession session,
        final long timestamp,
        final DirectBuffer buffer,
        final int offset,
        final int length,
        final Header header)
    {
/*        LOGGER.info(
            "Client message received, session: {}, timestamp: {}",
            session,
            timestamp);*/

        context.setSessionContext(session, timestamp);
        sbeDemuxer.dispatch(buffer, offset, length);
    }

    @Override
    public void onTimerEvent(final long correlationId, final long timestamp)
    {
        context.setClusterTime(timestamp);
        timerManager.onTimerEvent(correlationId, timestamp);
    }

    @Override
    public void onTakeSnapshot(final ExclusivePublication snapshotPublication)
    {
        snapshotManager.takeSnapshot(snapshotPublication);
    }

    @Override
    public void onRoleChange(final Cluster.Role newRole)
    {
        LOGGER.info("Role change: {}", newRole);
    }

    @Override
    public void onTerminate(final Cluster cluster)
    {
        LOGGER.info("Terminating");
    }
}
