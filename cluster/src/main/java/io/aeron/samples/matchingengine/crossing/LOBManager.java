package io.aeron.samples.matchingengine.crossing;


import org.agrona.DirectBuffer;

public interface LOBManager {

    DirectBuffer processOrder(DirectBuffer message);
    boolean isClientMarketDataRequest();
    boolean isClientMarketDepthRequest();
    boolean isAdminRequest();
}
