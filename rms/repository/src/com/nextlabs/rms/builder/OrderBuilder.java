package com.nextlabs.rms.builder;

import com.nextlabs.common.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.criterion.Order;

public class OrderBuilder {

    private final Map<String, String> supportedFields;
    private final Map<String, Order> orders = new LinkedHashMap<>();

    public OrderBuilder(String... supportedFields) {
        if (supportedFields == null) {
            throw new IllegalArgumentException("Invalid fields");
        }
        Map<String, String> map = new HashMap<>(supportedFields.length);
        for (String s : supportedFields) {
            map.put(s, s);
        }
        this.supportedFields = Collections.unmodifiableMap(map);
    }

    public OrderBuilder(Map<String, String> supportedFields) {
        if (supportedFields == null) {
            throw new IllegalArgumentException("Invalid fields");
        }
        this.supportedFields = Collections.unmodifiableMap(supportedFields);
    }

    public OrderBuilder add(String orderBy) {
        String fieldName = orderBy;
        boolean desc = StringUtils.startsWith(orderBy, "-");
        fieldName = desc ? StringUtils.substringAfter(fieldName, "-") : orderBy;
        return add(fieldName, !desc);
    }

    public OrderBuilder add(String fieldName, boolean asc) {
        String field = supportedFields.get(fieldName);
        if (field == null) {
            return this;
        }
        Order order = asc ? Order.asc(field) : Order.desc(field);
        orders.put(fieldName, order);
        return this;
    }

    public List<Order> build() {
        try {
            Collection<Order> values = orders.values();
            return new ArrayList<>(values);
        } finally {
            orders.clear();
        }
    }
}
