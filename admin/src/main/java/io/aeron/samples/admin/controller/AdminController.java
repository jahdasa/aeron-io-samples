package io.aeron.samples.admin.controller;

import io.aeron.samples.admin.model.ResponseWrapper;
import io.aeron.samples.admin.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Admin controller for the cluster main class, working on a direct connection to the cluster
 */

@RestController
@RequestMapping(path = "/api")
@RequiredArgsConstructor
public class AdminController
{
    private final AdminService adminService;

    /**
     * method to handle the command execution
     */
    @PostMapping(path = "/v1/connect")
    public void connect()
    {
        adminService.connect();
    }

    /**
     * method to handle the command execution
     */
    @PostMapping(path = "/v1/disconnect")
    public void disconnect()
    {
        adminService.disconnect();
    }

    /**
     * method to handle the command execution
     *
     * @param id command name
     * @param name command input
     */
    @PostMapping(path = "/v1/add-participant")
    public void addParticipant(
            @RequestParam(defaultValue = "0") final int id,
            @RequestParam final String name)
    {
        adminService.addParticipant(id, name);
    }

    /**
     * method to handle the command execution
     */
    @GetMapping(path = "/v1/list-participants", produces = "application/json")
    public ResponseWrapper addParticipant()
    {
        return adminService.listParticipants();
    }

    /**
     * method to handle the command execution
     */
    @PostMapping(path = "/v1/add-auction", produces = "application/json")
    public ResponseWrapper addAuction(
        @RequestParam final String name,
        @RequestParam(defaultValue = "0") final int participantId,
        @RequestParam(defaultValue = "25") final int duration)
    {
        return adminService.addAuction(name, participantId, duration);
    }

    /**
     * method to handle the command execution
     */
    @GetMapping(path = "/v1/list-auctions", produces = "application/json")
    public ResponseWrapper listAuctions()
    {
        return adminService.listActions();
    }

    /**
     * method to handle the command execution
     */
    @PostMapping(path = "/v1/add-auction-bid", produces = "application/json")
    public ResponseWrapper addAuctionBid(
            @RequestParam final long auctionId,
            @RequestParam final int participantId,
            @RequestParam final long price)
    {
        return adminService.addAuctionBid(auctionId, participantId, price);
    }

    // placeorder-tid@side@security@clientOrderId@trader@client
    // clientOrderId@security@side@placeOrder@trader@client
    // clientOrderId@security@fc@trader@client
    /**
     * Endpoint to submit a buy limit order
     */
    @PostMapping(path = "/v1/place-order")
    public ResponseWrapper submitOrder(
            @RequestParam(defaultValue = "1") final int securityId,
            @RequestParam final String clientOrderId,
            @RequestParam final long volume,
            @RequestParam final long price,
            @RequestParam final String side,
            @RequestParam final String orderType,
            @RequestParam final String timeInForce,
            @RequestParam final long displayQuantity,
            @RequestParam final long minQuantity,
            @RequestParam final long stopPrice,
            @RequestParam final int traderId,
            @RequestParam final int client
            ) throws Exception
    {
        return adminService.placeOrder(
            securityId,
            clientOrderId,
            volume,
            price,
            side,
            orderType,
            timeInForce,
            displayQuantity,
            minQuantity,
            stopPrice,
            traderId,
            client);
    }

    // admin-tid@type@security@reqid@trader@client
    /**
     * Endpoint to submit a buy limit order
     */
    @GetMapping(path = "/v1/submit-admin-message")
    public void submitAdminMessage(
            @RequestParam final long requestId,
            @RequestParam(defaultValue = "1") final int securityId,
            @RequestParam final String adminMessageType,
            @RequestParam final long trader,
            @RequestParam final int client
            ) throws Exception {
        adminService.submitAdminMessage(securityId, adminMessageType);
    }

    /**
     * Endpoint to submit a buy limit order
     */
    @GetMapping(path = "/v1/bbo")
    public void bestBidOffer() throws Exception {
        adminService.bbo();
    }

    // cancelorder-tid@side@security@clientOrderId@trader@client
    /**
     * Endpoint to cancel an order
     */
    @PostMapping(path = "/v1/cancel-order")
    public void cancelOrder(
            @RequestParam(defaultValue = "1") final int securityId,
            @RequestParam final String clientOrderId,
            @RequestParam final String side,
            @RequestParam final long price
            ) throws Exception {
        adminService.cancelOrder(securityId, clientOrderId, side, price);
    }


    // cancelorder-tid@side@security@clientOrderId@trader@client
    @PostMapping(path = "/v1/replace-order")
    public void replaceOrder(
            @RequestParam(defaultValue = "1") final int securityId,
            @RequestParam final String clientOrderId,
            @RequestParam final long volume,
            @RequestParam final long price,
            @RequestParam final String side,
            @RequestParam final String orderType,
            @RequestParam final String timeInForce,
            @RequestParam final long displayQuantity,
            @RequestParam final long minQuantity,
            @RequestParam final long stopPrice,
            @RequestParam final int traderId) throws Exception
    {
        adminService.replaceOrder(securityId,
                clientOrderId,
                volume,
                price,
                side,
                orderType,
                timeInForce,
                displayQuantity,
                minQuantity,
                stopPrice,
                traderId);
    }
}
