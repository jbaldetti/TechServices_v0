package com.tackleset.invest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import junit.framework.TestCase;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Unit test for Portfolios web service
 */
public class PortfoliosTest extends TestCase {

    private ObjectMapper objectMapper= new ObjectMapper();
    private Portfolios portfolios = new Portfolios();

    /**
     * testGetRiskLevelPortfolio
     *
     * test low bound and high bound out of range risk levels to verify Not Found Exception
     * test each of the valid 10 risk levels conforms to size and risk level
     */

    public void testGetRiskLevelPortfolio() throws IOException {
        for (int i = 0; i < 12; i++) {
            try {
                String json = portfolios.getRiskLevelPortfolio(i);
                try {
                    Map<String, Object> riskMap = objectMapper.readValue(
                            json, Map.class);
                    assertTrue("Map is expected size", riskMap.size() == 6);
                    assertTrue("Risk level matches", riskMap.get("level") == Integer.valueOf(i));
                } catch (IOException e) {
                    e.printStackTrace();
                    assertFalse(e.getMessage(), true);
                }
            } catch (NotFoundException nfe) {
                if (i == 0 || i == 11) {
                    assertTrue("Valid Not Found Exception", true);
                } else {
                    assertFalse("Invalid Not Found Exception", true);
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
        try {
            int level = 7;
            String bondAmt = "8";
            String largeCapAmt = "33";
            String midCapAmt = "14";
            String foreignAmt = "36";
            String smallCapAmt = "9";
            List<Map<String, Double>> expected = Arrays.asList(ImmutableMap.of("2", 11.0),
                    ImmutableMap.of("3", -11.0), ImmutableMap.of("1", -8.0),
                    ImmutableMap.of("0", 8.0), ImmutableMap.of("0", 4.0),
                    ImmutableMap.of("4", -4.0));
            String json = portfolios.getAdjustments(level, bondAmt, largeCapAmt, midCapAmt, foreignAmt, smallCapAmt);
            try {
               List<Object> transactionAdjustments = objectMapper.readValue(
                        json, List.class);
                assertTrue("Transactions as expected",
                        transactionAdjustments.equals(expected));

            } catch (IOException e) {
                e.printStackTrace();
                assertFalse(e.getMessage(), true);
            }
        } catch (NotFoundException nfe) {
            assertFalse("Invalid Not Found Exception", true);
        }
    }

    /**
     * testGetAdjustmentsWithLargerNumbers
     *
     * test not cancelling out transactions using larger numbers
     */
    public void testGetAdjustmentsWithLargerNumbers() {
        try {
            int level = 7;
            String bondAmt = "48";
            String largeCapAmt = "353";
            String midCapAmt = "144";
            String foreignAmt = "326";
            String smallCapAmt = "19";
            List<Map<String, Double>> expected = Arrays.asList(ImmutableMap.of("0", 130.0),
                    ImmutableMap.of("1", -130.0), ImmutableMap.of("2", 78.50),
                    ImmutableMap.of("3", -78.50), ImmutableMap.of("3", -25.0),
                    ImmutableMap.of("4", 25.0), ImmutableMap.of("4", 0.50),
                    ImmutableMap.of("1", -.50));
            String json = portfolios.getAdjustments(level, bondAmt, largeCapAmt, midCapAmt, foreignAmt, smallCapAmt);
            try {
                List<Object> transactionAdjustments = objectMapper.readValue(
                        json, List.class);
                assertTrue("Transactions as expected",
                        transactionAdjustments.equals(expected));
            } catch (IOException e) {
                e.printStackTrace();
                assertFalse(e.getMessage(), true);
            }
        } catch (NotFoundException nfe) {
            assertFalse("Invalid Not Found Exception", true);
        }
    }

    /**
     * testGetAdjustmentsWithLargerNumbersWithDecimals
     *
     *
     * test not cancelling out transactions using larger numbers and decimals to verify rounding
     */
    public void testGetAdjustmentsWithLargerNumbersWithDecimals() {
        try {
            int level = 7;
            String bondAmt = "48.56";
            String largeCapAmt = "35.73";
            String midCapAmt = "144.56";
            String foreignAmt = "326.11";
            String smallCapAmt = "19.33";
            List<Map<String, Double>> expected = Arrays.asList(ImmutableMap.of("1", 107.84),
                    ImmutableMap.of("3", -107.84), ImmutableMap.of("0", 66.30),
                    ImmutableMap.of("3", -66.30), ImmutableMap.of("3", -8.40),
                    ImmutableMap.of("4", 8.40), ImmutableMap.of("4", 0.99),
                    ImmutableMap.of("2", -.99));
            String json = portfolios.getAdjustments(level, bondAmt, largeCapAmt, midCapAmt, foreignAmt, smallCapAmt);
            try {
                List<Object> transactionAdjustments = objectMapper.readValue(
                        json, List.class);
                assertTrue("Transactions as expected",
                        transactionAdjustments.equals(expected));

            } catch (IOException e) {
                e.printStackTrace();
                assertFalse(e.getMessage(), true);
            }
        } catch (NotFoundException nfe) {
            assertFalse("Invalid Not Found Exception", true);
        }
    }

    /**
     * testGetAdjustmentsWithLargerNumbersWithDecimalsNegatives
     * <p>
     * <p>
     * test not cancelling out transactions using larger numbers and decimals to verify rounding, having a negative amt
     */
    public void testGetAdjustmentsWithLargerNumbersWithDecimalsNegatives() {
        try {
            int level = 7;
            String bondAmt = "48.56";
            String largeCapAmt = "35.73";
            String midCapAmt = "144.56";
            String foreignAmt = "326.11";
            String smallCapAmt = "-1.45";
            List<Map<String, Double>> expected = Arrays.asList(ImmutableMap.of("1", 102.65),
                    ImmutableMap.of("3", -102.65), ImmutableMap.of("0", 62.14),
                    ImmutableMap.of("3", -62.14), ImmutableMap.of("3", -22.94),
                    ImmutableMap.of("4", 22.94), ImmutableMap.of("4", 6.18),
                    ImmutableMap.of("2", -6.18));
            String json = portfolios.getAdjustments(level, bondAmt, largeCapAmt, midCapAmt, foreignAmt, smallCapAmt);
            try {
                List<Object> transactionAdjustments = objectMapper.readValue(
                        json, List.class);
                assertTrue("Transactions as expected",
                        transactionAdjustments.equals(expected));

            } catch (IOException e) {
                e.printStackTrace();
                assertFalse(e.getMessage(), true);
            }
        } catch (NotFoundException nfe) {
            assertFalse("Invalid Not Found Exception", true);
        }
    }

    /**
     * testGetAdjustmentsAllZeroDollars
     *
     * all zero dollar amounts returns not found exception
     */
    public void testGetAdjustmentsAllZeroDollars() {
        try {
            int level = 1;
            String bondAmt = "0";
            String largeCapAmt = "0";
            String midCapAmt = "0";
            String foreignAmt = "0";
            String smallCapAmt = "0";
            String json = portfolios.getAdjustments(level, bondAmt, largeCapAmt, midCapAmt, foreignAmt, smallCapAmt);
        } catch (NotFoundException nfe) {
            assertTrue("Valid Not Found Exception", true);
        }
    }

    /**
     * testGetAdjustmentsTotalNegative
     *
     * total amount is negative dollar returns not found exception
     */
    public void testGetAdjustmentsTotalNegative() {
        try {
            int level = 3;
            String bondAmt = "0";
            String largeCapAmt = "0";
            String midCapAmt = "0";
            String foreignAmt = "5";
            String smallCapAmt = "-10";
            String json = portfolios.getAdjustments(level, bondAmt, largeCapAmt, midCapAmt, foreignAmt, smallCapAmt);
        } catch (NotFoundException nfe) {
            assertTrue("Valid Not Found Exception", true);
        }
    }

    /**
     * testGetAdjustmentsTotalLessThan1
     *
     * total amount is less than 1 and returns not found exception
     */
    public void testGetAdjustmentsTotalLessThan1() {
        try {
            int level = 6;
            String bondAmt = "0.004";
            String largeCapAmt = "0.005";
            String midCapAmt = "0";
            String foreignAmt = "0.5";
            String smallCapAmt = "-0.0007";
            String json = portfolios.getAdjustments(level, bondAmt, largeCapAmt, midCapAmt, foreignAmt, smallCapAmt);
        } catch (NotFoundException nfe) {
            assertTrue("Valid Not Found Exception", true);
        }
    }

    /**
     * testGetAdjustmentsBadNumber
     *
     * use an amount that cannot be converted
     */
    public void testGetAdjustmentsBadNumber() {
        try {
            int level = 6;
            String bondAmt = "20,000.004";
            String largeCapAmt = "0.005";
            String midCapAmt = "0";
            String foreignAmt = "0.5";
            String smallCapAmt = "-0.0007";
            String json = portfolios.getAdjustments(level, bondAmt, largeCapAmt, midCapAmt, foreignAmt, smallCapAmt);
        } catch (BadRequestException bre) {
            assertTrue("Valid Bad Request Exception", true);
        }
    }

    /**
     * testGetAdjustmentsTooBigNumber
     *
     * an amount is bigger than the max length
     */
    public void testGetAdjustmentsTooBigNumber() {
        try {
            int level = 6;
            String bondAmt = "208872378974389798264492.004";
            String largeCapAmt = "0.005";
            String midCapAmt = "0";
            String foreignAmt = "0.5";
            String smallCapAmt = "-0.0007";
            String json = portfolios.getAdjustments(level, bondAmt, largeCapAmt, midCapAmt, foreignAmt, smallCapAmt);
        } catch (BadRequestException bre) {
            assertTrue("Valid Bad Request Exception", true);
            assertTrue(bre.getMessage().equalsIgnoreCase("Dollar amount(s) exceeds length 20"));
        }
    }

    /**
     * testGetAdjustmentsNullNumber
     *
     * an amount that is null, empty, blank
     */
    public void testGetAdjustmentsNullNumber() {
        try {
            int level = 7;
            String bondAmt = "8";
            String largeCapAmt = "33";
            String midCapAmt = "";
            String foreignAmt = "  ";
            String smallCapAmt = null;
            List<Map<String, Double>> expected = Arrays.asList(ImmutableMap.of("2", 10.25),
                    ImmutableMap.of("1", -10.25), ImmutableMap.of("3", 10.25),
                    ImmutableMap.of("1", -10.25), ImmutableMap.of("4", 2.05),
                    ImmutableMap.of("1", -2.05), ImmutableMap.of("0", 0.20),
                    ImmutableMap.of("1", -0.20));
            String json = portfolios.getAdjustments(level, bondAmt, largeCapAmt, midCapAmt, foreignAmt, smallCapAmt);
            try {
                List<Object> transactionAdjustments = objectMapper.readValue(
                        json, List.class);
                assertTrue("Transactions as expected",
                        transactionAdjustments.equals(expected)); //equalsIgnoreCase("[{0=10.20}, {3=-10.20}, {2=8.75}, {1=-8.75}, {3=-3.05}, {4=3.05}, {4=1.50}, {1=-1.50}]"));

            } catch (IOException e) {
                e.printStackTrace();
                assertFalse(e.getMessage(), true);
            }
        } catch (NotFoundException nfe) {
            assertFalse("Invalid Not Found Exception", true);
        }
    }
}