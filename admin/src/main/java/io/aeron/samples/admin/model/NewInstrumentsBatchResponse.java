package io.aeron.samples.admin.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@RequiredArgsConstructor
public class NewInstrumentsBatchResponse extends BaseResponse
{
    List<NewInstrumentResponse> instrumentResponses = new ArrayList<>();
}