package io.aeron.samples.admin.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@RequiredArgsConstructor
public class InstrumentDTO extends BaseResponse
{
    final int securityId;
    final String code;
    final String name;
}