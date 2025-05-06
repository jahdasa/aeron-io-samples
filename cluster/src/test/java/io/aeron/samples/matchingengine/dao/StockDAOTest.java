package io.aeron.samples.matchingengine.dao;

import dao.StockDAO;
import orderBook.Stock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StockDAOTest {

    @BeforeEach
    public void setup(){
        StockDAO.loadStocks(Paths.get("src/test/resources/data").toAbsolutePath().getParent() + "/data");
    }

    @Test
    public void testGetStock() throws Exception {
        Stock stock = new Stock();
        stock.setStockCode(1);
        stock.setMRS(700);
        stock.setTickSize(1);

        Stock storedValue = StockDAO.getStock(1L);

        assertEquals(stock,storedValue);
    }
}
