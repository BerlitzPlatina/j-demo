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
    @Column(name = "id")
    private Long id;

    /**
     * Creation time
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private Date createdAt;

    /**
     * Last update time
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at", nullable = false)
    @LastModifiedDate
    private Date updatedAt;

    /**
     * Backward-compatible alias for existing code that still uses the old property
     * names.
     */
    public Date getCreateTime() {
        return createdAt;
    }

    public void setCreateTime(Date createTime) {
        this.createdAt = createTime;
    }

    public Date getLastUpdateTime() {
        return updatedAt;
    }

    public void setLastUpdateTime(Date lastUpdateTime) {
        this.updatedAt = lastUpdateTime;
    }
}
