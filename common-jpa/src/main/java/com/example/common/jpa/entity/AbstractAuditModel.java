package com.example.common.jpa.entity;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 * Common base class for entities
 * </p>
 *
 * @author yangkai.shen
 * @date Created in 2018-11-07 14:01
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Data
public abstract class AbstractAuditModel implements Serializable {
    /**
     * Primary key
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Creation time
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "create_time", nullable = false, updatable = false)
    @CreatedDate
    private Date createTime;

    /**
     * Last update time
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_update_time", nullable = false)
    @LastModifiedDate
    private Date lastUpdateTime;
}
