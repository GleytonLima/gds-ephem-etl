#!/bin/bash

# Lê as variáveis de ambiente
POSTGRES_USER=$POSTGRES_USER
REMOTE_DB_HOST=$REMOTE_DB_HOST
REMOTE_DB_PORT=$REMOTE_DB_PORT
REMOTE_DB_NAME=$REMOTE_DB_NAME
REMOTE_DB_USERNAME=$REMOTE_DB_USERNAME
REMOTE_DB_PASSWORD=$REMOTE_DB_PASSWORD
REMOTE_TABLE_NAME=$REMOTE_TABLE_NAME

# create databases
psql -c "CREATE DATABASE $ETL_DB_SCHEMA_NAME;"

# Gere e execute o SQL diretamente usando psql -c
psql -U postgres -d $ETL_DB_SCHEMA_NAME -a <<EOF
CREATE EXTENSION postgres_fdw;

-- Crie um servidor estrangeiro
CREATE SERVER gds_ephem_elt_server
    FOREIGN DATA WRAPPER postgres_fdw
    OPTIONS (host '$REMOTE_DB_HOST', port '$REMOTE_DB_PORT', dbname '$REMOTE_DB_NAME');

-- Crie um usuário mapeado
CREATE USER MAPPING FOR $POSTGRES_USER
    SERVER gds_ephem_elt_server
    OPTIONS (user '$REMOTE_DB_USERNAME', password '$REMOTE_DB_PASSWORD');

-- Crie a foreign table
CREATE FOREIGN TABLE gds_ephem_integracao_foreign_table (
    id bigint, data jsonb, event_source_id varchar(100), user_email varchar(100), signal_id bigint)
    SERVER gds_ephem_elt_server
    OPTIONS (schema_name 'public', table_name '$REMOTE_TABLE_NAME');
EOF
