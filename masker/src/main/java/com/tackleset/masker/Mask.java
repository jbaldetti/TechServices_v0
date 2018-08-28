package com.tackleset.masker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class Mask {

    private static ObjectMapper objectMapper;

    enum FORMAT {
        NUM10LEAD0, PERSCODE;

        public Object format(Object value) {
            switch (this) {
                case NUM10LEAD0:
                    return String.format("%010d", (Number) value);
                case PERSCODE:
                    return String.format("%02d", (Number) value);
            }
            return "F";
        }
    }

    enum TYPE {
        text, alphanumeric, unsigned, date, decimal;

        public boolean validate(Object value) {
            if ((this.equals(TYPE.alphanumeric) || this.equals(TYPE.text)) && !(value instanceof String)) {
                return false;
            }
            if (this.equals(TYPE.unsigned) || this.equals(TYPE.decimal)) {
                if (value instanceof String) {
                    return StringUtils.isNumeric((String) value);
                }
                if (!(value instanceof Number)) {
                    return false;
                }
            }
            return true;
        }
    }

    static final String CONST_SSN = "123121234";

    enum HINT {
        unique, random, const_ssn, list_first_names, list_last_names, list_genders, address_address_1, address_address_2,
        address_city, address_state, address_zip;

        public Object mask(Object value, int iteration, TYPE type) {
            if (this.name().startsWith("list_")) {
                List<Object> list = LIST_MAP.get(this.name());
                return list.get(iteration % list.size());
            }
            if (this.name().startsWith("address_")) {
                Map<String, Object> map = AddressList.get(iteration % AddressList.size());
                return map.get(this.name().substring("address_".length()));
            }
            if (this.name().startsWith("const_")) {
                String constant = CONST_MAP.get(this.name());
                return constant;
            }
            if (this.equals(HINT.unique)) {
                return iteration;
            }
            if (this.equals(HINT.random)) {
                Random random = new Random();
                switch (type) {
                    case decimal:
                    case unsigned: {
                        Number num = null;
                        if (value instanceof String) {
                            String str = (String) value;
                            int size = (int) (Math.pow(10, str.length()) - 1);
                            int randomInt = random.nextInt((size > Integer.MAX_VALUE) ? Integer.MAX_VALUE : size);
                            return randomInt;
                        }
                    }
                    case date: {
                        int year = random.nextInt(100) + 1910;
                        int month = random.nextInt(12)+1;
                        int day = random.nextInt(DAYSOFMONTH.get(month - 1).intValue())+1;
                        LocalDate ld = LocalDate.of(year, month, day);
                        return ld.toString();
                    }
                }
            }
            return "M";
        }
    }

    static final List<Integer> DAYSOFMONTH = Arrays.asList(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31);
    public static final String ADDRESSES =
            "[" +
                    "{ \"address_1\": \"1 Main Plaza\",\n" +
                    "    \"address_2\": \"Number 10\",\n" +
                    "    \"city\": \"Shell\",\n" +
                    "    \"state\": \"CA\",\n" +
                    "    \"zip\": \"90210\"}," +
                    " { \"address_1\": \"2 Loss Rd\",\n" +
                    "    \"address_2\": \"Apt A\",\n" +
                    "    \"city\": \"Baltimore\",\n" +
                    "    \"state\": \"GA\",\n" +
                    "    \"zip\": \"66753\"}" +
                    "]\n";

    static List<Map<String, Object>> AddressList;

    static {
        objectMapper = new ObjectMapper();
        try {
            AddressList = objectMapper.readValue(ADDRESSES, List.class);
        } catch (IOException e) {
           e.printStackTrace();
        }
    }

    static final List<Object> LIST_GENDERS = Arrays.asList("male", "female");
    static final List<Object> LIST_FIRST_NAMES = Arrays.asList("Jo", "Tracy");
    static final List<Object> LIST_LAST_NAMES = Arrays.asList("Armstrong", "Smith");
    static final Map<String, List<Object>> LIST_MAP = ImmutableMap.of("list_genders", LIST_GENDERS,
            "list_first_names", LIST_FIRST_NAMES,
            "list_last_names", LIST_LAST_NAMES);
    static final Map<String, String> CONST_MAP = ImmutableMap.of("const_ssn", CONST_SSN);

    static final String SENSITIVES_KEY = "member_id";
    static final Map<String, Map<Class, String>> SENSITIVES = new ImmutableMap.Builder<String, Map<Class, String>>()
            .put("member_id", ImmutableMap.of(HINT.class, HINT.unique.name(), TYPE.class, TYPE.unsigned.name(), FORMAT.class, FORMAT.NUM10LEAD0.name()))
            .put("person_code", ImmutableMap.of(HINT.class, HINT.random.name(), TYPE.class, TYPE.unsigned.name(), FORMAT.class, FORMAT.PERSCODE.name()))
            .put("first_name", ImmutableMap.of(HINT.class, HINT.list_first_names.name(), TYPE.class, TYPE.text.name()))
            .put("last_name", ImmutableMap.of(HINT.class, HINT.list_last_names.name(), TYPE.class, TYPE.text.name()))
            .put("date_of_birth", ImmutableMap.of(HINT.class, HINT.random.name(),TYPE.class, TYPE.date.name()))
            .put("gender", ImmutableMap.of(HINT.class, HINT.list_genders.name(), TYPE.class, TYPE.text.name()))
            .put("ssn", ImmutableMap.of(HINT.class, HINT.const_ssn.name(), TYPE.class, TYPE.unsigned.name()))
            .put("address_1", ImmutableMap.of(HINT.class, HINT.address_address_1.name(), TYPE.class, TYPE.alphanumeric.name()))
            .put("address_2", ImmutableMap.of(HINT.class, HINT.address_address_2.name(), TYPE.class, TYPE.alphanumeric.name()))
            .put("city", ImmutableMap.of(HINT.class, HINT.address_city.name(), TYPE.class, TYPE.text.name()))
            .put("state", ImmutableMap.of(HINT.class, HINT.address_state.name(), TYPE.class, TYPE.text.name()))
            .put("zip", ImmutableMap.of(HINT.class, HINT.address_zip.name(), TYPE.class, TYPE.unsigned.name()))
            .build();

    static final Map<String, Map<Class, String>> NON_SENSITIVES = new ImmutableMap.Builder<String, Map<Class, String>>()
            .put("prescription_number", ImmutableMap.of(TYPE.class, TYPE.unsigned.name(), FORMAT.class, FORMAT.NUM10LEAD0.name()))
            .put("drug_id", ImmutableMap.of(TYPE.class, TYPE.unsigned.name(), FORMAT.class, FORMAT.NUM10LEAD0.name()))
            .put("quantity", ImmutableMap.of(TYPE.class, TYPE.unsigned.name()))
            .put("days_supply", ImmutableMap.of(TYPE.class, TYPE.unsigned.name()))
            .put("strength", ImmutableMap.of(TYPE.class, TYPE.alphanumeric.name()))
            .put("total_cost", ImmutableMap.of(TYPE.class, TYPE.decimal.name()))
            .put("copay", ImmutableMap.of(TYPE.class, TYPE.decimal.name()))
            .put("ingredient_cost", ImmutableMap.of(TYPE.class, TYPE.decimal.name()))
            .put("dispensing_fee", ImmutableMap.of(TYPE.class, TYPE.decimal.name()))
            .build();


    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.format("Please enter path to a json file containing 'flat records' (json array of objects) as first argument");
            return;
        }
        Mask Mask = new Mask();
        objectMapper = new ObjectMapper();
        try {
            List<Map<String, Object>> recordList = objectMapper.readValue(
                    new File(args[0]), List.class);
            List<Map<String, Object>> newRecords = Mask.anonymizeRecords(recordList);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(
                    new File(args[0] + ".masked.json"), newRecords);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<Map<String, Object>> anonymizeRecords(List<Map<String, Object>> records) {
        int count = 0;
//        Map<String, Object> prevMap = new HashMap<>();
//        Map<String, Object> prevConverted = new HashMap<>();
        Map<String, Map<String, Object[]>> sensitiveValues = new LinkedHashMap<>();
        for (Map<String, Object> map : records) {
            count++;
            System.out.println(map.toString());
//            Map<String, Object[]> tempMap = new HashMap(map);
            anonymizeRecord(/* prevMap, prevConverted,*/ map, sensitiveValues, count);
            System.out.println(map.toString());
            System.out.println();
  //          prevMap = tempMap;
  //          prevConverted = map;
        }
        return records;
    }

    private static Map<String, Object> anonymizeRecord(/*Map<String, Object> prev, Map<String, Object> prevConverted,*/
                                                       Map<String, Object> record,
                                                       Map<String, Map<String, Object[]>> sensitiveTable, int iteration) {
        Map<String, Object[]> sensitiveValues = new LinkedHashMap<>();
        Map<String, Object[]> prevTable = null;
        prevTable = sensitiveTable.get(SENSITIVES_KEY);
        if (prevTable == null) {
            sensitiveTable.put(SENSITIVES_KEY, sensitiveValues);
        }
        for (String key : record.keySet()) {
            Map<Class, String> sensitive = SENSITIVES.get(key);
            Object value = record.get(key);
            Object newValue = null;
            Object prevValue = null;
            if (prevTable != null) {
                Object[] prevValues = prevTable.get(key);
                if (prevValues != null) {
                    prevValue = prevValues[0];
                }
            }
            if (sensitive != null) {
//                Object prevValue =

    //            prevValue = sensitiveTable.get()
                if (prevValue == null || !prevValue.equals(value)) {
                    String strHint = sensitive.get(HINT.class);
                    String strType = sensitive.get(TYPE.class);
                    if (StringUtils.isNotEmpty(strHint)) {
                        HINT hint = HINT.valueOf(strHint);
                        if (StringUtils.isNotEmpty(strType)) {
                            newValue = hint.mask(value, iteration, TYPE.valueOf(strType));
                        } else {
                            newValue = hint.mask(value, iteration, TYPE.alphanumeric);
                        }
                        String strFormat = sensitive.get(FORMAT.class);
                        if (StringUtils.isNotEmpty(strFormat)) {
                            FORMAT format = FORMAT.valueOf(strFormat);
                            newValue = format.format(newValue);
                        }
                        record.put(key, newValue);
                    }
                } else {
                    record.put(key, prevTable.get(key)[1]);
                }
            } else {
                Map<Class, String> nonSensitive = NON_SENSITIVES.get(key);
 //               Object prevValue = prev.get(key);
                if (nonSensitive == null) {
                    if (prevValue == null || !prevValue.equals(value)) {
                        if (value instanceof String) {
                            newValue = HINT.unique.mask(value, iteration, TYPE.alphanumeric);
                        }
                        record.put(key, newValue);
                    } else {
                        record.put(key, prevTable.get(key)[1]);
                    }
                } else {
                        String strType = nonSensitive.get(TYPE.class);
                        if (value != null && !strType.isEmpty()) {
                            TYPE type = TYPE.valueOf(strType);
                            if (!type.validate(value)) {
                                record.put(key, "MISMATCH TYPE");
                            }
                            if (value instanceof String && ((String) value).length() > 7) {
                                if (sensitiveValues.values().stream().map(s -> s[0]).collect(Collectors.toList()).contains(value)) {
                                    record.put(key, "SENSITIVE MATCHING VALUE");
                                    continue;
                                }
                            }
                        }
                }
            }
            if (newValue != null && !newValue.equals(value)) {
                sensitiveValues.put(key, new Object[] {value, newValue});
            }
        }
        return record;
    }
}
