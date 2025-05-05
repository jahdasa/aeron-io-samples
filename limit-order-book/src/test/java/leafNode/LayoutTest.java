package leafNode;

import bplusTree.BPlusTree;
import com.carrotsearch.hppc.LongObjectHashMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.layouters.CurrentLayouter;
import org.openjdk.jol.layouters.HotSpotLayouter;
import org.openjdk.jol.layouters.Layouter;
import org.openjdk.jol.datamodel.Model32;
import org.openjdk.jol.datamodel.Model64;

import java.util.HashMap;

/**
 * Created by dharmeshsing on 1/07/15.
 */
public class LayoutTest {

    @Test
    public void testOrderEntry(){
        System.out.println(ClassLayout.parseClass(OrderEntry.class).toPrintable());
    }

    @Test
    public void testOrderEntryDivisbleBy8(){
        int header = 12;
        long objectSize = OrderEntry.getObjectSize();
        System.out.println(header + objectSize);
        boolean result = (header + objectSize) % 8 == 0;
        Assertions.assertEquals(true, result);
    }

    @Test
    public void testOrderList(){
        System.out.println(ClassLayout.parseClass(OrderListImpl.class).toPrintable());
    }

    @Test
    public void testBPlusTree(){
        System.out.println(ClassLayout.parseClass(BPlusTree.class).toPrintable());
    }

    @Test
    public void testDiffLayouts(){
        Layouter l;

        l = new CurrentLayouter();
        System.out.println("***** " + l);
        System.out.println(ClassLayout.parseClass(HashMap.class, l).toPrintable());

        l = new HotSpotLayouter(new Model32(), 21);
        System.out.println("***** " + l);
        System.out.println(ClassLayout.parseClass(HashMap.class, l).toPrintable());

        l = new HotSpotLayouter(new Model64(false, false), 21);
        System.out.println("***** " + l);
        System.out.println(ClassLayout.parseClass(HashMap.class, l).toPrintable());

        l = new HotSpotLayouter(new Model64(true, true), 21);
        System.out.println("***** " + l);
        System.out.println(ClassLayout.parseClass(HashMap.class, l).toPrintable());
    }

    @Test
    public void testHashMap(){
        System.out.println(ClassLayout.parseClass(HashMap.class).toPrintable());
        System.out.println(ClassLayout.parseClass(LongObjectHashMap.class).toPrintable());
    }
}
