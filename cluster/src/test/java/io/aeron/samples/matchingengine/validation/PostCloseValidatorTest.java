package io.aeron.samples.matchingengine.validation;

import leafNode.OrderEntry;
import org.junit.jupiter.api.Test;
import sbe.msg.NewOrderEncoder;
import sbe.msg.OrderCancelReplaceRequestEncoder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Created by dharmeshsing on 9/11/15.
 */
public class PostCloseValidatorTest {

    @Test
    public void testNewOrderRejected(){
        OrderEntry orderEntry = mock(OrderEntry.class);
        SessionValidator postCloseValidator = new PostCloseValidator();
        boolean result = postCloseValidator.isMessageValidForSession(orderEntry, NewOrderEncoder.TEMPLATE_ID);

        assertEquals(false,result);
    }

    @Test
    public void testAmendRejected(){
        OrderEntry orderEntry = mock(OrderEntry.class);
        SessionValidator postCloseValidator = new PostCloseValidator();
        boolean result = postCloseValidator.isMessageValidForSession(orderEntry, OrderCancelReplaceRequestEncoder.TEMPLATE_ID);

        assertEquals(false,result);
    }

}