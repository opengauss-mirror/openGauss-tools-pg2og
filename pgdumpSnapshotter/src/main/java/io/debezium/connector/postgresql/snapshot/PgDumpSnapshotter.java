/*
 * Copyright (c) 2026 Huawei Technologies Co., Ltd.
 * openGauss-tools-pg2og is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *          http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package io.debezium.connector.postgresql.snapshot;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

import io.debezium.connector.postgresql.PostgresConnectorConfig;
import io.debezium.connector.postgresql.spi.OffsetState;
import io.debezium.connector.postgresql.spi.SlotCreationResult;
import io.debezium.connector.postgresql.spi.SlotState;
import io.debezium.connector.postgresql.spi.Snapshotter;
import io.debezium.relational.TableId;
import java.util.Date;
import java.text.SimpleDateFormat;

public class PgDumpSnapshotter implements Snapshotter {
    private OffsetState sourceInfo;
    @Override
    public Optional<String> buildSnapshotQuery(TableId tableId) {
        StringBuilder q = new StringBuilder();
        q.append("SELECT * FROM ");
        q.append(tableId.toDoubleQuotedString());
        q.append(" limit 1");
        return Optional.of(q.toString());
    }

    @Override
    public Optional<String> snapshotTableLockingStatement(Duration lockTimeout, Set<TableId> tableIds) {
        return Optional.empty();
    }

    @Override
    public String snapshotTransactionIsolationLevelStatement(SlotCreationResult newSlotInfo) {
	SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        if (newSlotInfo != null) {
            try {
                String cmd1="sh ~/pg2og_migration/export.sh"+" "+newSlotInfo.snapshotName();
                String cmd2="sh ~/pg2og_migration/import.sh";
                System.out.println(df.format(new Date())+"  "+"outline migration start...");
                Process ps = Runtime.getRuntime().exec(new String[] {"/bin/sh","-c",cmd1});
                ps.waitFor();
                ps=Runtime.getRuntime().exec(new String[] {"/bin/sh","-c",cmd2});
                ps.waitFor();
                System.out.println(df.format(new Date())+"  "+"outline migration end...");
            } catch (Exception e) {
                e.printStackTrace();
            }
            String snapSet = String.format("SET TRANSACTION SNAPSHOT '%s';", newSlotInfo.snapshotName());
            return "SET TRANSACTION ISOLATION LEVEL REPEATABLE READ; \n" + snapSet;
        }
        return Snapshotter.super.snapshotTransactionIsolationLevelStatement(newSlotInfo);
    }

    public void init(PostgresConnectorConfig config, OffsetState sourceInfo, SlotState slotState) {
        this.sourceInfo = sourceInfo;
    }

    @Override
    public boolean shouldStream() {
        return true;
    }

    @Override
    public boolean shouldSnapshot() {
        if (sourceInfo == null) {
            return true;
        }
        else if (sourceInfo.snapshotInEffect()) {
            return true;
        }
        else {
            return false;
        }
    }
}
