package io.aeron.samples.admin.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;

@Data
@RequiredArgsConstructor
public class BBODTO extends BaseResponse implements Serializable
{
    final long bidQuantity;
    final long offerQuantity;
    final double bidValue;
    final double offerValue;
}
