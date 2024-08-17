# Exemplo de criação para ambiente de testes

Este é um exemplo de script para conexão do banco etl_ephem com o banco de dados de integração.

```sql
\c etl_ephem

CREATE EXTENSION postgres_fdw;

-- Crie um servidor estrangeiro
CREATE SERVER gds_ephem_elt_server
    FOREIGN DATA WRAPPER postgres_fdw
    OPTIONS (host 'ip-remoto', port '5432', dbname 'integracao');

-- Crie um usuário mapeado
CREATE USER MAPPING FOR postgres
    SERVER gds_ephem_elt_server
    OPTIONS (user 'usuario_remoto', password 'senha_remota');

-- Crie a foreign table
CREATE FOREIGN TABLE gds_ephem_integracao_foreign_table (
    id bigint, data jsonb, event_source_id varchar(100), user_email varchar(100), signal_id bigint)
    SERVER gds_ephem_elt_server
    OPTIONS (schema_name 'public', table_name 'evento_integracao');
```
