package io.aeron.samples.admin.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@ToString
@Data
@AllArgsConstructor
public class ResponseWrapper
{
    private int status;
    private BaseResponse data;
    private BaseError error;

    public ResponseWrapper()
    {}

    public ResponseWrapper(int status, String errorMessage)
    {
        this.status = status;
        this.error = new BaseError(errorMessage);
    }
}