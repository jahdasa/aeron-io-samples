package io.aeron.samples.admin.model;

import io.aeron.samples.cluster.protocol.AuctionStatus;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;

@Data
@RequiredArgsConstructor
public class ActionDTO implements Serializable
{
    final long auctionId;
    final String name;
    final long createdBy;
    final long startTime;
    final long endTime;
    final long winningParticipantId;
    final long currentPrice;
    final AuctionStatus status;
}
