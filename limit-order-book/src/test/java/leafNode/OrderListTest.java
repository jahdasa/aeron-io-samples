package leafNode;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import unsafe.UnsafeUtil;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OrderListTest {
    private OrderListImpl orderList;
    private Date currentDate;
    private OrderEntry flyWeight;

    @BeforeEach
    public void setUp(){
        orderList = new OrderListImpl();
        currentDate = Calendar.getInstance().getTime();
        flyWeight = new OrderEntry();
    }

    @AfterEach
    public void tearDown(){
        orderList.free();
        flyWeight = null;
    }

    @Test
    public void testEmptyList() throws Exception {

        long address = UnsafeUtil.getUnsafe().allocateMemory(OrderEntry.getObjectSize());
        OrderEntry orderEntry = new OrderEntry();
        orderEntry.setObjectOffset(address);
        orderEntry.init();

        orderEntry.setSide((byte) 0);
        orderEntry.setOrderId(0);
        orderEntry.setType((byte) 0);
        orderEntry.setSubmittedTime(0);

        orderEntry.setQuantity(0);
        orderEntry.setMinExecutionSize(0);
        orderEntry.setPrice(0);
        orderEntry.setStopPrice(0);
        orderEntry.setExecuteVolume(0);
        orderEntry.setTimeInForce((byte) 0);
        orderEntry.setDisplayQuantity(0);
        orderEntry.setExpireTime(0);
        orderEntry.setTrader(0);

        for(int i=0; i<orderList.size(); i++){
            orderList.get(i,flyWeight);
            assertEquals(orderEntry,flyWeight);
        }
    }

    @Test
    public void testGet() throws Exception {
        OrderEntry orderEntry1 = TestOrderEntryFactory.createOrderEntry("10:00");
        OrderEntry orderEntry2 = TestOrderEntryFactory.createOrderEntry("11:00");
        OrderEntry orderEntry3 = TestOrderEntryFactory.createOrderEntry("12:00");

        try {

            orderList.add(orderEntry1);
            orderList.add(orderEntry2);
            orderList.add(orderEntry3);

            orderList.get(1, flyWeight);
            assertEquals(orderEntry2, flyWeight);
        }finally {
            UnsafeUtil.freeOrderEntryMemory(orderEntry1);
            UnsafeUtil.freeOrderEntryMemory(orderEntry2);
            UnsafeUtil.freeOrderEntryMemory(orderEntry3);
        }

    }

    @Test
    public void testIterator() throws Exception {
        OrderEntry orderEntry1 = TestOrderEntryFactory.createOrderEntry("10:00");
        OrderEntry orderEntry2 = TestOrderEntryFactory.createOrderEntry("11:00");
        OrderEntry orderEntry3 = TestOrderEntryFactory.createOrderEntry("12:00");

        try {
            orderList.add(orderEntry1);
            orderList.add(orderEntry2);
            orderList.add(orderEntry3);

            for(OrderListCursor oe : orderList){
                assertEquals(1, oe.value.getOrderId());
            }

        }finally {
            UnsafeUtil.freeOrderEntryMemory(orderEntry1);
            UnsafeUtil.freeOrderEntryMemory(orderEntry2);
            UnsafeUtil.freeOrderEntryMemory(orderEntry3);
        }

    }

    @Test
    public void testSize() throws Exception {
        OrderEntry orderEntry1 = TestOrderEntryFactory.createOrderEntry("10:00");
        OrderEntry orderEntry2 = TestOrderEntryFactory.createOrderEntry("11:00");
        OrderEntry orderEntry3 = TestOrderEntryFactory.createOrderEntry("12:00");

        try {
            orderList.add(orderEntry1);
            orderList.add(orderEntry2);
            orderList.add(orderEntry3);

            assertEquals(3, orderList.size());
        }finally {
            UnsafeUtil.freeOrderEntryMemory(orderEntry1);
            UnsafeUtil.freeOrderEntryMemory(orderEntry2);
            UnsafeUtil.freeOrderEntryMemory(orderEntry3);
        }
    }

    @Test
    public void testAdd() throws Exception {
        OrderEntry orderEntry = TestOrderEntryFactory.createOrderEntry("10:00");

        try {
            orderList.add(orderEntry);
            orderList.get(0, flyWeight);
            assertEquals(orderEntry, flyWeight);
        }finally {
            UnsafeUtil.freeOrderEntryMemory(orderEntry);
        }
    }

    @Test
    public void testOrder() throws Exception {
        OrderEntry orderEntry1 = TestOrderEntryFactory.createOrderEntry("10:00");
        OrderEntry orderEntry2 = TestOrderEntryFactory.createOrderEntry("11:00");
        OrderEntry orderEntry3 = TestOrderEntryFactory.createOrderEntry("12:00");

        try {

            orderList.add(orderEntry3);
            orderList.add(orderEntry1);
            orderList.add(orderEntry2);

            orderList.get(0, flyWeight);
            assertEquals(orderEntry1, flyWeight);

            orderList.get(1, flyWeight);
            assertEquals(orderEntry2, flyWeight);

            orderList.get(2, flyWeight);
            assertEquals(orderEntry3, flyWeight);
        }finally {
            UnsafeUtil.freeOrderEntryMemory(orderEntry1);
            UnsafeUtil.freeOrderEntryMemory(orderEntry2);
            UnsafeUtil.freeOrderEntryMemory(orderEntry3);
        }
    }

    @Test
    public void testOrder2() throws Exception {
        OrderEntry orderEntry1 = TestOrderEntryFactory.createOrderEntry(System.nanoTime());
        OrderEntry orderEntry2 = TestOrderEntryFactory.createOrderEntry(System.nanoTime());
        OrderEntry orderEntry3 = TestOrderEntryFactory.createOrderEntry(System.nanoTime());

        try {

            orderList.add(orderEntry1);
            orderList.add(orderEntry2);
            orderList.add(orderEntry3);

            orderList.get(0, flyWeight);
            assertEquals(orderEntry1, flyWeight);

            orderList.get(1, flyWeight);
            assertEquals(orderEntry2, flyWeight);

            orderList.get(2, flyWeight);
            assertEquals(orderEntry3, flyWeight);
        }finally {
            UnsafeUtil.freeOrderEntryMemory(orderEntry1);
            UnsafeUtil.freeOrderEntryMemory(orderEntry2);
            UnsafeUtil.freeOrderEntryMemory(orderEntry3);
        }
    }

    @Test
    public void testRemove() throws Exception {
        OrderEntry orderEntry1 = TestOrderEntryFactory.createOrderEntry("10:00");
        OrderEntry orderEntry2 = TestOrderEntryFactory.createOrderEntry("11:00");
        OrderEntry orderEntry3 = TestOrderEntryFactory.createOrderEntry("12:00");

        try {
            orderList.add(orderEntry1);
            orderList.add(orderEntry2);
            orderList.add(orderEntry3);

            orderList.get(0, flyWeight);
            assertEquals(orderEntry1, flyWeight, "Initial Order 0");
            orderList.get(1, flyWeight);
            assertEquals(orderEntry2, flyWeight, "Initial Order 1");
            orderList.get(2, flyWeight);
            assertEquals(orderEntry3, flyWeight, "Initial Order 2");

            orderList.remove(1);

            orderList.get(0, flyWeight);
            assertEquals(orderEntry1, flyWeight, "Expected Order 0");

            orderList.get(1, flyWeight);
            assertEquals(orderEntry3, flyWeight, "Expected Order 1");

            assertEquals(2, orderList.size(), "Size");
        }finally {
            UnsafeUtil.freeOrderEntryMemory(orderEntry1);
            UnsafeUtil.freeOrderEntryMemory(orderEntry2);
            UnsafeUtil.freeOrderEntryMemory(orderEntry3);
        }
    }


    @Test
    public void testRemoveLastEntry() throws Exception {
        OrderEntry orderEntry1 = TestOrderEntryFactory.createOrderEntry("10:00");
        OrderEntry orderEntry2 = TestOrderEntryFactory.createOrderEntry("11:00");
        OrderEntry orderEntry3 = TestOrderEntryFactory.createOrderEntry("12:00");

        try {
            orderList.add(orderEntry1);
            orderList.add(orderEntry2);
            orderList.add(orderEntry3);

            orderList.get(0, flyWeight);
            assertEquals(orderEntry1, flyWeight, "Initial Order 0");
            orderList.get(1, flyWeight);
            assertEquals(orderEntry2, flyWeight, "Initial Order 1");
            orderList.get(2, flyWeight);
            assertEquals(orderEntry3, flyWeight, "Initial Order 2");

            orderList.remove(2);

            orderList.get(0, flyWeight);
            assertEquals(orderEntry1, flyWeight, "Expected Order 0");

            orderList.get(1, flyWeight);
            assertEquals(orderEntry2, flyWeight, "Expected Order 1");

            orderList.get(2, flyWeight);
            assertEquals(0, flyWeight.getOrderId(), "Expected Order 2");

            assertEquals(2, orderList.size(), "Size");

        }finally {
            UnsafeUtil.freeOrderEntryMemory(orderEntry1);
            UnsafeUtil.freeOrderEntryMemory(orderEntry2);
            UnsafeUtil.freeOrderEntryMemory(orderEntry3);
        }
    }

    @Test
    public void testAddAtCapacity() throws Exception {
        int seconds = 0;
        List<OrderEntry> orderEntryList = new ArrayList<>();
        for(int i=0; i<orderList.capacity(); i++){
            OrderEntry orderEntry = TestOrderEntryFactory.createOrderEntry("10:" + seconds++);
            orderEntryList.add(orderEntry);
            orderList.add(orderEntry);
        }

        assertEquals(orderList.size(),orderList.capacity(), "Size == Capacity");
        int oldCapacity = orderList.capacity();

        try {
            OrderEntry newOrder = TestOrderEntryFactory.createOrderEntry("11:00");
            orderList.add(newOrder);

            orderList.get(oldCapacity, flyWeight);
            assertEquals(newOrder, flyWeight);
        }finally{
            for(OrderEntry oe: orderEntryList){
                UnsafeUtil.freeOrderEntryMemory(oe);
            }
        }
    }

    @Test
    public void testAddThousandOrders() throws Exception {
        int size = 1000;
        List<OrderEntry> orderEntryList = new ArrayList<>();
        for(int i=0; i<size; i++){
            OrderEntry orderEntry = TestOrderEntryFactory.createOrderEntry("10:00");
            orderEntry.setSubmittedTime(System.nanoTime());
            orderEntryList.add(orderEntry);
            orderList.add(orderEntry);
        }

        try {
            assertEquals(size, orderList.size());
        }finally{
            for(OrderEntry oe: orderEntryList){
                UnsafeUtil.freeOrderEntryMemory(oe);
            }
        }
    }

    @Test
    public void testAddMillionOrdersRemoveFirstHundred() throws Exception {
        int size = 1000000;
        List<OrderEntry> orderEntryList = new ArrayList<>();
        for(int i=0; i<size; i++){
            OrderEntry orderEntry = TestOrderEntryFactory.createOrderEntry("10:00");
            orderEntry.setSubmittedTime(System.nanoTime());
            orderEntryList.add(orderEntry);
            orderList.add(orderEntry);
        }

        try {
            assertEquals(size, orderList.size());
            for(int i=0; i<100; i++){
                orderList.remove(i);
            }
        }finally{
            for(OrderEntry oe: orderEntryList){
                UnsafeUtil.freeOrderEntryMemory(oe);
            }
        }
    }

    @Test
    public void testRemoveQuantity() throws Exception {
        OrderEntry orderEntry1 = TestOrderEntryFactory.createOrderEntry("08:00");
        OrderEntry orderEntry2 = TestOrderEntryFactory.createOrderEntry("09:00");
        OrderEntry orderEntry3 = TestOrderEntryFactory.createOrderEntry("10:00");
        OrderEntry orderEntry4 = TestOrderEntryFactory.createOrderEntry("12:00");
        OrderEntry orderEntry5 = TestOrderEntryFactory.createOrderEntry("13:00");
        OrderEntry orderEntry6 = TestOrderEntryFactory.createOrderEntry("14:00");
        OrderEntry orderEntry7 = TestOrderEntryFactory.createOrderEntry("16:00");
        OrderEntry orderEntry8 = TestOrderEntryFactory.createOrderEntry("17:00");

        OrderEntry oe = new OrderEntry();

        try {
            orderList.add(orderEntry1);
            orderList.add(orderEntry2);
            orderList.add(orderEntry3);

            orderList.get(1, oe);
            oe.removeQuantity(1000);
            orderList.remove(1);

            orderList.add(orderEntry4);
            orderList.add(orderEntry5);
            orderList.add(orderEntry6);

            orderList.get(2, oe);
            oe.removeQuantity(1000);
            orderList.remove(2);

            orderList.add(orderEntry7);
            orderList.add(orderEntry8);

            assertEquals(6000, orderList.total());
        }finally {
            UnsafeUtil.freeOrderEntryMemory(orderEntry1);
            UnsafeUtil.freeOrderEntryMemory(orderEntry2);
            UnsafeUtil.freeOrderEntryMemory(orderEntry3);
            UnsafeUtil.freeOrderEntryMemory(orderEntry4);
            UnsafeUtil.freeOrderEntryMemory(orderEntry5);
            UnsafeUtil.freeOrderEntryMemory(orderEntry6);
            UnsafeUtil.freeOrderEntryMemory(orderEntry7);
            UnsafeUtil.freeOrderEntryMemory(orderEntry8);
        }
    }

    @Test
    public void testRemoveTotal() throws Exception {
        OrderEntry orderEntry1 = TestOrderEntryFactory.createOrderEntry("08:00");
        orderEntry1.setOrderId(1);
        orderEntry1.setQuantity(1000);
        OrderEntry orderEntry2 = TestOrderEntryFactory.createOrderEntry("09:00");
        orderEntry2.setOrderId(2);
        orderEntry2.setQuantity(2000);
        OrderEntry orderEntry3 = TestOrderEntryFactory.createOrderEntry("10:00");
        orderEntry3.setOrderId(3);
        orderEntry3.setQuantity(3000);
        OrderEntry orderEntry4 = TestOrderEntryFactory.createOrderEntry("12:00");
        orderEntry4.setOrderId(4);
        orderEntry4.setQuantity(4000);
        OrderEntry orderEntry5 = TestOrderEntryFactory.createOrderEntry("13:00");
        orderEntry5.setOrderId(5);
        orderEntry5.setQuantity(5000);
        OrderEntry orderEntry6 = TestOrderEntryFactory.createOrderEntry("14:00");
        orderEntry6.setOrderId(6);
        orderEntry6.setQuantity(6000);
        OrderEntry orderEntry7 = TestOrderEntryFactory.createOrderEntry("16:00");
        orderEntry7.setOrderId(7);
        orderEntry7.setQuantity(7000);
        OrderEntry orderEntry8 = TestOrderEntryFactory.createOrderEntry("17:00");
        orderEntry8.setOrderId(8);
        orderEntry8.setQuantity(8000);

        try {
            orderList.add(orderEntry1);
            orderList.add(orderEntry2);
            orderList.add(orderEntry3);
            orderList.add(orderEntry4);
            orderList.add(orderEntry5);
            orderList.add(orderEntry6);
            orderList.add(orderEntry7);
            orderList.add(orderEntry8);

            for (Iterator<OrderListCursor> iterator = orderList.iterator();iterator.hasNext();) {
                OrderEntry oe = iterator.next().value;
                if(oe.getOrderId() == 3 || oe.getOrderId() == 5) {
                    iterator.remove();
                }
            }

            assertEquals(28000, orderList.total());
        }finally {
            UnsafeUtil.freeOrderEntryMemory(orderEntry1);
            UnsafeUtil.freeOrderEntryMemory(orderEntry2);
            UnsafeUtil.freeOrderEntryMemory(orderEntry3);
            UnsafeUtil.freeOrderEntryMemory(orderEntry4);
            UnsafeUtil.freeOrderEntryMemory(orderEntry5);
            UnsafeUtil.freeOrderEntryMemory(orderEntry6);
            UnsafeUtil.freeOrderEntryMemory(orderEntry7);
            UnsafeUtil.freeOrderEntryMemory(orderEntry8);
        }
    }

    @Test
    public void testOrderList2sEqual() throws Exception {
        OrderEntry orderEntry1 = TestOrderEntryFactory.createOrderEntry("10:00");
        OrderEntry orderEntry2 = TestOrderEntryFactory.createOrderEntry("11:00");
        OrderEntry orderEntry3 = TestOrderEntryFactory.createOrderEntry("12:00");

        OrderListImpl orderList2 = new OrderListImpl();

        OrderEntry orderEntry4 = TestOrderEntryFactory.createOrderEntry("10:00");
        OrderEntry orderEntry5 = TestOrderEntryFactory.createOrderEntry("11:00");
        OrderEntry orderEntry6 = TestOrderEntryFactory.createOrderEntry("12:00");

        try {

            orderList.add(orderEntry1);
            orderList.add(orderEntry2);
            orderList.add(orderEntry3);

            orderList2.add(orderEntry4);
            orderList2.add(orderEntry5);
            orderList2.add(orderEntry6);

            assertEquals(orderList, orderList2);
        }finally {
            UnsafeUtil.freeOrderEntryMemory(orderEntry1);
            UnsafeUtil.freeOrderEntryMemory(orderEntry2);
            UnsafeUtil.freeOrderEntryMemory(orderEntry3);
            UnsafeUtil.freeOrderEntryMemory(orderEntry4);
            UnsafeUtil.freeOrderEntryMemory(orderEntry5);
            UnsafeUtil.freeOrderEntryMemory(orderEntry6);
            orderList2.free();
        }
    }

    @Test
    public void testTrimToSize() throws Exception {
        OrderEntry orderEntry1 = TestOrderEntryFactory.createOrderEntry("10:00");
        OrderEntry orderEntry2 = TestOrderEntryFactory.createOrderEntry("11:00");
        OrderEntry orderEntry3 = TestOrderEntryFactory.createOrderEntry("12:00");

        try {

            orderList.add(orderEntry1);
            orderList.add(orderEntry2);
            orderList.add(orderEntry3);

            orderList.trimToSize();

            orderList.get(0, flyWeight);
            assertEquals(orderEntry1, flyWeight);

            orderList.get(1, flyWeight);
            assertEquals(orderEntry2, flyWeight);

            orderList.get(2, flyWeight);
            assertEquals(orderEntry3, flyWeight);

            assertEquals(orderList.size(),orderList.capacity());
        }finally {
            UnsafeUtil.freeOrderEntryMemory(orderEntry1);
            UnsafeUtil.freeOrderEntryMemory(orderEntry2);
            UnsafeUtil.freeOrderEntryMemory(orderEntry3);
        }
    }

    @Test
    public void testHiddenOrders() throws Exception {
        OrderEntry orderEntry1 = TestOrderEntryFactory.createOrderEntry("08:00");
        OrderEntry orderEntry2 = TestOrderEntryFactory.createHOOrderEntry("09:00");
        OrderEntry orderEntry3 = TestOrderEntryFactory.createOrderEntry("10:00");
        OrderEntry orderEntry4 = TestOrderEntryFactory.createHOOrderEntry("12:00");
        OrderEntry orderEntry5 = TestOrderEntryFactory.createOrderEntry("13:00");
        OrderEntry orderEntry6 = TestOrderEntryFactory.createHOOrderEntry("14:00");

        try {
            orderList.add(orderEntry1);
            orderList.add(orderEntry2);
            orderList.add(orderEntry3);
            orderList.add(orderEntry4);
            orderList.add(orderEntry5);
            orderList.add(orderEntry6);

            orderList.get(0, flyWeight);
            assertEquals(orderEntry1, flyWeight, "Order 1");

            orderList.get(1, flyWeight);
            assertEquals(orderEntry3, flyWeight, "Order 2");

            orderList.get(2, flyWeight);
            assertEquals(orderEntry5, flyWeight, "Order 3");

            orderList.get(3, flyWeight);
            assertEquals(orderEntry2, flyWeight, "Order 4");

            orderList.get(4, flyWeight);
            assertEquals(orderEntry4, flyWeight, "Order 5");

            orderList.get(5, flyWeight);
            assertEquals(orderEntry6, flyWeight, "Order 6");

        }finally {
            UnsafeUtil.freeOrderEntryMemory(orderEntry1);
            UnsafeUtil.freeOrderEntryMemory(orderEntry2);
            UnsafeUtil.freeOrderEntryMemory(orderEntry3);
            UnsafeUtil.freeOrderEntryMemory(orderEntry4);
            UnsafeUtil.freeOrderEntryMemory(orderEntry5);
            UnsafeUtil.freeOrderEntryMemory(orderEntry6);
        }
    }

    @Test
    public void testDisplayQuantityEqualsQuantity() throws Exception {
        OrderEntry orderEntry1 = TestOrderEntryFactory.createOrderEntry("08:00");
        orderEntry1.setQuantity(2000);

        try {
            orderList.add(orderEntry1);

            orderList.get(0, flyWeight);
            assertEquals(2000,flyWeight.getDisplayQuantity());


        }finally {
            UnsafeUtil.freeOrderEntryMemory(orderEntry1);
        }
    }

    @Test
    public void testHODisplayQuantityEqualsZero() throws Exception {
        OrderEntry orderEntry1 = TestOrderEntryFactory.createHOOrderEntry("08:00");
        orderEntry1.setQuantity(2000);

        try {
            orderList.add(orderEntry1);

            orderList.get(0, flyWeight);
            assertEquals(0,flyWeight.getDisplayQuantity());


        }finally {
            UnsafeUtil.freeOrderEntryMemory(orderEntry1);
        }
    }
}