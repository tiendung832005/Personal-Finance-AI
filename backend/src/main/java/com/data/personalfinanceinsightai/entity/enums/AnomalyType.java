package com.data.personalfinanceinsightai.entity.enums;

public enum AnomalyType {
    /** Giao dịch cao bất thường so với trung bình category (>3x) */
    UNUSUAL_AMOUNT,
    /** Danh mục chưa từng chi trong 3 tháng trước */
    NEW_CATEGORY,
    /** Chi quá nhiều lần trong thời gian ngắn */
    FREQUENCY_SPIKE,
    /** Giao dịch đơn lẻ rất lớn (>30% thu nhập tháng) */
    LARGE_SINGLE_TXN
}
