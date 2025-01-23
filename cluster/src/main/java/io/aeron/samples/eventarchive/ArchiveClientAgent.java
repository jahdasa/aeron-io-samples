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

package io.aeron.samples.eventarchive;

import io.aeron.Aeron;
import io.aeron.Subscription;
import io.aeron.archive.client.AeronArchive;
import io.aeron.archive.client.RecordingDescriptorConsumer;
import io.aeron.logbuffer.ControlledFragmentHandler;
import io.aeron.logbuffer.FragmentHandler;
import org.agrona.CloseHelper;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.IdleStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Create an agent runner and initialise it.
 *
 */
public class ArchiveClientAgent implements Agent
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveClientAgent.class);
    private final Aeron aeron;
    private final IdleStrategy idleStrategy;
    private final AeronArchive archive;
    private State currentState;
    private Subscription replayDestinationSubs;
    final EventsLoggerSubscriber eventsSubscriber = new EventsLoggerSubscriber();
    final FragmentHandler fragmentHandler;
    /**
     * Create an agent runner and initialise it.
     *
     * @param idleStrategy to use for Agent run loop
     * @param aeron to use as a client
     * @param archive to replay
     */
    public ArchiveClientAgent(final Aeron aeron, final AeronArchive archive, final IdleStrategy idleStrategy)
    {
        this.aeron = aeron;
        this.archive = archive;
        this.idleStrategy = idleStrategy;
        this.currentState = State.AERON_READY;

        this.fragmentHandler = (buffer, offset, length, header) ->
        {
            final String message = buffer.getStringWithoutLengthAscii(offset, length);
            LOGGER.info("Subscriber received: {}", message);

            eventsSubscriber.onMessage(buffer, offset, length);
        };
    }

    /**
     * Connect then poll
     */
    @Override
    public int doWork()
    {
        switch (currentState)
        {
            case AERON_READY -> findLastSequenceId();
            case SEQUENCE_ID_READY -> connectToArchive();
            case POLLING_SUBSCRIPTION -> replayDestinationSubs.poll(fragmentHandler, 1);
            default -> LOGGER.error("unknown state {}", currentState);
        }

        return 0;
    }

    /**
     * findLastSequenceId
     */
    public void findLastSequenceId()
    {

        // Replay from the beginning of the recording
        final long sequenceId = findLastSequenceId(archive);
        if (sequenceId < 0)
        {
            LOGGER.error("No sequenceId {} found for stream ID: {}", sequenceId, 17);

            return;
        }
        else
        {
            LOGGER.info("last sequenceId: {} found for stream ID: {}", sequenceId, 17);
        }

        currentState = State.SEQUENCE_ID_READY;
    }

    /**
     * connectToArchive
     */
    private void connectToArchive()
    {
        LOGGER.info("connect to archive");

        // Replay from the beginning of the recording
        final long recordingId = findRecording(archive);
        if (recordingId < 0)
        {
            LOGGER.error("No recording found for stream ID: {}", 17);
            return;
        }

        replayDestinationSubs = aeron.addSubscription("aeron:ipc", 18);

        LOGGER.info("Starting replay for recordingId: {}", recordingId);

        archive.startReplay(
            recordingId,
            0,
            Long.MAX_VALUE, // Replay everything
            "aeron:ipc",
            18
        );

        while (!replayDestinationSubs.isConnected())
        {
            idleStrategy.idle();
        }
        LOGGER.info("replayDestinationSubs isConnected");

        currentState = State.POLLING_SUBSCRIPTION;
    }

    /**
     * findLastSequenceId
     *
     * @param archive to find latest recording
     * @return last sequenceId
     */
    public long findLastSequenceId(final AeronArchive archive)
    {
        long lastSequenceId = -1;

        final List<Long> allRecording = findAllRecording(archive);

        for (final long recordingId: allRecording)
        {
            final long sequenceId = findLastSequenceId(archive, recordingId);

            if (sequenceId > lastSequenceId)
            {
                lastSequenceId = sequenceId;
            }
        }

        return lastSequenceId;
    }

    /**
     * findLastSequenceId
     *
     * @param archive to find latest recording
     * @param recordingId to find sequence in
     * @return last sequenceId
     */
    private long findLastSequenceId(final AeronArchive archive, final long recordingId)
    {
        final long[] latestSequenceId =
        {
            -1
        };



        final long stopPosition = archive.getStopPosition(recordingId);
        if (stopPosition < 0)
        {
            return 0;
        }

        try (Subscription replaySession = archive.replay(
            recordingId,
            0,
            stopPosition,
            "aeron:ipc",
            21))
        {

            while (latestSequenceId[0] < 0)
            {
                replaySession.controlledPoll((buffer, offset, length, header) ->
                {
                    final long sequenceId =
                        eventsSubscriber.findSequenceIdOfMessage(buffer, offset, length);

                    LOGGER.info("recordingId: {}, last sequenceId: {}", recordingId, sequenceId);

                    if (stopPosition == header.position())
                    {
                        latestSequenceId[0] = sequenceId;
                        return ControlledFragmentHandler.Action.BREAK;
                    }

                    return ControlledFragmentHandler.Action.CONTINUE;
                }, 1);
            }
        }

        LOGGER.info("lastSequenceId: {}", latestSequenceId[0]);

        return latestSequenceId[0];
    }

    /**
     * Connect then poll
     *
     * @param archive to find latest recording
     * @return recodingId
     */
    private long findRecording(final AeronArchive archive)
    {
        final List<Long> recordingIds17StreamId = new ArrayList<>();

        final long[] latestRecordingId =
        {
            -1
        };

        archive.listRecordings(
            0, // Start from the first recording
            Integer.MAX_VALUE, // Search all recordings
            new RecordingDescriptorConsumer()
            {
                @Override
                public void onRecordingDescriptor(
                    final long controlSessionId,
                    final long correlationId,
                    final long recordingId,
                    final long startTimestamp,
                    final long stopTimestamp,
                    final long startPosition,
                    final long stopPosition,
                    final int initialTermId,
                    final int segmentFileLength,
                    final int termBufferLength,
                    final int mtuLength,
                    final int sessionId,
                    final int streamId,
                    final String strippedChannel,
                    final String originalChannel,
                    final String sourceIdentity
                )
                {
                    if (streamId == 17)
                    {
                        latestRecordingId[0] = recordingId;
                        recordingIds17StreamId.add(recordingId);
                    }
                }
            }
        );

        LOGGER.info("streamId 17 recordingIds: {}", recordingIds17StreamId);
        LOGGER.info("latestRecordingId: {}", latestRecordingId[0]);

        return latestRecordingId[0];
    }

    /**
     * find all recodingIds
     *
     * @param archive to find latest recording
     * @return recodingId
     */
    private List<Long> findAllRecording(final AeronArchive archive)
    {
        final List<Long> recordingIds17StreamId = new ArrayList<>();

        archive.listRecordings(
            0, // Start from the first recording
            Integer.MAX_VALUE, // Search all recordings
            new RecordingDescriptorConsumer()
            {
                @Override
                public void onRecordingDescriptor(
                    final long controlSessionId,
                    final long correlationId,
                    final long recordingId,
                    final long startTimestamp,
                    final long stopTimestamp,
                    final long startPosition,
                    final long stopPosition,
                    final int initialTermId,
                    final int segmentFileLength,
                    final int termBufferLength,
                    final int mtuLength,
                    final int sessionId,
                    final int streamId,
                    final String strippedChannel,
                    final String originalChannel,
                    final String sourceIdentity
                )
                {
                    if (streamId == 17)
                    {
                        recordingIds17StreamId.add(recordingId);
                    }
                }
            }
        );

        LOGGER.info("streamId 17 recordingIds: {}", recordingIds17StreamId);

        return recordingIds17StreamId;
    }

    /**
     * role name
     *
     */
    @Override
    public String roleName()
    {
        return "archive-client";
    }

    /**
     * start
     *
     */
    @Override
    public void onStart()
    {
        Agent.super.onStart();
        LOGGER.info("starting Archive client agent");
    }

    /**
     * onClose
     *
     */
    @Override
    public void onClose()
    {
        Agent.super.onClose();
        LOGGER.info("shutting down");
        CloseHelper.quietClose(replayDestinationSubs);
    }

}
