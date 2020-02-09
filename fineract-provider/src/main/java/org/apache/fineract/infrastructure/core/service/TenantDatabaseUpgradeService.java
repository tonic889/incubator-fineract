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
package org.apache.fineract.infrastructure.core.service;

import com.google.common.collect.ImmutableMap;
import com.googlecode.flyway.core.Flyway;
import com.googlecode.flyway.core.api.FlywayException;
import com.googlecode.flyway.core.util.jdbc.DriverDataSource;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import org.apache.fineract.infrastructure.core.boot.JDBCDriverConfig;
import org.apache.fineract.infrastructure.core.boot.db.TenantDataSourcePortFixService;
import org.apache.fineract.infrastructure.core.domain.FineractPlatformTenant;
import org.apache.fineract.infrastructure.core.domain.FineractPlatformTenantConnection;
import org.apache.fineract.infrastructure.security.service.TenantDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * A service that picks up on tenants that are configured to auto-update their
 * specific schema on application startup.
 */
@Service
public class TenantDatabaseUpgradeService {

    private final static Logger LOG = LoggerFactory.getLogger(TenantDatabaseUpgradeService.class);

    private final TenantDetailsService tenantDetailsService;
    protected final DataSource tenantDataSource;
    protected final TenantDataSourcePortFixService tenantDataSourcePortFixService;

    @Autowired private JDBCDriverConfig driverConfig;

    @Autowired
    public TenantDatabaseUpgradeService(final TenantDetailsService detailsService,
            @Qualifier("tenantDataSourceJndi") final DataSource dataSource, TenantDataSourcePortFixService tenantDataSourcePortFixService) {
        this.tenantDetailsService = detailsService;
        this.tenantDataSource = dataSource;
        this.tenantDataSourcePortFixService = tenantDataSourcePortFixService;
    }

    @PostConstruct
    public void upgradeAllTenants() {
        upgradeTenantDB();
        final List<FineractPlatformTenant> tenants = this.tenantDetailsService.findAllTenants();
        for (final FineractPlatformTenant tenant : tenants) {
            final FineractPlatformTenantConnection connection = tenant.getConnection();
            if (connection.isAutoUpdateEnabled()) {
                final Flyway flyway = new Flyway();
                String connectionProtocol = driverConfig.constructProtocol(connection.getSchemaServer(), connection.getSchemaServerPort(), connection.getSchemaName()) ;
                DriverDataSource source = new DriverDataSource(driverConfig.getDriverClassName(), connectionProtocol, connection.getSchemaUsername(), connection.getSchemaPassword()) ;
                flyway.setDataSource(source);
                flyway.setLocations("sql/migrations/core_db");
                flyway.setOutOfOrder(true);
                try {
                    flyway.migrate();
                } catch (FlywayException e) {
                    String betterMessage = e.getMessage() + "; for Tenant DB URL: " + connectionProtocol + ", username: "
                            + connection.getSchemaUsername();
                    throw new FlywayException(betterMessage, e.getCause());
                }
            }
        }
    }

    /**
     * Initializes, and if required upgrades (using Flyway) the Tenant DB
     * itself.
     */
    private void upgradeTenantDB() {
        String dbHostname = getEnvVar("FINERACT_DEFAULT_TENANTDB_HOSTNAME", "localhost");
        String dbPort = getEnvVar("FINERACT_DEFAULT_TENANTDB_PORT", "3306");
        String dbUid = getEnvVar("FINERACT_DEFAULT_TENANTDB_UID", "root");
        String dbPwd = getEnvVar("FINERACT_DEFAULT_TENANTDB_PWD", "mysql");
        LOG.info("upgradeTenantDB: FINERACT_DEFAULT_TENANTDB_HOSTNAME = {}, FINERACT_DEFAULT_TENANTDB_PORT = {}", dbHostname, dbPort);

        final Flyway flyway = new Flyway();
        flyway.setDataSource(tenantDataSource);
        flyway.setLocations("sql/migrations/list_db");
        flyway.setOutOfOrder(true);
        flyway.setPlaceholders(ImmutableMap.of( // FINERACT-773
                "fineract_default_tenantdb_hostname", dbHostname,
                "fineract_default_tenantdb_port",     dbPort,
                "fineract_default_tenantdb_uid",      dbUid,
                "fineract_default_tenantdb_pwd",      dbPwd));
        flyway.migrate();

        tenantDataSourcePortFixService.fixUpTenantsSchemaServerPort();
    }

    private String getEnvVar(String name, String defaultValue) {
        String value = System.getenv(name);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }
}
