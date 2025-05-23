package sbe.builder;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import sbe.msg.AdminEncoder;
import sbe.msg.AdminTypeEnum;
import sbe.msg.MessageHeaderEncoder;

import java.nio.ByteBuffer;

public class AdminBuilder {
    private int bufferIndex;
    private AdminEncoder admin;
    private MessageHeaderEncoder messageHeader;
    private UnsafeBuffer encodeBuffer;


    private int compID;
    private int securityId;
    private AdminTypeEnum adminMessage;

    public static int BUFFER_SIZE = 20;

    int messageLength = 0;

    public AdminBuilder(){
        admin = new AdminEncoder();
        messageHeader = new MessageHeaderEncoder();
        encodeBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect(BUFFER_SIZE));
    }

    public AdminBuilder compID(int value){
        this.compID = value;
        return this;
    }

    public AdminBuilder securityId(int value){
        this.securityId = value;
        return this;
    }

    public AdminBuilder adminMessage(AdminTypeEnum value){
        this.adminMessage = value;
        return this;
    }

    public DirectBuffer build(){
        bufferIndex = 0;
        messageHeader.wrap(encodeBuffer, bufferIndex)
                .blockLength(admin.sbeBlockLength())
                .templateId(admin.sbeTemplateId())
                .schemaId(admin.sbeSchemaId())
                .version(admin.sbeSchemaVersion())
                .compID(compID);

        bufferIndex += messageHeader.encodedLength();
        admin.wrap(encodeBuffer, bufferIndex);

        admin.adminMessage(adminMessage)
              .securityId(securityId);

        messageLength = messageHeader.encodedLength() + admin.encodedLength();

        return encodeBuffer;
    }

    public int getMessageLength() {
        return messageLength;
    }
}
