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

PG_HOST=""
PG_USER=""
PG_DATA_DIR=""
OG_DBNAME=""
OG_PORT=""
OG_DATA_DIR=""
if [ ! -d ~/pg2og_migration/ ];then
    mkdir ~/pg2og_migration/
fi
if [ ! -d $OG_DATA_DIR ];then
    mkdir $OG_DATA_DIR
fi
scp  $PG_USER@$PG_HOST:$PG_DATA_DIR/pg.sql $OG_DATA_DIR/pg.sql
gsql -p $OG_PORT -d $OG_DBNAME -f $OG_DATA_DIR/pg.sql
echo "导入数据库成功"
