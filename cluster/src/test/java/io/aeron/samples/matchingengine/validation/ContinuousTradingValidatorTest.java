package io.aeron.samples.matchingengine.validation;

import common.OrderType;
import common.TimeInForce;
import leafNode.OrderEntry;
import org.junit.jupiter.api.Test;
import sbe.msg.NewOrderEncoder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by dharmeshsing on 9/11/15.
 */
public class ContinuousTradingValidatorTest {

    @Test
    public void testOPGOrderRejected(){
        OrderEntry orderEntry = mock(OrderEntry.class);
        when(orderEntry.getType()).thenReturn(OrderType.LIMIT.getOrderType());
        when(orderEntry.getTimeInForce()).thenReturn(TimeInForce.OPG.getValue());

        SessionValidator continuousTradingValidator = new ContinuousTradingValidator();
        boolean result = continuousTradingValidator.isMessageValidForSession(orderEntry, NewOrderEncoder.TEMPLATE_ID);

        assertEquals(false,result);
    }

}