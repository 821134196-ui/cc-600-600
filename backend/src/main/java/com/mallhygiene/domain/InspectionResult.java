package com.mallhygiene.domain;

/** 巡查结论 */
public enum InspectionResult {
    /** 正常 */
    NORMAL,
    /** 污染（立即下架） */
    CONTAMINATED,
    /** 临期 */
    NEAR_EXPIRY,
    /** 到期（立即下架） */
    EXPIRED
}
