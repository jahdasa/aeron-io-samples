package io.aeron.samples.admin.model;

import io.aeron.samples.cluster.protocol.AddAuctionResult;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@RequiredArgsConstructor
public class CreateAuctionResponse extends BaseResponse
{
    private final String correlationId;
    final long auctionId;
    final AddAuctionResult result;
}