package com.mallhygiene.domain;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** 流转事件：沿编号可追溯从领用到销毁的全部记录 */
@Entity
@Table(name = "item_event")
public class ItemEvent extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    public TesterItem item;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public EventType type;

    @Column(nullable = false)
    public LocalDateTime eventAt;

    /** 经手人 / 操作人 */
    public String operator;

    public String fromLocation;

    public String toLocation;

    public String reason;

    @Column(length = 1024)
    public String note;
}
