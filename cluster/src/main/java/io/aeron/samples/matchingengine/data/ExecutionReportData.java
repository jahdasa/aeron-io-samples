package io.aeron.samples.matchingengine.data;

import com.carrotsearch.hppc.LongIntHashMap;
import io.aeron.samples.matchingengine.crossing.CrossingProcessor;
import dao.TraderDAO;
import leafNode.OrderEntry;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.joda.time.Instant;
import sbe.builder.BuilderUtil;
import sbe.builder.ExecutionReportBuilder;
import sbe.builder.NewInstrumentCompleteBuilder;
import sbe.builder.OrderViewBuilder;
import sbe.msg.*;

import java.nio.ByteBuffer;

public enum ExecutionReportData {
    INSTANCE;

    private int compID;
    private byte[] clientOrderId = new byte[ExecutionReportDecoder.clientOrderIdLength()];
    private int orderId;
    private ExecutionTypeEnum executionType;
    private OrderStatusEnum orderStatus;
    private RejectCode rejectCode = RejectCode.NULL_VAL;
    private long executedPrice;
    private LongIntHashMap fillGroups;
    private ContainerEnum container = ContainerEnum.Main;

    private ExecutionReportBuilder reportBuilder;
    private OrderViewBuilder orderViewBuilder;
    private final NewInstrumentCompleteBuilder newInstrumentCompleteBuilder;

    ExecutionReportData(){
        this.fillGroups = new LongIntHashMap();
        this.reportBuilder = new ExecutionReportBuilder();
        this.orderViewBuilder = new OrderViewBuilder();
        this.newInstrumentCompleteBuilder = new NewInstrumentCompleteBuilder();
    }

    public void reset(){
        compID = 0;
        executedPrice = 0L;
        fillGroups.clear();
        rejectCode = RejectCode.NULL_VAL;
        reportBuilder.reset();
        orderViewBuilder.reset();
        newInstrumentCompleteBuilder.reset();
    }

    public void addFillGroup(long price, int quantity){
        fillGroups.put(price, quantity);
    }

    public LongIntHashMap getFillGroup(){
        return fillGroups;
    }

    public byte[] getClientOrderId() {
        return clientOrderId;
    }

    public void setClientOrderId(byte[] clientOrderId) {
        this.clientOrderId = clientOrderId;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public ExecutionTypeEnum getExecutionType() {
        return executionType;
    }

    public void setExecutionType(ExecutionTypeEnum executionType) {
        this.executionType = executionType;
    }

    public OrderStatusEnum getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(OrderStatusEnum orderStatus) {
        this.orderStatus = orderStatus;
    }

    public RejectCode getRejectCode() {
        return rejectCode;
    }

    public void setRejectCode(RejectCode rejectCode) {
        this.rejectCode = rejectCode;
    }

    public long getExecutedPrice() {
        return executedPrice;
    }

    public void setExecutedPrice(long executedPrice) {
        this.executedPrice = executedPrice;
    }

    public ContainerEnum getContainer() {
        return container;
    }

    public void setContainer(ContainerEnum container) {
        this.container = container;
    }

    public int getCompID() {
        return compID;
    }

    public void setCompID(int compID) {
        this.compID = compID;
    }

    public DirectBuffer buildExecutionReport(OrderEntry aggOrder, int securityId){
        String execId = BuilderUtil.fill("Exec" + Instant.now().getMillis(), ExecutionReportEncoder.executionIDLength());
        String acc= BuilderUtil.fill("test123", ExecutionReportEncoder.accountLength());

        return reportBuilder.compID(getCompID())
                .partitionId((short)0)
                .sequenceNumber(CrossingProcessor.sequenceNumber.incrementAndGet())
                .executionId(execId.getBytes())
                .clientOrderId(getClientOrderId())
                .orderId(getOrderId())
                .executionType(getExecutionType())
                .orderStatus(OrderStatusEnum.get(aggOrder.getOrderStatus()))
                .rejectCode(getRejectCode())
                .addAllFillGroup(getFillGroup())
                .leavesQuantity(aggOrder.getQuantity())
                .container(getContainer())
                .securityId(securityId)
                .side(SideEnum.get(aggOrder.getSide()))
                .traderId(aggOrder.getTrader())
                .account(acc.getBytes())
                .isMarketOpsRequest(IsMarketOpsRequestEnum.No)
                .transactTime(Instant.now().getMillis())
                .orderBook(OrderBookEnum.Regular)
                .build();
    }

    public int getExecutionReportMessageLength(){
        return reportBuilder.getMessageLength();
    }

    public void buildOrderView(OrderEntry aggOrder, long securityId){
        UnsafeBuffer clientOrderId = new UnsafeBuffer(ByteBuffer.allocateDirect(OrderViewEncoder.clientOrderIdLength()));
        clientOrderId.wrap(BuilderUtil.fill(Long.toString(aggOrder.getClientOrderId()), OrderViewEncoder.clientOrderIdLength()).getBytes());
        orderViewBuilder.compID(getCompID())
                .orderId((int) aggOrder.getOrderId())
                .traderId(aggOrder.getTrader())
                .clientOrderId(clientOrderId.byteArray())
                .orderQuantity(aggOrder.getQuantity())
                .price(aggOrder.getPrice())
                .side(aggOrder.getSide() == 1 ? SideEnum.Buy : SideEnum.Sell)
                .submittedTime(java.time.Instant.now().toEpochMilli())
                .securityId((int)securityId);
    }

    public DirectBuffer buildNewInstrumentReport(
        final int securityId,
        final String code,
        final NewInstrumentCompleteStatus status)
    {
        return newInstrumentCompleteBuilder.compID(getCompID())
            .securityId(securityId)
            .code(code)
            .status(status)
            .build();
    }

    public DirectBuffer getOrderView()
    {
        return orderViewBuilder.build();
    }

    public int getOrderViewMessageLength()
    {
        return orderViewBuilder.getMessageLength();
    }

    public int getNewInstrumentCompleteMessageLength()
    {
        return newInstrumentCompleteBuilder.getMessageLength();
    }
}
