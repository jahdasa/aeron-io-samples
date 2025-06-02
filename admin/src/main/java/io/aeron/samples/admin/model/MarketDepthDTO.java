package io.aeron.samples.admin.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
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

        BigDecimal price;
        sbe.msg.SideEnum side;
        BigDecimal quantity;
        BigDecimal total;
    }
}
