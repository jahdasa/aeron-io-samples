
# Matching Engine

Based on open source CoinTossX exchange
https://arxiv.org/pdf/2102.10925
https://github.com/dharmeshsing/CoinTossX

## Local

- run `./gradlew` to build the code
- in one terminal, run `./gradlew runSingleNodeCluster`
- in another terminal, run the admin application. See [admin readme](admin/readme.md) for more details.

# Development requirements

- Java 21
- Gradle 8.5

# Linux
du@LAPTOP-1CS3JJSU:/mnt/c/code/github/aeron-io-samples$ ./cluster/build/distributions/cluster/bin/cluster --add-opens java.base/sun.nio.ch=ALL-UNNAMED io.aeron.samples.ClusterApp

# Useful commands & Options
netstat -lntu
sudo ncat -l 9999
wsl hostname -I
--add-opens java.base/sun.nio.ch=ALL-UNNAMED
-XX:+UnlockExperimentalVMOptions -XX:+UseZGC -XX:MaxGCPauseMillis=1

# Compile
Build & Install ObjectLayout project [https://github.com/jahdasa/ObjectLayout]
mvn build install
update DATA_PATH in two MatchingEngine.properties files:

DATA_PATH=D:\\Users\\r.jahdasa\\code\\aeron-io-samples\\data -> DATA_PATH: $HOME/data


# Run
1. run cluster
2. run admin
3. call connect endpoint
4. place/replace order
5. get LOB/vwap/bbo/market depth


# curl

1. connect
curl --location --request POST 'localhost:8080/api/v1/connect'

2. place order sell
curl --location 'http://localhost:8080/api/v1/place-order' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode 'clientOrderId=3209' \
--data-urlencode 'volume=901000' \
--data-urlencode 'price=10000000' \
--data-urlencode 'side=Sell' \
--data-urlencode 'orderType=Limit' \
--data-urlencode 'timeInForce=Day' \
--data-urlencode 'displayQuantity=602000' \
--data-urlencode 'minQuantity=0' \
--data-urlencode 'stopPrice=0' \
--data-urlencode 'traderId=1' \
--data-urlencode 'client=1'

3. place order buy
curl --location 'http://localhost:8080/api/v1/place-order' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode 'securityId=1' \
--data-urlencode 'clientOrderId=55770' \
--data-urlencode 'volume=1000' \
--data-urlencode 'price=40000000' \
--data-urlencode 'side=Buy' \
--data-urlencode 'orderType=Limit' \
--data-urlencode 'timeInForce=Day' \
--data-urlencode 'displayQuantity=1000' \
--data-urlencode 'minQuantity=0' \
--data-urlencode 'stopPrice=0' \
--data-urlencode 'traderId=1' \
--data-urlencode 'client=1'

4. replace order
curl --location 'http://localhost:8080/api/v1/replace-order' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode 'securityId=1' \
--data-urlencode 'clientOrderId=3209' \
--data-urlencode 'volume=1000' \
--data-urlencode 'price=10000000' \
--data-urlencode 'side=Sell' \
--data-urlencode 'orderType=Limit' \
--data-urlencode 'timeInForce=Day' \
--data-urlencode 'displayQuantity=1000' \
--data-urlencode 'minQuantity=0' \
--data-urlencode 'stopPrice=0' \
--data-urlencode 'traderId=1' \
--data-urlencode 'client=1'

5. get LOB
curl --location 'http://localhost:8080/api/v1/submit-admin-message?securityId=1&adminMessageType=LOB&trader=1&client=1&requestId=1'

6. get VWAP
curl --location 'http://localhost:8080/api/v1/submit-admin-message?requestId=1&securityId=1&adminMessageType=VWAP&trader=1&client=1'

7. get BBO
curl --location 'http://localhost:8080/api/v1/submit-admin-message?requestId=1&securityId=1&adminMessageType=BestBidOfferRequest&trader=1&client=1'

8. get Market Depth
curl --location 'http://localhost:8080/api/v1/submit-admin-message?securityId=1&adminMessageType=MarketDepth&requestId=1&trader=1&client=1'

9. get cancel order
curl --location 'http://localhost:8080/api/v1/cancel-order' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode 'securityId=1' \
--data-urlencode 'clientOrderId=3204' \
--data-urlencode 'side=Sell' \
--data-urlencode 'price=10000000' \
--data-urlencode 'traderId=1' \
--data-urlencode 'client=1'

# todo
1. Support Idempotency
2. Connected Ready
3. clean code
4. improve performance
5. test functions
6. stream execution events

# logs
2025-05-12T14:26:38.342+03:30  INFO 29588 --- [AdminApp] [nio-8080-exec-7] i.a.samples.admin.service.AdminService   : CorrelationId: 91@VWAP@1@1@1@1
2025-05-12T14:26:38.344+03:30  INFO 29588 --- [AdminApp] [teraction-agent] i.a.s.admin.cluster.LineReaderHelper     : Process admin message: 91
2025-05-12T14:26:38.344+03:30  INFO 29588 --- [AdminApp] [teraction-agent] i.a.s.a.cluster.PendingMessageManager    : addMessage to trackedMessages correlationId: 91@VWAP@1@1@1@1, messageType: admin-message
2025-05-12T14:26:38.354+03:30  INFO 29588 --- [AdminApp] [teraction-agent] i.a.s.admin.cluster.LineReaderHelper     : Unit header: 18 1 messages in group: 1
2025-05-12T14:26:38.354+03:30  INFO 29588 --- [AdminApp] [teraction-agent] i.a.s.admin.cluster.LineReaderHelper     : VWAP: 95 bidVWAP/offerVWAP: 0.0@2999.4452
2025-05-12T14:26:38.354+03:30  INFO 29588 --- [AdminApp] [teraction-agent] i.a.s.a.cluster.PendingMessageManager    : markMessageAsReceived correlationId: 91@VWAP@1@1@1@1
2025-05-12T14:26:38.354+03:30  INFO 29588 --- [AdminApp] [nio-8080-exec-7] i.a.samples.admin.service.AdminService   : Response: VWAPDTO(bidVWAP=0.0, offerVWAP=2999.4452)
2025-05-12T14:26:39.046+03:30  INFO 29588 --- [AdminApp] [io-8080-exec-10] i.a.samples.admin.service.AdminService   : CorrelationId: 91@VWAP@1@1@1@1
2025-05-12T14:26:39.047+03:30  INFO 29588 --- [AdminApp] [teraction-agent] i.a.s.admin.cluster.LineReaderHelper     : Process admin message: 91
2025-05-12T14:26:39.047+03:30  INFO 29588 --- [AdminApp] [teraction-agent] i.a.s.a.cluster.PendingMessageManager    : addMessage to trackedMessages correlationId: 91@VWAP@1@1@1@1, messageType: admin-message
2025-05-12T14:26:39.065+03:30  INFO 29588 --- [AdminApp] [teraction-agent] i.a.s.admin.cluster.LineReaderHelper     : Unit header: 18 1 messages in group: 1
2025-05-12T14:26:39.065+03:30  INFO 29588 --- [AdminApp] [teraction-agent] i.a.s.admin.cluster.LineReaderHelper     : VWAP: 95 bidVWAP/offerVWAP: 0.0@2999.4452
2025-05-12T14:26:39.065+03:30  INFO 29588 --- [AdminApp] [teraction-agent] i.a.s.a.cluster.PendingMessageManager    : markMessageAsReceived correlationId: 91@VWAP@1@1@1@1
2025-05-12T14:26:39.065+03:30  INFO 29588 --- [AdminApp] [io-8080-exec-10] i.a.samples.admin.service.AdminService   : Response: VWAPDTO(bidVWAP=0.0, offerVWAP=2999.4452)
2025-05-12T14:26:39.741+03:30  INFO 29588 --- [AdminApp] [nio-8080-exec-1] i.a.samples.admin.service.AdminService   : CorrelationId: 91@VWAP@1@1@1@1
2025-05-12T14:26:39.742+03:30  INFO 29588 --- [AdminApp] [teraction-agent] i.a.s.admin.cluster.LineReaderHelper     : Process admin message: 91
2025-05-12T14:26:39.742+03:30  INFO 29588 --- [AdminApp] [teraction-agent] i.a.s.a.cluster.PendingMessageManager    : addMessage to trackedMessages correlationId: 91@VWAP@1@1@1@1, messageType: admin-message
2025-05-12T14:26:39.753+03:30  INFO 29588 --- [AdminApp] [teraction-agent] i.a.s.admin.cluster.LineReaderHelper     : Unit header: 18 1 messages in group: 1
2025-05-12T14:26:39.753+03:30  INFO 29588 --- [AdminApp] [teraction-agent] i.a.s.admin.cluster.LineReaderHelper     : VWAP: 95 bidVWAP/offerVWAP: 0.0@2999.4452
2025-05-12T14:26:39.753+03:30  INFO 29588 --- [AdminApp] [teraction-agent] i.a.s.a.cluster.PendingMessageManager    : markMessageAsReceived correlationId: 91@VWAP@1@1@1@1
2025-05-12T14:26:39.753+03:30  INFO 29588 --- [AdminApp] [nio-8080-exec-1] i.a.samples.admin.service.AdminService   : Response: VWAPDTO(bidVWAP=0.0, offerVWAP=2999.4452)
2025-05-12T14:26:40.390+03:30  INFO 29588 --- [AdminApp] [nio-8080-exec-2] i.a.samples.admin.service.AdminService   : CorrelationId: 91@VWAP@1@1@1@1
2025-05-12T14:26:40.392+03:30  INFO 29588 --- [AdminApp] [teraction-agent] i.a.s.admin.cluster.LineReaderHelper     : Process admin message: 91
2025-05-12T14:26:40.392+03:30  INFO 29588 --- [AdminApp] [teraction-agent] i.a.s.a.cluster.PendingMessageManager    : addMessage to trackedMessages correlationId: 91@VWAP@1@1@1@1, messageType: admin-message
2025-05-12T14:26:40.418+03:30  INFO 29588 --- [AdminApp] [teraction-agent] i.a.s.admin.cluster.LineReaderHelper     : Unit header: 18 1 messages in group: 1
2025-05-12T14:26:40.418+03:30  INFO 29588 --- [AdminApp] [teraction-agent] i.a.s.admin.cluster.LineReaderHelper     : VWAP: 95 bidVWAP/offerVWAP: 0.0@2999.4452
2025-05-12T14:26:40.418+03:30  INFO 29588 --- [AdminApp] [teraction-agent] i.a.s.a.cluster.PendingMessageManager    : markMessageAsReceived correlationId: 91@VWAP@1@1@1@1
2025-05-12T14:26:40.418+03:30  INFO 29588 --- [AdminApp] [nio-8080-exec-2] i.a.samples.admin.service.AdminService   : Response: VWAPDTO(bidVWAP=0.0, offerVWAP=2999.4452)
2025-05-12T14:26:41.082+03:30  INFO 29588 --- [AdminApp] [nio-8080-exec-3] i.a.samples.admin.service.AdminService   : CorrelationId: 91@VWAP@1@1@1@1
2025-05-12T14:26:41.084+03:30  INFO 29588 --- [AdminApp] [teraction-agent] i.a.s.admin.cluster.LineReaderHelper     : Process admin message: 91
2025-05-12T14:26:41.084+03:30  INFO 29588 --- [AdminApp] [teraction-agent] i.a.s.a.cluster.PendingMessageManager    : addMessage to trackedMessages correlationId: 91@VWAP@1@1@1@1, messageType: admin-message
2025-05-12T14:26:41.096+03:30  INFO 29588 --- [AdminApp] [teraction-agent] i.a.s.admin.cluster.LineReaderHelper     : Unit header: 18 1 messages in group: 1
2025-05-12T14:26:41.096+03:30  INFO 29588 --- [AdminApp] [teraction-agent] i.a.s.admin.cluster.LineReaderHelper     : VWAP: 95 bidVWAP/offerVWAP: 0.0@2999.4452
2025-05-12T14:26:41.096+03:30  INFO 29588 --- [AdminApp] [teraction-agent] i.a.s.a.cluster.PendingMessageManager    : markMessageAsReceived correlationId: 91@VWAP@1@1@1@1
2025-05-12T14:26:41.096+03:30  INFO 29588 --- [AdminApp] [nio-8080-exec-3] i.a.samples.admin.service.AdminService   : Response: VWAPDTO(bidVWAP=0.0, offerVWAP=2999.4452)
2025-05-12T14:26:55.702+03:30  INFO 29588 --- [AdminApp] [nio-8080-exec-4] i.a.samples.admin.service.AdminService   : CorrelationId: 91@LOB@1@1@1@1
2025-05-12T14:26:55.703+03:30  INFO 29588 --- [AdminApp] [teraction-agent] i.a.s.admin.cluster.LineReaderHelper     : Process admin message: 91
2025-05-12T14:26:55.703+03:30  INFO 29588 --- [AdminApp] [teraction-agent] i.a.s.a.cluster.PendingMessageManager    : addMessage to trackedMessages correlationId: 91@LOB@1@1@1@1, messageType: admin-message
2025-05-12T14:26:55.715+03:30  INFO 29588 --- [AdminApp] [teraction-agent] i.a.s.admin.cluster.LineReaderHelper     : Unit header: 18 0 messages in group: 1
2025-05-12T14:26:55.715+03:30  INFO 29588 --- [AdminApp] [teraction-agent] i.a.s.admin.cluster.LineReaderHelper     : Admin Message:91 adminTypeEnum: StartLOB securityId: 1
2025-05-12T14:26:55.715+03:30  INFO 29588 --- [AdminApp] [teraction-agent] i.a.s.a.cluster.PendingMessageManager    : markMessageAsReceived correlationId: 91@LOB@1@1@1@1
2025-05-12T14:26:55.715+03:30  INFO 29588 --- [AdminApp] [teraction-agent] i.a.s.admin.cluster.LineReaderHelper     : Admin Message:91 adminTypeEnum: EndLOB securityId: 1
2025-05-12T14:26:55.715+03:30  INFO 29588 --- [AdminApp] [teraction-agent] i.a.s.a.cluster.PendingMessageManager    : markMessageAsReceived correlationId: 91@LOB@1@1@1@1
2025-05-12T14:26:55.715+03:30  INFO 29588 --- [AdminApp] [nio-8080-exec-4] i.a.samples.admin.service.AdminService   : Response: LimitOrderBookDTO(securityId=1, orders=[LimitOrderBookDTO.OrderDTO(clientOrderId=3209                , orderId=12, price=1000.0, side=Sell, quantity=1000), LimitOrderBookDTO.OrderDTO(clientOrderId=3201                , orderId=6, price=2000.0, side=Sell, quantity=901000), LimitOrderBookDTO.OrderDTO(clientOrderId=3200                , orderId=3, price=3000.0, side=Sell, quantity=900950), LimitOrderBookDTO.OrderDTO(clientOrderId=3201                , orderId=5, price=3000.0, side=Sell, quantity=901000), LimitOrderBookDTO.OrderDTO(clientOrderId=3203                , orderId=7, price=4000.0, side=Sell, quantity=901000)])
Message=OrderCancel|OrderId=3204
2025-05-12T14:28:59.824+03:30  INFO 29588 --- [AdminApp] [nio-8080-exec-9] i.a.samples.admin.service.AdminService   : CorrelationId: 9@2@1@-3204@1@1
2025-05-12T14:28:59.826+03:30  INFO 29588 --- [AdminApp] [teraction-agent] i.a.s.admin.cluster.LineReaderHelper     : Process new order10
2025-05-12T14:28:59.826+03:30  INFO 29588 --- [AdminApp] [teraction-agent] i.a.s.a.cluster.PendingMessageManager    : addMessage to trackedMessages correlationId: 9@2@1@-3204@1@1, messageType: cancel-order
2025-05-12T14:28:59.839+03:30  INFO 29588 --- [AdminApp] [teraction-agent] i.a.s.admin.cluster.LineReaderHelper     : Execution report: 13 partitionId: 0 sequenceNumber: 13 executionID: 'Exec1747047539834    ' clientOrderId: '-3204               ' orderId: 13 executionTypeEnum: Cancelled orderStatusEnum: Filled rejectCode: NULL_VAL leavesQuantity: 0 container: Main securityId: 1 side: Sell traderId: '1' account: 'test123   ' marketOpsRequest: 'No' transactTime: '1747047539834' orderBookEnum: 'Regular'
2025-05-12T14:28:59.839+03:30  INFO 29588 --- [AdminApp] [teraction-agent] i.a.s.admin.cluster.LineReaderHelper     : Unit header: 18 1 messages in group: 1
2025-05-12T14:28:59.839+03:30  INFO 29588 --- [AdminApp] [teraction-agent] i.a.s.admin.cluster.LineReaderHelper     : BBO: 26 security 1 bidQuantity/bid: 0@0.0 offerQuantity/offerValue: 1000@1000.0
2025-05-12T14:28:59.839+03:30  INFO 29588 --- [AdminApp] [teraction-agent] i.a.s.a.cluster.PendingMessageManager    : markMessageAsReceived correlationId: 91@BestBidOfferRequest@1@1@1@1
2025-05-12T14:28:59.839+03:30  INFO 29588 --- [AdminApp] [teraction-agent] i.a.s.admin.cluster.LineReaderHelper     : Order view: 93 securityId: 1 clientOrderId: '-3204               ' orderId: 13 submittedTime: 1747047539834 side: Sell orderQuantity: 0 price: 1000.0 traderId: 1
2025-05-12T14:28:59.839+03:30  INFO 29588 --- [AdminApp] [teraction-agent] i.a.s.a.cluster.PendingMessageManager    : markMessageAsReceived correlationId: 9@2@1@-3204@1@1
2025-05-12T14:28:59.839+03:30  INFO 29588 --- [AdminApp] [teraction-agent] i.a.s.admin.cluster.LineReaderHelper     : correlationId: 9@2@1@-3204@1@1
2025-05-12T14:28:59.839+03:30  INFO 29588 --- [AdminApp] [nio-8080-exec-9] i.a.samples.admin.service.AdminService   : Response: OrderViewResponse(correlationId=9@2@1@-3204@1@1, securityId=1, traderId=1, clientOrderId=-3204               , orderId=13, submittedTime=1747047539834, priceValue=1000.0, orderQuantity=0, side=Sell)


## Step by step build
To skip a part, use -x, for example `-x check` to ignore check styles or `-x test` to ignore tests.

```bash
./gradlew messages:build
./gradlew cluster-protocol:build
./gradlew limit-order-book:build
./gradlew cluster:build
./gradlew admin:build
```

## Run single cluster node and admin in two separate terminals
```bash
./gradlew runSingleNodeCluster

./gradlew admin:run
```

## Load test with k6
Make sure you are connected to the cluster before running the test (either with curl or postman)

```bash
k6 run --vus 10 --duration 30s .k6-scripts/001.placeorder.js
```