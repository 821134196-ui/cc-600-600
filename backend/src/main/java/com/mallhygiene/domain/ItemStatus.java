package com.mallhygiene.domain;

/** 试用品状态机：备品 → 在用 → (冻结) → 下架 → 销毁 */
public enum ItemStatus {
    /** 未开封备品 */
    SEALED,
    /** 已开封在用 */
    IN_USE,
    /** 冻结（不得再次领用） */
    FROZEN,
    /** 已下架，待销毁 */
    REMOVED,
    /** 已销毁 */
    DESTROYED
}
