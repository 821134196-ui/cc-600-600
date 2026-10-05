package com.mallhygiene.domain;

/** 试用品全生命周期事件类型 */
public enum EventType {
    /** 备品入库 */
    STOCK_IN,
    /** 领用开封 */
    ISSUE,
    /** 调柜（仅改变柜位，开封日期与效期不变） */
    TRANSFER,
    /** 巡查登记 */
    INSPECT,
    /** 冻结 */
    FREEZE,
    /** 解冻 */
    UNFREEZE,
    /** 下架 */
    REMOVE,
    /** 销毁 */
    DESTROY
}
