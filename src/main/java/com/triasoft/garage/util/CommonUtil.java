package com.triasoft.garage.util;

import java.math.BigDecimal;

public class CommonUtil {

    public static double calculateDelta(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }
        return current.subtract(previous)
                .divide(previous, 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .doubleValue();
    }
}
