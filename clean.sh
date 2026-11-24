#!/bin/bash

# Copyright (c) 2026 Huawei Technologies Co., Ltd.
# openGauss-tools-pg2og is licensed under Mulan PSL v2.
# You can use this software according to the terms and conditions of the Mulan PSL v2.
# You may obtain a copy of Mulan PSL v2 at:
#          http://license.coscl.org.cn/MulanPSL2
# THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
# EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
# MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
# See the Mulan PSL v2 for more details.

PG_USER=""
PG_HOST=""
PG_DATA_DIR=""
OG_DATA_DIR=""

if [ -d ~/pg2og_migration/ ]; then
    rm -rf ~/pg2og_migration
fi
if [ -f $OG_DATA_DIR/pg.sql ]; then
    rm -rf $OG_DATA_DIR/pg.sql
fi
ssh $PG_USER@$PG_HOST  << eeooff
    if [ -d ~/pg2og_migration/ ]; then
        rm -rf ~/pg2og_migration
    fi
    if [ -f $PG_DATA_DIR/pg.sql ]; then
        rm -rf $PG_DATA_DIR/pg.sql
    fi
eeooff

curl -X DELETE http://localhost:8083/connectors/fulfillment-connector

echo "clean done!"