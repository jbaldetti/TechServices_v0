package com.tackleset.invest;

import com.fasterxml.jackson.databind.ObjectMapper;
import junit.framework.TestCase;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;
import java.util.Map;

/**
 * IntegrationTest
 *
 * REQUIRES running Advisor REST web service prior
 *
 */
public class IntegrationTest extends TestCase {

    private ObjectMapper objectMapper= new ObjectMapper();
    private static String host = "http://localhost:8080";

    public static void main(String[] args) {
        if (args.length > 0  && args[0].startsWith("http")) {
            host = args[0];
        }
        IntegrationTest integrationTest = new IntegrationTest();
        integrationTest.testGetRiskLevelPortfolio();
        integrationTest.testGetAdjustmentsWithCancellingOut();
        integrationTest.testGetAdjustmentsWithLargerNumbers();
        integrationTest.testGetAdjustmentsWithLargerNumbersWithDecimals();
    }

    /**
     * testGetRiskLevelPortfolio
     * <p>
     * test low bound and high bound out of range risk levels to verify Not Found Exception
     * test each of the valid 10 risk levels conforms to size and risk level
     */
    public void testGetRiskLevelPortfolio() {
        for (int i = 0; i < 12; i++) {
            try {
                HttpUriRequest request = new HttpGet( String.format(host + "/invest/portfolios?riskLevel=%d",i));
                HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);
                int statusCode = httpResponse.getStatusLine().getStatusCode();
                if (i == 0 || i == 11) {
                    assertTrue(statusCode == 404);
                    continue;
                }
                String json = IOUtils.toString(httpResponse.getEntity().getContent());
                try {
                    Map<String, Object> riskMap = objectMapper.readValue(
                            json, Map.class);
                    assertTrue("Map is expected size", riskMap.size() == 6);
                    assertTrue("Risk level matches", riskMap.get("level") == Integer.valueOf(i));
                } catch (IOException e) {
                    assertFalse(e.getMessage(), false);
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * testGetAdjustmentsWithCancellingOut
     *
     * test cancelling out transactions that are neither max/min deltas
     */
    public void testGetAdjustmentsWithCancellingOut() {
        HttpUriRequest request = new HttpGet( host + "/invest/adjustments?riskLevel=7&bondAmt=8&largeCapAmt=33&midCapAmt=14&foreignAmt=36&smallCapAmt=9");

        try {
            HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);
            String json = IOUtils.toString(httpResponse.getEntity().getContent());
            try {
                String transactionAdjustments = objectMapper.readValue(
                        json, String.class);
                assertTrue("Transactions as expected",
                        transactionAdjustments.equalsIgnoreCase("[{2=11.0}, {3=-11.0}, {1=-8.00}, {0=8.00}, {0=4.00}, {4=-4.00}]\n"));

            } catch (IOException e) {
                assertFalse(e.getMessage(), false);
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * testGetAdjustmentsWithLargerNumbers
     *
     * test not cancelling out transactions using larger numbers
     */
    public void testGetAdjustmentsWithLargerNumbers() {
        HttpUriRequest request = new HttpGet( host + "/invest/adjustments?riskLevel=7&bondAmt=48&largeCapAmt=353&midCapAmt=144&foreignAmt=326&smallCapAmt=19");

        try {
            HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);
            String json = IOUtils.toString(httpResponse.getEntity().getContent());
            try {
                String transactionAdjustments = objectMapper.readValue(
                        json, String.class);
                assertTrue("Transactions as expected",
                        transactionAdjustments.equalsIgnoreCase("[{0=130.00}, {1=-130.00}, {2=78.50}, {3=-78.50}, {3=-25.00}, {4=25.00}, {4=0.50}, {1=-0.50}]\n"));

            } catch (IOException e) {
                assertFalse(e.getMessage(), false);
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * testGetAdjustmentsWithLargerNumbersWithDecimals
     * <p>
     * <p>
     * test not cancelling out transactions using larger numbers and decimals to verify rounding
     */
    public void testGetAdjustmentsWithLargerNumbersWithDecimals() {
        HttpUriRequest request = new HttpGet(host + "/invest/adjustments?riskLevel=7&bondAmt=48.56&largeCapAmt=35.73&midCapAmt=144.56&foreignAmt=326.11&smallCapAmt=19.33");

        try {
            HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);
            String json = IOUtils.toString(httpResponse.getEntity().getContent());
            try {
                String transactionAdjustments = objectMapper.readValue(
                        json, String.class);
                assertTrue("Transactions as expected",
                        transactionAdjustments.equalsIgnoreCase("[{1=107.84}, {3=-107.84}, {0=66.30}, {3=-66.30}, {3=-8.40}, {4=8.40}, {4=0.99}, {2=-0.99}]\n"));

            } catch (IOException e) {
                assertFalse(e.getMessage(), false);
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
