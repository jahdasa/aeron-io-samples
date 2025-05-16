import http from 'k6/http';
import { check, sleep } from 'k6';


function getRandomNumber(min, max)
{
    return Math.floor(Math.random() * (max - min + 1)) + min;
}

function randomClientOrderId()
{
    const prefix = 10000000000;
    const randomDigits = Math.floor(Math.random() * 1e10);

    return prefix + randomDigits;
}

function randomPrice()
{
    const prefix = 10000;
    const randomDigits = Math.floor(Math.random() * 1e2);

    return prefix + randomDigits;
}

function randomVolume()
{
    const prefix = 100;
    const randomDigits = Math.floor(Math.random() * 1e3);

    return prefix + randomDigits;
}

function randomSide()
{
    const sides = ['Buy', 'Sell'];
    return sides[Math.floor(Math.random() * sides.length)];
}

export default function () {
    const url = 'http://localhost:8080/api/v1/place-order';

    const volume = randomVolume();
    const payload = {
        // securityId: getRandomNumber(101, 109),
        securityId: 101,
        clientOrderId: randomClientOrderId(),
        volume: volume,
        price: randomPrice(),
        side: randomSide(),
        orderType: 'Limit',
        timeInForce: 'Day',
        displayQuantity: volume,
        minQuantity: '0',
        stopPrice: '0',
        traderId: 1,
        client: 1
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