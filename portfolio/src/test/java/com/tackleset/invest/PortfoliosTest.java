package com.tackleset.invest;

import com.fasterxml.jackson.databind.ObjectMapper;
import junit.framework.TestCase;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import java.io.IOException;
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
    public void testGetAdjustmentsWithCancellingOut() throws IOException {
        try {
            int level = 7;
            String bondAmt = "8";
            String largeCapAmt = "33";
            String midCapAmt = "14";
            String foreignAmt = "36";
            String smallCapAmt = "9";
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
    public void testGetAdjustmentsWithLargerNumbers() throws IOException {
        try {
            int level = 7;
            String bondAmt = "48";
            String largeCapAmt = "353";
            String midCapAmt = "144";
            String foreignAmt = "326";
            String smallCapAmt = "19";
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
    public void testGetAdjustmentsWithLargerNumbersWithDecimals() throws IOException {
        try {
            int level = 7;
            String bondAmt = "48.56";
            String largeCapAmt = "35.73";
            String midCapAmt = "144.56";
            String foreignAmt = "326.11";
            String smallCapAmt = "19.33";
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

    /**
     * testGetAdjustmentsWithLargerNumbersWithDecimalsNegatives
     * <p>
     * <p>
     * test not cancelling out transactions using larger numbers and decimals to verify rounding, having a negative amt
     */
    public void testGetAdjustmentsWithLargerNumbersWithDecimalsNegatives() throws IOException {
        try {
            int level = 7;
            String bondAmt = "48.56";
            String largeCapAmt = "35.73";
            String midCapAmt = "144.56";
            String foreignAmt = "326.11";
            String smallCapAmt = "-1.45";
            String json = portfolios.getAdjustments(level, bondAmt, largeCapAmt, midCapAmt, foreignAmt, smallCapAmt);
            try {
                String transactionAdjustments = objectMapper.readValue(
                        json, String.class);
                assertTrue("Transactions as expected",
                        transactionAdjustments.equalsIgnoreCase("[{1=102.65}, {3=-102.65}, {0=62.14}, {3=-62.14}, {3=-22.94}, {4=22.94}, {4=6.18}, {2=-6.18}]\n"));

            } catch (IOException e) {
                assertFalse(e.getMessage(), false);
                e.printStackTrace();
            }
        } catch (NotFoundException nfe) {
            assertFalse("Invalid Not Found Exception", false);
        }
    }

    /**
     * testGetAdjustmentsAllZeroDollars
     *
     * all zero dollar amounts returns not found exception
     */
    public void testGetAdjustmentsAllZeroDollars() throws IOException {
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
    public void testGetAdjustmentsTotalNegative() throws IOException {
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
    public void testGetAdjustmentsTotalLessThan1() throws IOException {
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
    public void testGetAdjustmentsBadNumber() throws IOException {
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
    public void testGetAdjustmentsTooBigNumber() throws IOException {
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
     * an amount that is null
     */
    public void testGetAdjustmentsNullNumber() throws IOException {
        try {
            int level = 7;
            String bondAmt = "8";
            String largeCapAmt = "33";
            String midCapAmt = "14";
            String foreignAmt = "36";
            String smallCapAmt = null;
            String json = portfolios.getAdjustments(level, bondAmt, largeCapAmt, midCapAmt, foreignAmt, smallCapAmt);
            try {
                String transactionAdjustments = objectMapper.readValue(
                        json, String.class);
                assertTrue("Transactions as expected",
                        transactionAdjustments.equalsIgnoreCase("[{0=10.20}, {3=-10.20}, {2=8.75}, {1=-8.75}, {3=-3.05}, {4=3.05}, {4=1.50}, {1=-1.50}]\n"));

            } catch (IOException e) {
                assertFalse(e.getMessage(), false);
                e.printStackTrace();
            }
        } catch (NotFoundException nfe) {
            assertFalse("Invalid Not Found Exception", false);
        }
    }
}