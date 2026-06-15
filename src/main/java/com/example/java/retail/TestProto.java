package com.example.java.retail;

import com.google.cloud.retail.v2.ProductDetail;
import java.lang.reflect.Method;

public class TestProto {
    public static void main(String[] args) {
        for (Method m : ProductDetail.Builder.class.getMethods()) {
            if (m.getName().toLowerCase().contains("quantity")) {
                System.out.println(m.getName() + " " + m.getParameterTypes()[0].getName());
            }
        }
    }
}
