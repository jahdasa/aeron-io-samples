package io.aeron.samples.matchingengine.validation;

import leafNode.OrderEntry;
import org.junit.jupiter.api.Test;
import sbe.msg.NewOrderEncoder;
import sbe.msg.OrderCancelReplaceRequestEncoder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Created by dharmeshsing on 10/11/15.
 */
public class HaltSessionValidatorTest {
    @Test
    public void testNewOrderRejected(){
        OrderEntry orderEntry = mock(OrderEntry.class);
        SessionValidator haltSessionValidator = new HaltSessionValidator();
        boolean result = haltSessionValidator.isMessageValidForSession(orderEntry, NewOrderEncoder.TEMPLATE_ID);

        assertEquals(false,result);
    }

    @Test
    public void testAmendRejected(){
        OrderEntry orderEntry = mock(OrderEntry.class);
        SessionValidator haltSessionValidator = new HaltSessionValidator();
        boolean result = haltSessionValidator.isMessageValidForSession(orderEntry, OrderCancelReplaceRequestEncoder.TEMPLATE_ID);

        assertEquals(false,result);
    }

}