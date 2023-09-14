-- remove coluna url_db_remoto de configuracao
alter table configuracao drop column url_db_remoto;
-- alterando a tabela configuracao para adicionar a coluna dominio_remoto
alter table configuracao add column dominio_remoto varchar(255);
-- adicionando a coluna dominio_remoto na tabela configuracao
update configuracao set dominio_remoto = 'https://vbeapi.online' where id = 1;
-- cria tabela acao_tomada
create table acao_tomada
(
    id         bigserial not null primary key,
    dados      jsonb     not null,
    created_at timestamp,
    updated_at timestamp
);
-- cria tabela recomendacao_tecnica
create table recomendacao_tecnica
(
    id         bigserial not null primary key,
    dados      jsonb     not null,
    created_at timestamp,
    updated_at timestamp
);
-- cria tabela fonte
create table fonte
(
    id         bigserial not null primary key,
    dados      jsonb     not null,
    created_at timestamp,
    updated_at timestamp
);