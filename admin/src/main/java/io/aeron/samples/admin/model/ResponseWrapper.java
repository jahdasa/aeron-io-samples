package io.aeron.samples.admin.model;

import lombok.Data;
import lombok.ToString;

@ToString
@Data
public class ResponseWrapper
{
    private int status;
    private BaseResponse data;
    private BaseError error;
}