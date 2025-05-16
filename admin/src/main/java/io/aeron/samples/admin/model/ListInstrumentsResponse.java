package io.aeron.samples.admin.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@RequiredArgsConstructor
public class ListInstrumentsResponse extends BaseResponse
{
    private final String correlationId;
    final List<InstrumentDTO> instruments;
}