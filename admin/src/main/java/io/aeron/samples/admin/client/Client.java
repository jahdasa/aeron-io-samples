package io.aeron.samples.admin.client;

//import gateway.client.GatewayClient;
//import gateway.client.GatewayClientImpl;
import com.carrotsearch.hppc.IntObjectMap;
import org.agrona.DirectBuffer;
import sbe.builder.*;
import sbe.msg.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Client {

//    private GatewayClient tradingGatewayPub;
//    private TradingGatewaySubscriber tradingGatewaySubscriber;
//    private MulticastMDGSubscriber marketDataGatewaySubscriber;
//    private ClientMDGSubscriber clientMDGSubscriber;
//    private GatewayClient marketDataGatewayPub;

    private NewOrderBuilder newOrderBuilder = new NewOrderBuilder().account("account123".getBytes())
            .capacity(CapacityEnum.Agency)
            .cancelOnDisconnect(CancelOnDisconnectEnum.DoNotCancel)
            .orderBook(OrderBookEnum.Regular)
            .expireTime("20211230-23:00:00".getBytes());

    private OrderCancelRequestBuilder orderCancelRequestBuilder = new OrderCancelRequestBuilder()
            .orderBook(OrderBookEnum.Regular);

    private OrderCancelReplaceRequestBuilder orderCancelReplaceRequestBuilder = new OrderCancelReplaceRequestBuilder()
            .account("account123".getBytes())
            .orderBook(OrderBookEnum.Regular);
    private AdminBuilder adminBuilder = new AdminBuilder();

    private ClientData clientData;
    private long bid;
    private long bidQuantity;
    private long offer;
    private long offerQuantity;
    private int securityId;
    private boolean auction = false;
    private long staticPriceReference;
    private long dynamicPriceReference;

//    private NonBlockingSemaphore mktDataUpdateSemaphore = new NonBlockingSemaphore(1);
//    private NonBlockingSemaphore snapShotSemaphore = new NonBlockingSemaphore(1);

    public Client(ClientData clientData, int securityId){
        this.clientData = clientData;
        this.securityId = securityId;
    }

    public static Client newInstance(int clientId, int securityId) throws Exception {
        // Define the client ID corresponding the client to be logged in as well as the security ID corresponding to the security in which they will trade
        // Load the simulation settings as well as all client data (ports, passwords and IDs)
        Properties properties = loadedProperties( PROPERTIES_FILE);
        String dataPath = properties.get("DATA_PATH").toString();
        IntObjectMap<ClientData> clientData = ClientData.loadClientDataData(dataPath);
        return new Client(clientData.get(clientId), securityId);
    }

    private static final String PROPERTIES_FILE =  "MatchingEngine.properties";

    protected static Properties loadedProperties(String propertiesFile) throws IOException {
        try(InputStream inputStream = Client.class.getClassLoader().getResourceAsStream(propertiesFile)) {

            if (inputStream != null) {
                Properties properties = new Properties();
                properties.load(inputStream);

                return properties;
            } else {
                throw new IOException("Unable to load properties file " + propertiesFile);
            }
        }
    }

/*
    public void initTradingGatewaySub(){
        String url = clientData.getNgOutputURL();
        int streamId = clientData.getNgOutputStreamId();
        tradingGatewaySubscriber = new TradingGatewaySubscriber(url, streamId);
        Thread thread = new Thread(tradingGatewaySubscriber);
        thread.start();
    }
*/

/*    public void initMarketDataGatewayPub() {
        String url = clientData.getMdgInputURL();
        int streamId = clientData.getMdgInputStreamId();
        marketDataGatewayPub = new GatewayClientImpl();
        marketDataGatewayPub.connectInput(url, streamId);
    }*/

/*    public void initMulticastMarketDataGatewaySub(Properties properties) {
        String url = properties.get("MDG_MULTICAST_URL").toString();
        int streamId = Integer.parseInt(properties.get("MDG_MULTICAST_STREAM_ID").toString());
        marketDataGatewaySubscriber = new MulticastMDGSubscriber(url, streamId, this, mktDataUpdateSemaphore);
        Thread thread = new Thread(marketDataGatewaySubscriber);
        thread.start();
    }*/

/*    public void initClientMarketDataGatewaySub() {
        String url = clientData.getMdgOutputURL();
        int streamId = clientData.getMdgOutputStreamId();
        clientMDGSubscriber = new ClientMDGSubscriber(url, streamId, snapShotSemaphore, securityId);
        Thread thread = new Thread(clientMDGSubscriber);
        thread.start();
    }*/

/*    public void init(Properties properties) throws Exception {
        initClientMarketDataGatewaySub();
        initMulticastMarketDataGatewaySub(properties);
        initTradingGatewaySub();
        loginToTradingGatewayPub();
        initMarketDataGatewayPub();
    }*/

/*    public void sendStartMessage() {
        System.out.println("Session started at " + LocalDateTime.now() + ".");
        DirectBuffer buffer = adminBuilder.compID(clientData.getCompID())
                .securityId(securityId)
                .adminMessage(AdminTypeEnum.StartMessage)
                .build();
        tradingGatewayPub.send(buffer);
    }*/

/*    public void loginToTradingGatewayPub() throws IOException {
        String url = clientData.getNgInputURL();
        int streamId = clientData.getNgInputStreamId();
        int compId = clientData.getCompID();
        String password = clientData.getPassword();

        tradingGatewayPub = new GatewayClientImpl();
        tradingGatewayPub.connectInput(url,streamId);

        LogonBuilder logonBuilder = new LogonBuilder();
        DirectBuffer buffer = logonBuilder.compID(compId)
                .password(password.getBytes())
                .newPassword(password.getBytes())
                .build();
        for(int i=0; i<3; i++) {
            try {
                Thread.sleep(1000);
                System.out.println("Logging in.");
                tradingGatewayPub.send(buffer);
                Thread.sleep(2000);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(0);
            }
        }
        System.out.println("Logged in.");
    }*/

/*    public void sendEndMessage() {
        System.out.println("Session ended at " + LocalDateTime.now() + ".");
        DirectBuffer buffer = adminBuilder.compID(clientData.getCompID())
                .securityId(securityId)
                .adminMessage(AdminTypeEnum.EndMessage)
                .build();
        tradingGatewayPub.send(buffer);
    }*/

/*    public void close() {
        while(!clientMDGSubscriber.isStop()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        tradingGatewayPub.disconnectInput();
        tradingGatewaySubscriber.close();
        marketDataGatewaySubscriber.close();
        clientMDGSubscriber.close();
        marketDataGatewayPub.disconnectInput();
        System.out.println("Logged out.");
    }*/

/*
    public void waitForMarketDataUpdate() { while(!mktDataUpdateSemaphore.acquire()){} }
*/

    public DirectBuffer placeOrder(
        String clientOrderId,
        final long volume,
        final long price,
        final String side,
        final String orderType,
        final String timeInForce,
        final long displayQuantity,
        final long minQuantity,
        final long stopPrice,
        final int traderId)
    {
        clientOrderId = BuilderUtil.fill(clientOrderId, NewOrderEncoder.clientOrderIdLength());

        DirectBuffer directBuffer = newOrderBuilder.compID(clientData.getCompID())
                .clientOrderId(clientOrderId.getBytes())
                .securityId(securityId)
                .orderType(OrdTypeEnum.valueOf(orderType))
                .timeInForce(TimeInForceEnum.valueOf(timeInForce))
                .side(SideEnum.valueOf(side))
                .orderQuantity((int) volume)
                .displayQuantity((int) displayQuantity)
                .minQuantity((int) minQuantity)
                .limitPrice(price)
                .stopPrice(stopPrice)
                .traderId(traderId)
                .build();

        System.out.println("Message=OrderAdd|OrderId=" + clientOrderId.trim() + "|Type=" + orderType + "|Side=" + side + "|Volume=" + volume + "(" + displayQuantity + ")" + "|Price=" + price + "|StopPrice=" + stopPrice + "|TIF=" + timeInForce + "|MES=" + minQuantity);
        return directBuffer;
    }
    public int getNewOrderEncodedLength() {
        return newOrderBuilder.messageEncodedLength();
    }

    public int getCancelOrderEncodedLength() {
        return orderCancelRequestBuilder.getMessageEncodedLength();
    }

    public int getReplaceOrderEncodedLength() {
        return orderCancelReplaceRequestBuilder.getMessageEncodedLength();
    }

    public DirectBuffer cancelOrder(String originalClientOrderId, String side, long price) {
        String origClientOrderId = BuilderUtil.fill(originalClientOrderId, OrderCancelRequestEncoder.origClientOrderIdLength());
        String clientOrderId = BuilderUtil.fill("-" + originalClientOrderId, OrderCancelRequestEncoder.clientOrderIdLength());

        DirectBuffer directBuffer = orderCancelRequestBuilder.compID(clientData.getCompID())
                .clientOrderId(clientOrderId.getBytes())
                .origClientOrderId(origClientOrderId.getBytes())
                .securityId(securityId)
                .side(SideEnum.valueOf(side))
                .limitPrice(price)
                .build();
        System.out.println("Message=OrderCancel|OrderId=" + origClientOrderId.trim());

        return directBuffer;
    }

    public DirectBuffer replaceOrder(
            String originalClientOrderId,
            long volume,
            long price,
            String side,
            String orderType,
            String timeInForce,
            long displayQuantity,
            long minQuantity,
            long stopPrice,
            int traderId)
    {
        //String clientOrderId = BuilderUtil.fill(LocalDateTime.now().toString(), OrderCancelReplaceRequestEncoder.clientOrderIdLength());
        String clientOrderId = BuilderUtil.fill(originalClientOrderId, OrderCancelReplaceRequestEncoder.clientOrderIdLength());
        String origClientOrderId = BuilderUtil.fill(originalClientOrderId, OrderCancelReplaceRequestEncoder.origClientOrderIdLength());

        DirectBuffer directBuffer = orderCancelReplaceRequestBuilder.compID(clientData.getCompID())
                .clientOrderId(clientOrderId.getBytes())
                .origClientOrderId(origClientOrderId.getBytes())
                .securityId(securityId)
                .tradeId(traderId)
                .orderType(OrdTypeEnum.valueOf(orderType))
                .timeInForce(TimeInForceEnum.valueOf(timeInForce))
                .expireTime("20211230-23:00:00".getBytes())
                .side(SideEnum.valueOf(side))
                .orderQuantity((int) volume)
                .displayQuantity((int) displayQuantity)
                .minQuantity((int) minQuantity)
                .limitPrice(price)
                .stopPrice(stopPrice)
                .build();
        System.out.println("Message=OrderModify|Time=" + clientOrderId + "|OrderId=" + origClientOrderId + "|Type=" + orderType + "|Side=" + side + "|Volume=" + volume + "(" + displayQuantity + ")" + "|Price=" + price + "|StopPrice=" + stopPrice + "|TIF=" + timeInForce + "|MES=" + minQuantity);

        return directBuffer;
    }

    public DirectBuffer calcVWAP() {
        DirectBuffer buffer = adminBuilder.compID(clientData.getCompID())
                    .securityId(securityId)
                    .adminMessage(AdminTypeEnum.VWAP)
                    .build();

        return buffer;
    }

    public DirectBuffer lobSnapshot() {
        DirectBuffer buffer = adminBuilder.compID(clientData.getCompID())
                .securityId(securityId)
                .adminMessage(AdminTypeEnum.LOB)
                .build();
        return buffer;
    }

    public DirectBuffer marketDepth() {
        DirectBuffer buffer = adminBuilder.compID(clientData.getCompID())
                .securityId(securityId)
                .adminMessage(AdminTypeEnum.MarketDepth)
                .build();
        return buffer;
    }

    public int getLobSnapshotMessageLength() {
        return adminBuilder.getMessageLength();
    }

    public void setBid(long bid) {
        this.bid = bid;
    }

    public long getBid(){
        return bid;
    }

    public long getOffer(){
        return offer;
    }

    public void setOffer(long offer) {
        this.offer = offer;
    }

    public long getBidQuantity() { return bidQuantity; }

    public void setBidQuantity(long bidQuantity) {
        this.bidQuantity = bidQuantity;
    }

    public long getOfferQuantity() {
        return offerQuantity;
    }

    public void setOfferQuantity(long offerQuantity) {
        this.offerQuantity = offerQuantity;
    }

    public int getSecurityId() {
        return securityId;
    }

    public void setSecurityId(int securityId) {
        this.securityId = securityId;
    }

/*    public GatewayClient getTradingGatewayPub() {
        return tradingGatewayPub;
    }*/

    public boolean isAuction() {
        return auction;
    }

    public void setAuction(boolean auction) {
        this.auction = auction;
    }

/*    public int getClientId(){
        return clientData.getCompID();
    }*/

    public void setStaticPriceReference(long staticPriceReference) {
        this.staticPriceReference = staticPriceReference;
    }

    public long getStaticPriceReference() {
        return staticPriceReference;
    }

    public void setDynamicPriceReference(long dynamicPriceReference) {
        this.dynamicPriceReference = dynamicPriceReference;
    }

    public long getDynamicPriceReference() {
        return dynamicPriceReference;
    }
}
