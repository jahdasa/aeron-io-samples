package io.aeron.samples.admin.controller;

import io.aeron.samples.admin.model.ResponseWrapper;
import io.aeron.samples.admin.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    // placeorder-tid@side@security@clientOrderId@trader@client
    // clientOrderId@security@side@placeOrder@trader@client
    // clientOrderId@security@fc@trader@client
    /**
     * Endpoint to submit a buy limit order
     */
    @PostMapping(path = "/v1/place-order")
    public ResponseEntity<ResponseWrapper> placeOrder(
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
        final ResponseWrapper responseWrapper = adminService.placeOrder(
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

        HttpStatus status = HttpStatus.OK;
        if(responseWrapper.getStatus() < 0) {
            status = HttpStatus.BAD_GATEWAY;
        }
        return new ResponseEntity<>(responseWrapper, status);
    }

    // admin-tid@type@security@reqid@trader@client
    /**
     * Endpoint to submit a buy limit order
     */
    @GetMapping(path = "/v1/submit-admin-message")
    public ResponseWrapper submitAdminMessage(
            @RequestParam final long requestId,
            @RequestParam(defaultValue = "1") final int securityId,
            @RequestParam final String adminMessageType,
            @RequestParam final long trader,
            @RequestParam final int client
            ) throws Exception {
        return adminService.submitAdminMessage(securityId, adminMessageType, requestId, trader, client);
    }

    // cancelorder-tid@side@security@clientOrderId@trader@client
    /**
     * Endpoint to cancel an order
     */
    @PostMapping(path = "/v1/cancel-order")
    public ResponseWrapper cancelOrder(
            @RequestParam(defaultValue = "1") final int securityId,
            @RequestParam final String clientOrderId,
            @RequestParam final String side,
            @RequestParam final long price,
            @RequestParam final int traderId,
            @RequestParam final int client
            ) throws Exception
    {
        return adminService.cancelOrder(securityId, clientOrderId, side, price, traderId, client);
    }


    // replaceorder-tid@side@security@clientOrderId@trader@client
    @PostMapping(path = "/v1/replace-order")
    public ResponseWrapper replaceOrder(
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
            @RequestParam final int client) throws Exception
    {
        return adminService.replaceOrder(securityId,
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

    // newinstrument-tid@security@code@client
    @PostMapping(path = "/v1/new-instrument")
    public ResponseWrapper newInstrument(
            @RequestParam final int securityId,
            @RequestParam final String code,
            @RequestParam final String name,
            @RequestParam final int client) throws Exception
    {
        return adminService.newInstrument(
            securityId,
            code,
            name,
            client);
    }
}
