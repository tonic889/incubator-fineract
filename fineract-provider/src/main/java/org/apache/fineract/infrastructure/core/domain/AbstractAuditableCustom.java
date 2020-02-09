/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.infrastructure.core.domain;

import java.io.Serializable;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.apache.fineract.useradministration.domain.AppUser;
import org.springframework.data.domain.Auditable;
import org.springframework.data.jpa.domain.AbstractAuditable;

/**
 * A custom copy of {@link AbstractAuditable} to override the column names used
 * on database.
 *
 * Abstract base class for auditable entities. Stores the audition values in
 * persistent fields.
 *
 * @param <U>
 *            the auditing type. Typically some kind of user.
 * @param <PK>
 *            the type of the auditing type's identifier
 */
@MappedSuperclass
public abstract class AbstractAuditableCustom<U, PK extends Serializable> extends AbstractPersistableCustom<PK> implements Auditable<AppUser, Long, Instant> {

    private static final long serialVersionUID = 141481953116476081L;

    @OneToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "createdby_id")
    private AppUser createdBy;

    @Column(name = "created_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdDate;

    @OneToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "lastmodifiedby_id")
    private AppUser lastModifiedBy;

    @Column(name = "lastmodified_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastModifiedDate;

    /*
     * (non-Javadoc)
     *
     * @see org.springframework.data.domain.Auditable#getCreatedBy()
     */
    @Override
    public Optional<AppUser> getCreatedBy() {
        return Optional.ofNullable(this.createdBy);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.springframework.data.domain.Auditable#setCreatedBy(java.lang.Object)
     */
    @Override
    public void setCreatedBy(final AppUser createdBy) {

        this.createdBy = createdBy;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.springframework.data.domain.Auditable#getCreatedDate()
     */
    @Override
    public Optional<Instant> getCreatedDate() {
        return null == this.createdDate ? Optional.empty() : Optional.of(this.createdDate.toInstant());
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.springframework.data.domain.Auditable#setCreatedDate(T)
     */
    @Override
    public void setCreatedDate(final Instant createdDate) {
        this.createdDate = null == createdDate ? null : Date.from(createdDate);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.springframework.data.domain.Auditable#getLastModifiedBy()
     */
    @Override
    public Optional<AppUser> getLastModifiedBy() {
        return Optional.ofNullable(this.lastModifiedBy);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.springframework.data.domain.Auditable#setLastModifiedBy(java.lang
     * .Object)
     */
    @Override
    public void setLastModifiedBy(final AppUser lastModifiedBy) {

        this.lastModifiedBy = lastModifiedBy;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.springframework.data.domain.Auditable#getLastModifiedDate()
     */
    @Override
    public Optional<Instant> getLastModifiedDate() {
        return null == this.lastModifiedDate ? Optional.empty() : Optional.of(this.lastModifiedDate.toInstant());
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.springframework.data.domain.Auditable#setLastModifiedDate(T)
     */
    @Override
    public void setLastModifiedDate(final Instant lastModifiedDate) {
        this.lastModifiedDate = null == lastModifiedDate ? null : Date.from(lastModifiedDate);
    }
}
