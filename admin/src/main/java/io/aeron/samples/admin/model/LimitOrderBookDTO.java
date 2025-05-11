package io.aeron.samples.admin.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@RequiredArgsConstructor
public class LimitOrderBookDTO extends BaseResponse implements Serializable
{
    int securityId;
    List<OrderDTO> orders;

    @Data
    public static class OrderDTO implements Serializable
    {
        String clientOrderId;
        int orderId;
        double price;
        sbe.msg.SideEnum side;
        long quantity;
    }
}
