package io.aeron.samples.matchingengine.parser;

import org.agrona.DirectBuffer;
import sbe.msg.AdminDecoder;
import sbe.msg.AdminTypeEnum;


import java.io.UnsupportedEncodingException;

public class AdminMessageParser {
    private AdminDecoder adminDecoder = new AdminDecoder();
    private int securityId;
    private AdminTypeEnum adminTypeEnum;

    public void decode(DirectBuffer buffer, int bufferOffset, int actingBlockLength, int actingVersion) throws UnsupportedEncodingException {
        adminDecoder.wrap(buffer, bufferOffset, actingBlockLength, actingVersion);
        adminTypeEnum =  adminDecoder.adminMessage();
        securityId = adminDecoder.securityId();
    }


    public int getSecurityId() {
        return securityId;
    }

    public AdminTypeEnum getAdminTypeEnum() {
        return adminTypeEnum;
    }
}
