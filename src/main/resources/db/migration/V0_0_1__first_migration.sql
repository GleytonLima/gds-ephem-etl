create table configuracao
(
    id                        bigserial not null primary key,
    intervalo_etl_em_segundos bigint    not null,
    url_db_remoto             varchar(255),
    created_at                timestamp,
    updated_at                timestamp
);

create table sinal
(
    id         bigserial not null primary key,
    signal_id  bigint    not null,
    dados      jsonb     not null,
    created_at timestamp,
    updated_at timestamp
);

INSERT INTO configuracao (url_db_remoto, intervalo_etl_em_segundos, created_at, updated_at)
VALUES ('http://localhost:8080/api-integracao/v1/signals', 120, NOW(), NOW());
