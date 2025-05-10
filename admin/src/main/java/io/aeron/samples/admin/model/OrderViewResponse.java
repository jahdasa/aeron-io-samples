package io.aeron.samples.admin.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@RequiredArgsConstructor
public class OrderViewResponse extends BaseResponse
{
    private final String correlationId;
    final long securityId;
    final int traderId;
    final String clientOrderId;
    final long orderId;
    final long submittedTime;
    final double priceValue;
    final int orderQuantity;
    final sbe.msg.SideEnum side;
}