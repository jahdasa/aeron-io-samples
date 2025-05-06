import http from 'k6/http';
import { check, sleep } from 'k6';

function generateId(serial)
{
    const now = new Date();

    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0'); // Months are zero-based
    const day = String(now.getDate()).padStart(2, '0');
    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');
    const seconds = String(now.getSeconds()).padStart(2, '0');

    const timestamp = `${year}${month}${day}${hours}${minutes}${seconds}`;

    return `ammo-${timestamp}-${serial}`;
}

let latestSerial = Math.random();

function randomCISIN()
{
    const prefix = 1129 * 10000000000;
    const randomDigits = Math.floor(Math.random() * 1e10);

    return prefix + randomDigits;
}

function randomClientOrderId()
{
    const prefix = 10000000000;
    const randomDigits = Math.floor(Math.random() * 1e10);

    return prefix + randomDigits;
}

/*
export default function ()
{
    const requestId = generateId(latestSerial);
    latestSerial++;

    const baseUrl = 'https://ammoapistage.emofid.com/virtual-portfolio-experience/api/v1';

    const cisin = randomCISIN();

    const buyPayload = JSON.stringify({
        requestId: requestId,
        cisin: cisin,
        customerName : "کیخسرو " + cisin,
        amount: 10000000000,
        modelCode: 100234,
        "activities": {
            "blockBalance": false,
            "createInBackoffice": true,
            "createEndUserPortfolioInAmmo": true,
            "createVirtualPortfolioInAmmo": false,
            "connectToModel": false,
            "getAndSendPortfolioComposition": false,
            "rebalance": false,
            "activateVirtualPortfolio": false,
            "lockAmountVirtualBackOffice": false,
            "sendOrderDraftToEms": false
        }
    });

    const headers = {
        'Content-Type': 'application/json',
    };

    const buyResponse = http.post(`${baseUrl}/purchase`, buyPayload, { headers: headers });
    check(buyResponse, {
        'Buy request was successful': (r) => r.status === 200,
    });

    // Sleep for a short period between iterations
    sleep(0.9);
}*/

export default function () {
    const url = 'http://localhost:8080/api/v1/submit-order';

    const payload = {
        clientOrderId: randomClientOrderId(),
        volume: '1000',
        price: '500',
        side: 'Buy',
        orderType: 'Limit',
        timeInForce: 'Day',
        displayQuantity: '1000',
        minQuantity: '0',
        stopPrice: '0',
    };

    const headers = {
        'Content-Type': 'application/x-www-form-urlencoded',
    };

    const response = http.post(url, payload, { headers: headers });

    check(response, {
        'Request was successful': (r) => r.status === 200,
    });

    //sleep(1); // Pause between iterations
}