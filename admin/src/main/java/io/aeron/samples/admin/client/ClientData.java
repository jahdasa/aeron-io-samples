package io.aeron.samples.admin.client;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClientData
{
    private int compID;

    public ClientData(int compID)
    {
        this.compID = compID;
    }
}
