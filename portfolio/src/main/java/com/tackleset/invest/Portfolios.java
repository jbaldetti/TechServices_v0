package com.tackleset.invest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Portfolios REST services
 *
 */
@Path("/invest")
public class Portfolios {

    public static final int MAX_LENGTH = 20;
    public static final String DEFAULT_ZERO_STR = "0";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<Object> portfolios;


    public Portfolios() {
        try {
            Map<String, Object> riskMap = objectMapper.readValue(
                    Thread.currentThread().getContextClassLoader().getResourceAsStream("portfolios.json"), Map.class);
            portfolios = (List<Object>) riskMap.get("risk_levels");
        } catch (IOException e) {
            e.printStackTrace();
            throw new NotFoundException();
        }
    }

    /**
     * getRiskLevelPortfolio
     *
     * Gets a recommended portfolio based on the risk level
     *
     * @param level risk level where 1 is low risk and numbers above are higher risk
     * @return one portfolio map entry containing investment assets in percentage terms (adding to 100) in json format
     *
     * Throws NotFoundException if risk is invalid or otherwise not found
     */
    @GET
    @Path("portfolios")
    @Produces(MediaType.APPLICATION_JSON)
    public String getRiskLevelPortfolio(@QueryParam("riskLevel") int level) throws IOException {
        if (level < 1 || level > portfolios.size()) {
            throw new NotFoundException();
        }
        return objectMapper.writeValueAsString(portfolios.get(level - 1)) + "\n";
    }

    /**
     * getAdjustments
     *
     * Given dollar amounts for provided investment asses and risk level, provides a minimal set of transactions to
     * reach portfolio percentage levels for
     *
     * Assumes if the total amount is less than a dollar then the transactional cost is not worth dividing amounts and
     * returns NotFoundException
     *
     * @param level           risk level where 1 is low risk and numbers above are higher risk
     * @param bondAmtStr      bond dollar amount
     * @param largeCapAmtStr  large cap dollar amount
     * @param midCapAmtStr    mid cap dollar amount
     * @param foreignAmtStr   foreign dollar amount
     * @param smallCapAmtStr  small cap dollar amount
     * @return  List of transactions to balance portfolio to risk level
     */
    @GET
    @Path("adjustments")
    @Produces(MediaType.APPLICATION_JSON)
    public String getAdjustments(@QueryParam("riskLevel") int level,
                                 @QueryParam("bondAmt") String bondAmtStr,
                                 @QueryParam("largeCapAmt") String largeCapAmtStr,
                                 @QueryParam("midCapAmt") String midCapAmtStr,
                                 @QueryParam("foreignAmt") String foreignAmtStr,
                                 @QueryParam("smallCapAmt") String smallCapAmtStr) {
        if (level < 1 || level > portfolios.size()) {
            throw new NotFoundException();
        }
        bondAmtStr = StringUtils.defaultIfBlank(bondAmtStr, DEFAULT_ZERO_STR);
        largeCapAmtStr = StringUtils.defaultIfBlank(largeCapAmtStr, DEFAULT_ZERO_STR);
        midCapAmtStr = StringUtils.defaultIfBlank(midCapAmtStr, DEFAULT_ZERO_STR);
        foreignAmtStr = StringUtils.defaultIfBlank(foreignAmtStr, DEFAULT_ZERO_STR);
        smallCapAmtStr = StringUtils.defaultIfBlank(smallCapAmtStr, DEFAULT_ZERO_STR);

        Map<String, Integer> portfolio = (Map<String, Integer>) portfolios.get(level - 1);
        try {
            if (bondAmtStr.length() > MAX_LENGTH || largeCapAmtStr.length() > MAX_LENGTH || midCapAmtStr.length() > MAX_LENGTH ||
                    foreignAmtStr.length() > MAX_LENGTH || smallCapAmtStr.length() > MAX_LENGTH) {
                throw new BadRequestException(String.format("Dollar amount(s) exceeds length %d", MAX_LENGTH));
            }
            double bondAmt = Double.valueOf(bondAmtStr);
            double largeCapAmt = Double.valueOf(largeCapAmtStr);
            double midCapAmt = Double.valueOf(midCapAmtStr);
            double foreignAmt = Double.valueOf(foreignAmtStr);
            double smallCapAmt = Double.valueOf(smallCapAmtStr);
            BigDecimal bdBondAmt = BigDecimal.valueOf(bondAmt);
            BigDecimal bdLargeCapAmt = BigDecimal.valueOf(largeCapAmt);
            BigDecimal bdMidCapAmt = BigDecimal.valueOf(midCapAmt);
            BigDecimal bdForeignAmt = BigDecimal.valueOf(foreignAmt);
            BigDecimal bdSmallCapAmt = BigDecimal.valueOf(smallCapAmt);
            BigDecimal totalAmount = BigDecimal.ZERO.add(bdBondAmt)
                    .add(bdLargeCapAmt)
                    .add(bdMidCapAmt)
                    .add(bdForeignAmt)
                    .add(bdSmallCapAmt).setScale(2, RoundingMode.HALF_UP);
            if (totalAmount.doubleValue() >= 1) {
                Map<Integer, BigDecimal> deltas = new LinkedHashMap<>();
                BigDecimal bondAdj = BigDecimal.valueOf(portfolio.get("bonds_pct")).multiply(totalAmount)
                        .divide(BigDecimal.valueOf(100)).subtract(bdBondAmt).setScale(2, RoundingMode.HALF_UP);
                deltas.put(0, bondAdj);
                BigDecimal bigCapAdj = BigDecimal.valueOf(portfolio.get("large_cap_pct")).multiply(totalAmount)
                        .divide(BigDecimal.valueOf(100)).subtract(bdLargeCapAmt).setScale(2, RoundingMode.HALF_UP);
                deltas.put(1, bigCapAdj);
                BigDecimal midCapAdj = BigDecimal.valueOf(portfolio.get("mid_cap_pct")).multiply(totalAmount)
                        .divide(BigDecimal.valueOf(100)).subtract(bdMidCapAmt).setScale(2, RoundingMode.HALF_UP);
                deltas.put(2, midCapAdj);
                BigDecimal foreignCapAdj = BigDecimal.valueOf(portfolio.get("foreign_pct")).multiply(totalAmount)
                        .divide(BigDecimal.valueOf(100)).subtract(bdForeignAmt).setScale(2, RoundingMode.HALF_UP);
                deltas.put(3, foreignCapAdj);
                BigDecimal smallCapAdj = totalAmount.subtract(bdBondAmt).subtract(bondAdj)
                        .subtract(bdLargeCapAmt).subtract(bigCapAdj).subtract(bdMidCapAmt)
                        .subtract(midCapAdj).subtract(bdForeignAmt).subtract(foreignCapAdj)
                        .subtract(bdSmallCapAmt).setScale(2, RoundingMode.HALF_UP);
                deltas.put(4, smallCapAdj);

                List<Map<Integer, BigDecimal>> transactions = getCancellingOutTransactions(deltas);
                // remove the cancelling out transactions from the delta list
                for (Map<Integer, BigDecimal> mapI : transactions) {
                    for (Integer Int : mapI.keySet()) {
                        deltas.remove(Int.intValue());
                    }
                }
                offsetMaxMinPortfolioDeltas(deltas, transactions);
                return objectMapper.writeValueAsString(transactions + "\n");
            } else {
                throw new NotFoundException();
            }
        } catch (NumberFormatException nfe) {
            throw new BadRequestException();
        } catch (WebApplicationException wae) {
            throw wae;
        } catch (Throwable t) {
            throw new BadRequestException();
        }
    }

    /**
     * offsetMaxMinPortfolioDeltas
     *
     *
     * @param deltas map containing asset account key integer and the value containing the delta amount
     * @param transactions bucket to add new transactions
     */
    private void offsetMaxMinPortfolioDeltas(Map<Integer, BigDecimal> deltas, List<Map<Integer, BigDecimal>> transactions)
            throws IllegalArgumentException {

        double sanitySum = deltas.values().stream().mapToLong(bd -> bd.multiply(BigDecimal.valueOf(100)).longValue()).sum();
        if (sanitySum != 0) {
            throw new IllegalArgumentException("Delta list provided will not zero out");
        }
        double sum = deltas.values().stream().mapToLong(bd -> Math.abs(bd.multiply(BigDecimal.valueOf(100)).longValue())).sum();
        while (sum != 0) {
            Map.Entry<Integer, BigDecimal> bigDecimalMax =
                    deltas.entrySet().stream().max(Comparator.comparing(Map.Entry::getValue)).get();
            Map.Entry<Integer, BigDecimal> bigDecimalMin =
                    deltas.entrySet().stream().min(Comparator.comparing(Map.Entry::getValue)).get();
            boolean posBigger =
                    (Math.abs(bigDecimalMax.getValue().floatValue()) > Math.abs(bigDecimalMin.getValue().floatValue())) ?
                            true : false;

            Map.Entry<Integer, BigDecimal> source = (posBigger) ? bigDecimalMax : bigDecimalMin;
            Map.Entry<Integer, BigDecimal> target = (posBigger) ? bigDecimalMin : bigDecimalMax;

            BigDecimal adjustment = source.getValue().add(target.getValue());
            Map<Integer, BigDecimal> map1 = new LinkedHashMap<>();
            map1.put(target.getKey(), target.getValue());
            transactions.add(map1);
            Map<Integer, BigDecimal> map2 = new LinkedHashMap<>();
            map2.put(source.getKey(), target.getValue().multiply(BigDecimal.valueOf(-1)));
            transactions.add(map2);
            deltas.remove(target.getKey());
            deltas.put(source.getKey(), adjustment);

            sum = deltas.values().stream().mapToDouble(bd -> Math.abs(bd.floatValue())).sum();
        }
    }

    /**
     * returns a list of transactions that can be used to cancel a pair of offsetting amounts
     *
     * @param deltas map containing asset account key integer and the value containing the delta amount
     * @return offsetting transactions, if none, will be empty
     */
    private List<Map<Integer, BigDecimal>> getCancellingOutTransactions(Map<Integer, BigDecimal> deltas) {
        List<Map<Integer, BigDecimal>> transactions = new ArrayList<>();
        for (int i = 0; i < deltas.size(); i++) {
            BigDecimal bigDecimal = deltas.get(i);
            if (bigDecimal.equals(BigDecimal.ZERO)) {
                continue;
            }
            for (int j = i+1; j < deltas.size(); j++) {
                BigDecimal bigDecimal2 = deltas.get(j);
                if (bigDecimal2.equals(BigDecimal.ZERO)) {
                    continue;
                }
                if (bigDecimal.add(bigDecimal2).floatValue() == 0) {
                    Map<Integer, BigDecimal> map1 = new LinkedHashMap<>();
                    map1.put(Integer.valueOf(i), BigDecimal.valueOf(bigDecimal.floatValue()));
                    transactions.add(map1);
                    Map<Integer, BigDecimal> map2 = new LinkedHashMap<>();
                    map2.put(Integer.valueOf(j), BigDecimal.valueOf(bigDecimal2.floatValue()));
                    transactions.add(map2);
                }
            }
        }
        return transactions;
    }
}
