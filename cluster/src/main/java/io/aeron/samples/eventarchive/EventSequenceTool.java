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

import io.aeron.Subscription;
import io.aeron.archive.client.AeronArchive;
import io.aeron.archive.client.RecordingDescriptorConsumer;
import io.aeron.logbuffer.ControlledFragmentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Create an agent runner and initialise it.
 *
 */
public class EventSequenceTool
{
    private static final Logger LOGGER = LoggerFactory.getLogger(EventSequenceTool.class);
    final EventsLoggerSubscriber eventsSubscriber = new EventsLoggerSubscriber();

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
            -1L
        };

        final long stopPosition = archive.getStopPosition(recordingId);
        if (stopPosition <= 0)
        {
            return 0L;
        }

        try (Subscription replaySession = archive.replay(
            recordingId,
            0L,
            stopPosition,
            "aeron:ipc",
            21))
        {

            while (latestSequenceId[0] < 0L)
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

        LOGGER.info("recordingId: {}, lastSequenceId: {}", recordingId, latestSequenceId[0]);

        return latestSequenceId[0];
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
}
