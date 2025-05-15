package io.aeron.samples.admin.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import sbe.msg.NewInstrumentCompleteStatus;

@Data
@EqualsAndHashCode(callSuper = true)
@RequiredArgsConstructor
public class NewInstrumentResponse extends BaseResponse
{
    private final String correlationId;
    final long securityId;
    final String code;
    final NewInstrumentCompleteStatus status;
}