package io.aeron.samples.admin.model;

import io.aeron.samples.cluster.protocol.AddAuctionBidResult;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@RequiredArgsConstructor
public class AddAuctionBidResponse extends BaseResponse
{
    final String correlationId;
    final long auctionId;
    final AddAuctionBidResult result;
}