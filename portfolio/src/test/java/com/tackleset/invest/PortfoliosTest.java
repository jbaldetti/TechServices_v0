package com.tackleset.invest;

import com.fasterxml.jackson.databind.ObjectMapper;
import junit.framework.TestCase;

import javax.ws.rs.NotFoundException;
import java.io.IOException;
import java.util.Map;

/**
 * Unit test for Portfolios web service
 */
public class PortfoliosTest extends TestCase {

    private ObjectMapper objectMapper= new ObjectMapper();

    /**
     * testGetRiskLevelPortfolio
     *
     * test low bound and high bound out of range risk levels to verify Not Found Exception
     * test each of the valid 10 risk levels conforms to size and risk level
     */
    public void testGetRiskLevelPortfolio() {
        Portfolios portfolios = new Portfolios();
        for (int i = 0; i < 12; i++) {
            try {
                String json = portfolios.getRiskLevelPortfolio(i);
                try {
                    Map<String, Object> riskMap = objectMapper.readValue(
                            json, Map.class);
                    assertTrue("Map is expected size", riskMap.size() == 6);
                    assertTrue("Risk level matches", riskMap.get("level") == Integer.valueOf(i));
                } catch (IOException e) {
                    assertFalse(e.getMessage(), false);
                    e.printStackTrace();
                }
            } catch (NotFoundException nfe) {
                if (i == 0 || i == 11) {
                    assertTrue("Valid Not Found Exception", true);
                } else {
                    assertFalse("Invalid Not Found Exception", false);
                }
            }
        }
    }

    /**
     * testGetAdjustmentsWithCancellingOut
     *
     * test cancelling out transactions that are neither max/min deltas
     */
    public void testGetAdjustmentsWithCancellingOut() {
        Portfolios portfolios = new Portfolios();
        try {
            int level = 7;
            double bondAmt = 8;
            double largeCapAmt = 33;
            double midCapAmt = 14;
            double foreignAmt = 36;
            double smallCapAmt = 9;
            String json = portfolios.getAdjustments(level, bondAmt, largeCapAmt, midCapAmt, foreignAmt, smallCapAmt);
            try {
               String transactionAdjustments = objectMapper.readValue(
                        json, String.class);
                assertTrue("Transactions as expected",
                        transactionAdjustments.equalsIgnoreCase("[{2=11.0}, {3=-11.0}, {1=-8.00}, {0=8.00}, {0=4.00}, {4=-4.00}]\n"));

            } catch (IOException e) {
                assertFalse(e.getMessage(), false);
                e.printStackTrace();
            }
        } catch (NotFoundException nfe) {
            assertFalse("Invalid Not Found Exception", false);

        }
    }

    /**
     * testGetAdjustmentsWithLargerNumbers
     *
     * test not cancelling out transactions using larger numbers
     */
    public void testGetAdjustmentsWithLargerNumbers() {
        Portfolios portfolios = new Portfolios();
        try {
            int level = 7;
            double bondAmt = 48;
            double largeCapAmt = 353;
            double midCapAmt = 144;
            double foreignAmt = 326;
            double smallCapAmt = 19;
            String json = portfolios.getAdjustments(level, bondAmt, largeCapAmt, midCapAmt, foreignAmt, smallCapAmt);
            try {
                String transactionAdjustments = objectMapper.readValue(
                        json, String.class);
                assertTrue("Transactions as expected",
                        transactionAdjustments.equalsIgnoreCase("[{0=130.00}, {1=-130.00}, {2=78.50}, {3=-78.50}, {3=-25.00}, {4=25.00}, {4=0.50}, {1=-0.50}]\n"));

            } catch (IOException e) {
                assertFalse(e.getMessage(), false);
                e.printStackTrace();
            }
        } catch (NotFoundException nfe) {
            assertFalse("Invalid Not Found Exception", false);

        }
    }

    /**
     * testGetAdjustmentsWithLargerNumbersWithDecimals
     *
     *
     * test not cancelling out transactions using larger numbers and decimals to verify rounding
     */
    public void testGetAdjustmentsWithLargerNumbersWithDecimals() {
        Portfolios portfolios = new Portfolios();
        try {
            int level = 7;
            double bondAmt = 48.56;
            double largeCapAmt = 35.73;
            double midCapAmt = 144.56;
            double foreignAmt = 326.11;
            double smallCapAmt = 19.33;
            String json = portfolios.getAdjustments(level, bondAmt, largeCapAmt, midCapAmt, foreignAmt, smallCapAmt);
            try {
                String transactionAdjustments = objectMapper.readValue(
                        json, String.class);
                assertTrue("Transactions as expected",
                        transactionAdjustments.equalsIgnoreCase("[{1=107.84}, {3=-107.84}, {0=66.30}, {3=-66.30}, {3=-8.40}, {4=8.40}, {4=0.99}, {2=-0.99}]\n"));

            } catch (IOException e) {
                assertFalse(e.getMessage(), false);
                e.printStackTrace();
            }
        } catch (NotFoundException nfe) {
            assertFalse("Invalid Not Found Exception", false);

        }
    }
}