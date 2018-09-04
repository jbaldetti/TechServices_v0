package com.tackleset.invest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import junit.framework.TestCase;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * IntegrationTest Sample
 *
 * REQUIRES running Advisor REST web service prior
 *
 */
public class IntegrationTest extends TestCase {

    private ObjectMapper objectMapper= new ObjectMapper();
    private static String host = "http://localhost:8080";
    private static Runnable command = null;

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

    @Override
    protected void setUp() {
        startServer();
    }

    private synchronized static void startServer() {
        if (command == null) {
            command = () -> {
                try {
                    Advisor.main(new String[]{});
                } catch (Exception e) {
                    e.printStackTrace();
                    assertFalse("Server failed to startup!\n" + e.getMessage(), true);
                }
            };
            Executors.newSingleThreadExecutor().execute(command);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                // NoOp
            }
        }
    }

    /**
     * testGetRiskLevelPortfolio
     * <p>
     * test low bound and high bound out of range risk levels to verify Not Found Exception
     * test each of the valid 10 risk levels conforms to size and risk level
     */
    public void testGetRiskLevelPortfolio() {
        for (int i = 0; i < 12; i++) {
            String json = "";
            try {
                HttpUriRequest request = new HttpGet(String.format(host + "/invest/portfolios?riskLevel=%d", i));
                HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);
                int statusCode = httpResponse.getStatusLine().getStatusCode();
                if (i == 0 || i == 11) {
                    assertTrue(statusCode == 404);
                    continue;
                }
                json = IOUtils.toString(httpResponse.getEntity().getContent());
            } catch (IOException e) {
                e.printStackTrace();
                assertFalse("Is Server Up?\n" + e.getMessage(), true);
            }
            try {
                Map<String, Object> riskMap = objectMapper.readValue(
                        json, Map.class);
                assertTrue("Map is expected size", riskMap.size() == 6);
                assertTrue("Risk level matches", riskMap.get("level") == Integer.valueOf(i));
            } catch (IOException e) {
                e.printStackTrace();
                assertFalse(e.getMessage(), true);
            }
        }
    }

    /**
     * testGetAdjustmentsWithCancellingOut
     * <p>
     * test cancelling out transactions that are neither max/min deltas
     */
    public void testGetAdjustmentsWithCancellingOut() {
        HttpUriRequest request = new HttpGet(host + "/invest/adjustments?riskLevel=7&bondAmt=8&largeCapAmt=33&midCapAmt=14&foreignAmt=36&smallCapAmt=9");
        try {
            HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);
            List<Map<String, Double>> expected = Arrays.asList(ImmutableMap.of("2", 11.0),
                    ImmutableMap.of("3", -11.0), ImmutableMap.of("1", -8.0),
                    ImmutableMap.of("0", 8.0), ImmutableMap.of("0", 4.0),
                    ImmutableMap.of("4", -4.0));
            String json = IOUtils.toString(httpResponse.getEntity().getContent());
            try {
                List<Object> transactionAdjustments = objectMapper.readValue(
                        json, List.class);
                assertTrue("Transactions as expected",
                        transactionAdjustments.equals(expected));

            } catch (IOException e) {
                e.printStackTrace();
                assertFalse(e.getMessage(), true);
            }
        } catch (IOException e) {
            e.printStackTrace();
            assertFalse("Is Server Up?\n" + e.getMessage(), true);
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
            List<Map<String, Double>> expected = Arrays.asList(ImmutableMap.of("0", 130.0),
                    ImmutableMap.of("1", -130.0), ImmutableMap.of("2", 78.50),
                    ImmutableMap.of("3", -78.50), ImmutableMap.of("3", -25.0),
                    ImmutableMap.of("4", 25.0), ImmutableMap.of("4", 0.50),
                    ImmutableMap.of("1", -.50));
            String json = IOUtils.toString(httpResponse.getEntity().getContent());
            try {
                List<Object> transactionAdjustments = objectMapper.readValue(
                        json, List.class);
                assertTrue("Transactions as expected",
                        transactionAdjustments.equals(expected));

            } catch (IOException e) {
                e.printStackTrace();
                assertFalse(e.getMessage(), false);
            }
        } catch (IOException e) {
            e.printStackTrace();
            assertFalse("Is Server Up?\n" + e.getMessage(), true);
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
            List<Map<String, Double>> expected = Arrays.asList(ImmutableMap.of("1", 107.84),
                    ImmutableMap.of("3", -107.84), ImmutableMap.of("0", 66.30),
                    ImmutableMap.of("3", -66.30), ImmutableMap.of("3", -8.40),
                    ImmutableMap.of("4", 8.40), ImmutableMap.of("4", 0.99),
                    ImmutableMap.of("2", -.99));
            String json = IOUtils.toString(httpResponse.getEntity().getContent());
            try {
                List<Object> transactionAdjustments = objectMapper.readValue(
                        json, List.class);
                assertTrue("Transactions as expected",
                        transactionAdjustments.equals(expected));

            } catch (IOException e) {
                e.printStackTrace();
                assertFalse(e.getMessage(), false);
            }
        } catch (IOException e) {
            e.printStackTrace();
            assertFalse("Is Server Up?\n" + e.getMessage(), true);
        }
    }

}
