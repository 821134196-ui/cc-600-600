package com.mallhygiene.domain;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** 店员巡查记录：污染 / 临期 / 到期 / 正常 */
@Entity
@Table(name = "inspection_record")
public class InspectionRecord extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    public TesterItem item;

    @Column(nullable = false)
    public LocalDateTime inspectedAt;

    @Column(nullable = false)
    public String inspector;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public InspectionResult result;

    @Column(length = 1024)
    public String note;
}
