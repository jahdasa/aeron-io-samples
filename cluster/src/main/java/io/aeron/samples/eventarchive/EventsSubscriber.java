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

import io.aeron.samples.cluster.protocol.*;
import org.agrona.DirectBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Admin client egress listener
 */
public class EventsSubscriber
{
    private static final Logger LOGGER = LoggerFactory.getLogger(EventsSubscriber.class);

    private final MessageHeaderDecoder messageHeaderDecoder = new MessageHeaderDecoder();
    private final AuctionUpdateEventDecoder auctionUpdateEventDecoder = new AuctionUpdateEventDecoder();
    private final NewAuctionEventDecoder newAuctionEventDecoder = new NewAuctionEventDecoder();

    /**
     * Create an agent runner and initialise it.
     *
     * @param buffer buffer
     * @param offset offset
     * @param length  length
     */
    public void onMessage(
        final DirectBuffer buffer,
        final int offset,
        final int length)
    {
        if (length < MessageHeaderDecoder.ENCODED_LENGTH)
        {
            LOGGER.warn("Message too short");
            return;
        }
        messageHeaderDecoder.wrap(buffer, offset);

        switch (messageHeaderDecoder.templateId())
        {
            case NewAuctionEventDecoder.TEMPLATE_ID ->
            {
                newAuctionEventDecoder.wrapAndApplyHeader(buffer, offset, messageHeaderDecoder);
                final long auctionId = newAuctionEventDecoder.auctionId();
                final String auctionName = newAuctionEventDecoder.name();

                LOGGER.info("EventsSubscriber New auction: " + "'" + auctionName + "' (" + auctionId + ")");
            }
            case AuctionUpdateEventDecoder.TEMPLATE_ID -> displayAuctionUpdate(buffer, offset);
            default -> LOGGER.info("EventsSubscriber unknown message type: " + messageHeaderDecoder.templateId());
        }
    }

    /**
     * Create an agent runner and initialise it.
     *
     * @param buffer buffer
     * @param offset offset
     */
    private void displayAuctionUpdate(final DirectBuffer buffer, final int offset)
    {
        auctionUpdateEventDecoder.wrapAndApplyHeader(buffer, offset, messageHeaderDecoder);
        final long auctionId = auctionUpdateEventDecoder.auctionId();
        final AuctionStatus auctionStatus = auctionUpdateEventDecoder.status();
        final int bidCount = auctionUpdateEventDecoder.bidCount();
        final long currentPrice = auctionUpdateEventDecoder.currentPrice();
        final long winningParticipantId = auctionUpdateEventDecoder.winningParticipantId();

        if (bidCount == 0)
        {
            if (auctionStatus.equals(AuctionStatus.CLOSED))
            {
                LOGGER.info("EventsSubscriber Auction " + auctionId + " has ended. There were no bids.");
            }
            else
            {
                LOGGER.info("EventsSubscriber Auction " + auctionId + " is now in state " +
                    auctionStatus.name() + ". There have been " +
                    auctionUpdateEventDecoder.bidCount() + " bids.");

            }
        }
        else
        {
            if (auctionStatus.equals(AuctionStatus.CLOSED))
            {
                LOGGER.info(
                    "EventsSubscriber Auction {} won! Total {} bids. Winning price was {}",
                    auctionId,
                    bidCount,
                    currentPrice);

            }
            else
            {
                LOGGER.info("EventsSubscriber Auction update event: auction " + auctionId + " is now in state " +
                    auctionStatus.name() + ". Total " + bidCount + " bids. Current price is " +
                    currentPrice + ". The winning bidder is " + winningParticipantId);

            }
        }
    }
}
