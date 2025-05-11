package io.aeron.samples.admin.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@RequiredArgsConstructor
public class VWAPDTO extends BaseResponse implements Serializable
{
    private final double bidVWAP;
    private final double offerVWAP;

}
