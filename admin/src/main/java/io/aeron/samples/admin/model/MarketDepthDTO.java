package io.aeron.samples.admin.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@RequiredArgsConstructor
public class MarketDepthDTO  extends BaseResponse implements Serializable
{
    int securityId;
    List<MarketDepthLine> lines;

    long bidTotalVolume;
    long askTotalVolume;

    long bidTotal;
    long askTotal;

    @Data
    public static class MarketDepthLine implements Serializable
    {
        long count;
        double price;
        sbe.msg.SideEnum side;
        long quantity;
    }
}
