# Projeto de ETL dos Sinais do Ephem

[![coverage](https://raw.githubusercontent.com/GleytonLima/gds-ephem-etl/badges/jacoco.svg)](https://github.com/GleytonLima/gds-ephem-etl/actions/workflows/build_and_publish.yaml) [![branches coverage](https://raw.githubusercontent.com/GleytonLima/gds-ephem-etl/badges/branches.svg)](https://github.com/GleytonLima/gds-ephem-etl/actions/workflows/build_and_publish.yaml)

Esta aplicação age como um middleware entre o aplicativo guardiões da saúde e o ephem.

```mermaid
graph TD
    subgraph K[ETL App]
        B[Aplicação]
        C[Banco de Dados]
    end

    subgraph H[Integração App]
        D[Aplicação]
        E[Banco de Dados]
    end

    subgraph F[Google Data Studio]
        G[Paineis]
    end
    subgraph J[Ephem]
        A[Ephem]
    end
    F --> C
    B --> |Persistir Sinais| C
    B --> |Consultar Sinais\nPeriodicamente| D
    D --> |Consultar Sinais| J
    C --> |Foreign Table| E
```

## Documentação da API

{{dominio}}/api-etl/v1/swagger-ui/#/

## Subindo a aplicação localmente

### Pré-requisitos

- Java 11
- Docker e Docker Compose Instalados na máquina

### Configurando arquivos .env

Nas pastas [docker/db](docs/docker/db) e [docker/app](docs/docker/app) há um arquivo `.env.example` Faça uma cópia dos
mesmos como valor `.env` e ajuste os valores.

Acesse a [pasta docker](docs/docker) e execute o comando `docker compose up -d`.

```bash
cd docker
docker-compose -f docker-compose-with-app.yml up -d
```

A imagem do banco de dados será construida a partir do arquivo [docker/db/Dockerfile](docs/docker/db/Dockerfile)
e a versão do app de integração será baixado
do [repositório público](https://hub.docker.com/repository/docker/gleytonlima/gds-ephem-integracao/general).

Utilize a [collection do postman](docs/gds2ephem.postman_collection.json) para executar requisições na aplicação.

Acesso a página de homologação do Ephem da UNB para verificar o resultado.

## Desenvolvimento

### Configure as variáveis de ambiente

Acesse a pasta [docs/docker](docs/docker), crie uma copia do arquivo `.env.example` como `.env` e preencha os valores
das variaveis de ambiente

Cadastre manualmente as variáveis de ambiente do arquivo .env na sua conta do windows.

Faça o mesmo para a pasta [docker/db](docs/docker/db). Crie o arquivo `.env` a partir do exemplo e preencha os valores.

### Suba o container docker do banco de dados em Postgres

Para subir o banco de dados localmente acesse a [pasta docker](docs/docker) e execute:

```bash
cd docs/docker
docker-compose up -d
```

Clone este projeto e abra o [Intellij IDE](https://www.jetbrains.com/idea/).
Para executar o projeto localmente, execute a
classe [Application](src/main/java/br/unb/sds/gdsephemetl/Application.java).

## Foreign Table

Este projeto usa uma [foreign table](https://www.postgresql.org/docs/current/sql-createforeigntable.html) para acessar
os dados do banco de dados do aplicativo de integração.

Para consultar os dados da foreign table execute o comando:

```sql
select *
from gds_ephem_integracao_foreign_table;
```

A criação do foreign table é feita pelo [script](docs/docker/init-scripts/initdb.sh) que é executado quando o container
do banco de dados é iniciado.

Neste caso também é necessário criar um usuário no banco de dados remoto com as permissões necessárias para acessar a
foreign table.

```sql
psql -U postgres
CREATE USER << USER >> WITH PASSWORD <<PASS>> LOGIN;

REVOKE CONNECT ON DATABASE << DATABASE >> FROM PUBLIC;
GRANT CONNECT ON DATABASE << DATABASE >> TO <<NOME_USUARIO>>;

ALTER
USER
<<USER>> CONNECTION LIMIT 10;
ALTER USER
<<USER>>
SET CONFIGURATION_PARAMETER_NAME = 'pg_hba.conf', 'host', '<<DATABASE>>', '<<USER>>', '<<IP>>', 'md5';

\c <<DATABASE>>

GRANT USAGE ON SCHEMA public TO << USER >>;

GRANT SELECT ON TABLE << TABLE >> TO etl_user;
```

## Criação de views

Para fins de exibição dos dados no Google Data Studio, podem ser criadas views no banco de dados.

```sql
DROP VIEW IF EXISTS gds_ephem_integracao_view;
CREATE OR REPLACE VIEW public.gds_ephem_integracao_view
AS SELECT gds.event_source_id AS gds_id,
    gds.signal_id,
    gds.user_email,
    gds.data ->> 'evento_afetados'::text AS evento_afetados,
    gds.data ->> 'evento_detalhes'::text AS evento_detalhes,
    gds.data ->> 'evento_descricao'::text AS evento_descricao,
    gds.data ->> 'evento_data_ocorrencia'::text AS evento_data_ocorrencia,
    gds.data ->> 'evento_pais_ocorrencia'::text AS evento_pais_ocorrencia,
    gds.data ->> 'evento_qtde_envolvidos'::text AS evento_qtde_envolvidos,
    gds.data ->> 'evento_local_ocorrencia'::text AS evento_local_ocorrencia,
    gds.data ->> 'evento_estado_ocorrencia'::text AS evento_estado_ocorrencia,
    gds.data ->> 'evento_sabe_quando_ocorreu'::text AS evento_sabe_quando_ocorreu,
    COALESCE(gds.data ->> 'evento_cidade_ocorrencia'::text, gds.data ->> 'evento_municipio_ocorrencia'::text) AS evento_municipio_ocorrencia,
    sinal.dados ->> 'id'::text AS ephem_signal_id,
    (sinal.dados -> 'signal_stage_state_id'::text) ->> 1 AS ephem_sinal_status,
    (sinal.dados -> 'general_hazard_id'::text) ->> 1 AS ephem_general_hazard,
    (sinal.dados -> 'specific_hazard_id'::text) ->> 1 AS ephem_specific_hazard,
    sinal.dados ->> 'confidentiality'::text AS ephem_confidentiality,
    NULLIF(sinal.dados ->> 'incident_date'::text, 'false') AS ephem_incident_date,
    sinal.dados ->> 'name'::text AS ephem_name,
    sinal.dados ->> 'active'::text AS ephem_active,
    sinal.dados ->> 'outcome'::text AS ephem_outcome,
    (sinal.dados -> 'tag_ids'::text) ->> 1 AS ephem_tag,
    (sinal.dados -> 'state_id'::text) ->> 1 AS ephem_state,
    sinal.dados ->> 'was_event'::text AS ephem_was_event,
    (sinal.dados -> 'country_id'::text) ->> 1 AS ephem_country,
    sinal.dados ->> 'was_closed'::text AS ephem_was_closed,
    NULLIF(sinal.dados ->> 'create_date'::text, 'false') AS ephem_create_date,
    NULLIF(sinal.dados ->> 'date_closed'::text, 'false') AS ephem_date_closed,
    sinal.dados ->> 'description'::text AS ephem_description,
    sinal.dados -> 'message_ids'::text AS ephem_message_ids,
    NULLIF(sinal.dados ->> 'report_date'::text, 'false') AS ephem_report_date,
    sinal.dados ->> 'signal_type'::text AS ephem_signal_type,
    (sinal.dados -> 'aetiology_id'::text) ->> 1 AS ephem_aetiology,
    sinal.dados ->> 'display_name'::text AS ephem_display_name,
    sinal.dados -> 'district_ids'::text AS ephem_district_ids,
    sinal.dados ->> 'verification'::text AS ephem_verification,
    NULLIF(sinal.dados ->> '__last_update'::text, 'false') AS ephem_last_update,
    sinal.dados ->> 'verified_date'::text AS ephem_verified_date,
    sinal.dados ->> 'was_discarded'::text AS ephem_was_discarded,
    sinal.dados ->> 'was_monitored'::text AS ephem_was_monitored,
    sinal.dados ->> 'was_event_date'::text AS ephem_was_event_date,
    sinal.dados ->> 'is_event_closed'::text AS ephem_is_event_closed,
    sinal.dados ->> 'people_affected'::text AS ephem_people_affected,
    sinal.dados ->> 'was_closed_date'::text AS ephem_was_closed_date,
    sinal.dados ->> 'animals_affected'::text AS ephem_animals_affected,
    sinal.dados ->> 'was_discarded_date'::text AS ephem_was_discarded_date,
    sinal.dados ->> 'was_monitored_date'::text AS ephem_was_monitored_date,
    sinal.dados ->> 'date_outcome_decided'::text AS ephem_date_outcome_decided,
    (sinal.dados -> 'verification_source_id'::text) ->> 1 AS ephem_verification_source,
    sinal.dados ->> 'was_under_verification'::text AS ephem_was_under_verification,
    sinal.dados ->> 'under_verification_date'::text AS ephem_under_verification_date,
    (sinal.dados -> 'outcome_justification_id'::text) ->> 1 AS ephem_outcome_justification,
    sinal.dados ->> 'was_under_verification_date'::text AS ephem_was_under_verification_date
   FROM gds_ephem_integracao_foreign_table gds
     LEFT JOIN sinal ON sinal.signal_id = gds.signal_id;
```

Uma view somente dos sinais:

```sql
DROP VIEW IF EXISTS sinal_view;
CREATE OR REPLACE VIEW sinal_view AS
SELECT sinal.dados ->> 'id'                            AS signal_id,
       sinal.dados -> 'signal_stage_state_id' ->> 1    AS ephem_sinal_status,
       sinal.dados -> 'general_hazard_id' ->> 1        AS ephem_general_hazard,
       sinal.dados -> 'specific_hazard_id' ->> 1       AS ephem_specific_hazard,
       sinal.dados ->> 'confidentiality'               AS ephem_confidentiality,
       sinal.dados ->> 'incident_date'                 AS ephem_incident_date,
       sinal.dados ->> 'name'                          AS ephem_name,
       sinal.dados ->> 'active'                        AS ephem_active,
       sinal.dados ->> 'outcome'                       AS ephem_outcome,
       sinal.dados -> 'tag_ids' ->> 1                  AS ephem_tag,
       sinal.dados -> 'state_id' ->> 1                 AS ephem_state,
       sinal.dados ->> 'was_event'                     AS ephem_was_event,
       sinal.dados -> 'country_id' ->> 1               AS ephem_country,
       sinal.dados ->> 'was_closed'                    AS ephem_was_closed,
       sinal.dados ->> 'create_date'                   AS ephem_create_date,
       sinal.dados ->> 'date_closed'                   AS ephem_date_closed,
       sinal.dados ->> 'description'                   AS ephem_description,
       sinal.dados -> 'message_ids'                    AS ephem_message_ids,
       sinal.dados ->> 'report_date'                   AS ephem_report_date,
       sinal.dados ->> 'signal_type'                   AS ephem_signal_type,
       sinal.dados -> 'aetiology_id' ->> 1             AS ephem_aetiology,
       sinal.dados ->> 'display_name'                  AS ephem_display_name,
       sinal.dados -> 'district_ids'                   AS ephem_district_ids,
       sinal.dados ->> 'verification'                  AS ephem_verification,
       sinal.dados ->> '__last_update'                 AS ephem_last_update,
       sinal.dados ->> 'verified_date'                 AS ephem_verified_date,
       sinal.dados ->> 'was_discarded'                 AS ephem_was_discarded,
       sinal.dados ->> 'was_monitored'                 AS ephem_was_monitored,
       sinal.dados ->> 'was_event_date'                AS ephem_was_event_date,
       sinal.dados ->> 'is_event_closed'               AS ephem_is_event_closed,
       sinal.dados ->> 'people_affected'               AS ephem_people_affected,
       sinal.dados ->> 'was_closed_date'               AS ephem_was_closed_date,
       sinal.dados ->> 'animals_affected'              AS ephem_animals_affected,
       sinal.dados ->> 'was_discarded_date'            AS ephem_was_discarded_date,
       sinal.dados ->> 'was_monitored_date'            AS ephem_was_monitored_date,
       sinal.dados ->> 'date_outcome_decided'          AS ephem_date_outcome_decided,
       sinal.dados -> 'verification_source_id' ->> 1   AS ephem_verification_source,
       sinal.dados ->> 'was_under_verification'        AS ephem_was_under_verification,
       sinal.dados ->> 'under_verification_date'       AS ephem_under_verification_date,
       sinal.dados -> 'outcome_justification_id' ->> 1 AS ephem_outcome_justification,
       sinal.dados ->> 'was_under_verification_date'   AS ephem_was_under_verification_date
FROM sinal;
```

Uma view para as ações tomadas:

```sql
DROP VIEW IF EXISTS acao_tomada_view;
CREATE OR REPLACE VIEW acao_tomada_view AS
SELECT dados ->> 'id'                      AS id,
       dados ->> 'name'                    AS name,
       (dados -> 'signal_id')::jsonb -> 0  AS signal_id,
       dados ->> 'start_date'              AS start_date,
       dados -> 'action_type' ->> 1        AS action_type,
       dados ->> 'create_date'             AS create_date,
       dados ->> 'action_level'            AS action_level,
       dados ->> '__last_update'           AS __last_update,
       dados ->> 'action_status'           AS action_status,
       dados ->> 'complete_date'           AS complete_date,
       dados ->> 'action_details'          AS action_details,
       dados ->> 'display_name'            AS display_name,
       dados -> 'action_focal_point' ->> 1 AS action_focal_point
FROM acao_tomada;
```

Uma view para as fontes:

```sql
DROP VIEW IF EXISTS fonte_view;
CREATE OR REPLACE VIEW fonte_view AS
SELECT dados ->> 'id'                     AS id,
       dados ->> 'name'                   AS name,
       (dados -> 'signal_id')::jsonb -> 0 AS signal_id,
       dados ->> 'create_date'            AS create_date,
       dados ->> 'source_name'            AS source_name,
       dados -> 'source_type' ->> 1       AS source_type,
       dados ->> 'display_name'           AS display_name,
       dados ->> '__last_update'          AS __last_update,
       dados ->> 'source_address'         AS source_address
FROM fonte;
```

Uma view para as recomendações técnicas:

```sql
DROP VIEW IF EXISTS recomendacao_tecnica_view;
CREATE OR REPLACE VIEW recomendacao_tecnica_view AS
SELECT dados ->> 'id'                                       AS id,
       dados ->> 'name'                                     AS name,
       (dados ->> 'note')::text                             AS note,
       dados -> 'signal_id' -> 0                            AS signal_id,
       dados ->> 'create_date'                              AS create_date,
       dados ->> 'display_name'                             AS display_name,
       dados ->> '__last_update'                            AS __last_update,
       dados -> 'justification' ->> 1                       AS justification,
       dados -> 'recommended_action' ->> 1                  AS recommended_action,
       dados ->> 'recommendation_date'                      AS recommendation_date,
       (dados ->> 'recommendation_pending_review')::boolean AS recommendation_pending_review
FROM recomendacao_tecnica;
```

## Views Mais específicas

Tem médio em dias para conclusão das ações tomadas:

```sql
DROP VIEW IF EXISTS average_time_to_complete_actions_view;
CREATE OR REPLACE VIEW average_time_to_complete_actions_view AS
SELECT DATE_TRUNC('month', ephem_report_date::timestamp) AS month,
       action_type                                       AS action_type,
       action_status,
       AVG(CASE
               WHEN complete_date IS NULL OR complete_date = 'false' THEN
                   EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - start_date::timestamp))
               ELSE
                   EXTRACT(EPOCH FROM (complete_date::timestamp - start_date::timestamp))
           END)                                          AS avg_days_to_complete
FROM acao_tomada_view
         INNER JOIN
     sinal_view ON acao_tomada_view.signal_id::text = sinal_view.signal_id
GROUP BY month, action_status, action_type;
```

Quantidade de alertas positivos (eventos) (#8):

```sql
DROP VIEW IF EXISTS alertas_positivos_view;
CREATE OR REPLACE VIEW alertas_positivos_view AS
SELECT DATE_TRUNC('month', ephem_was_event_date::timestamp)                 AS month,
       SUM(CASE
               WHEN ephem_was_monitored = 'true' OR ephem_was_under_verification = 'true' THEN 1
               ELSE 0 END)                                                  AS total_verification_or_monitored,
       SUM(CASE WHEN ephem_was_event = 'true' THEN 1 ELSE 0 END)            AS total_event,
       (SUM(CASE WHEN ephem_was_monitored = 'true' OR ephem_was_under_verification = 'true' THEN 1 ELSE 0 END) *
        1.0) /
       NULLIF(SUM(CASE WHEN ephem_was_event = 'true' THEN 1 ELSE 0 END), 0) AS percent_verification_or_monitored_by_event
FROM sinal_view
WHERE ephem_was_event = 'true'
group by month;
```

Quantidade de alertas verificados (#9):

```sql
DROP VIEW IF EXISTS alertas_confirmados_view;
CREATE OR REPLACE VIEW alertas_confirmados_view AS
SELECT DATE_TRUNC('month', ephem_report_date::timestamp)                     AS month,
       SUM(CASE WHEN ephem_verification = 'verified' THEN 1 ELSE 0 END)      AS total_verified,
       SUM(CASE WHEN ephem_report_date NOTNULL THEN 1 ELSE 0 END)            AS total_reported,
       (SUM(CASE WHEN ephem_verification = 'verified' THEN 1 ELSE 0 END) * 1.0) /
       NULLIF(SUM(CASE WHEN ephem_report_date NOTNULL THEN 1 ELSE 0 END), 0) AS percent_verified
FROM sinal_view
group by month;
```

Oportunidade Média de Detecção (#11):

```sql
DROP VIEW IF EXISTS alertas_delay_view;
CREATE OR REPLACE VIEW alertas_delay_view AS
SELECT DATE_TRUNC('month', ephem_report_date::timestamp)                                        AS month,
       AVG(EXTRACT(EPOCH FROM (ephem_report_date::timestamp - ephem_incident_date::timestamp))) AS report_delay_hours
FROM sinal_view
group by month;
```

Quantidade de alertas por localizacao (#27 e #28)

```sql
DROP VIEW IF EXISTS alertas_por_local_view;
CREATE OR REPLACE VIEW alertas_por_local_view AS
SELECT evento_pais_ocorrencia,
       evento_estado_ocorrencia,
       evento_municipio_ocorrencia,
       evento_local_ocorrencia,
       DATE_TRUNC('month', ephem_report_date::timestamp) AS evento_mes,
       count(*)
FROM gds_ephem_integracao_view
GROUP BY evento_pais_ocorrencia, evento_estado_ocorrencia, evento_municipio_ocorrencia, evento_local_ocorrencia,
         evento_mes
ORDER BY evento_mes DESC;
```

Quantidade de ações tomadas por tipo e mês (#29):

```sql
DROP VIEW IF EXISTS total_acoes_por_tipo_view;
CREATE OR REPLACE VIEW total_acoes_por_tipo_view AS
SELECT display_name,
       DATE_TRUNC('month', create_date::timestamp) AS month,
       COUNT(*)                                    AS total_actions
FROM acao_tomada_view
WHERE signal_id <> 'false'
GROUP BY month, display_name;
```

## View Adicionais GDS

```sql
DROP VIEW IF EXISTS vw_vbe_users;
CREATE VIEW vw_vbe_users AS 
SELECT u.id,
       u.user_name AS name,
       u.email,
       date(u.birthdate) AS birthdate,
       date_part('year'::text, age(date(u.birthdate)::timestamp with time zone)) AS age,
       get_age_group(date_part('year'::text, age(date(u.birthdate)::timestamp with time zone))) AS age_group,
       date(u.created_at) AS created_at,
       u.country,
       u.state,
       u.city,
       u.gender,
       u.race,
       u.is_professional,
       u.identification_code,
       u.risk_group,
       u.is_vigilance
  FROM users u
 WHERE u.deleted_by IS NULL;
```

Engajamento usuários

Correção dos jsonbs mal formatados:

```sql
CREATE OR REPLACE VIEW public.flexible_answers_jsonb_view AS
SELECT 
    id, 
    flexible_form_version_id, 
    CASE 
        -- Verifica se o valor está em formato inválido, começando e terminando com aspas.
        WHEN data::text LIKE '"{\\"%' THEN
            -- Remove aspas externas e substitui \" por "
            (REGEXP_REPLACE(SUBSTRING(data::text FROM 2 FOR LENGTH(data::text) - 2), '\\"', '"', 'g'))::jsonb
        ELSE
            null
    END AS data_corrected,
    user_id, 
    created_at, 
    updated_at, 
    external_system_integration_id
FROM public.flexible_answers;

CREATE INDEX idx_flexible_answers_data_corrected ON public.flexible_answers USING GIN (data_corrected);

DROP VIEW IF EXISTS flexible_answers_extracted;
CREATE OR REPLACE VIEW public.flexible_answers_extracted AS
SELECT
    fa.id,
    fa.flexible_form_version_id,
    fa.user_id,
    fa.created_at,
    fa.updated_at,
    fa.external_system_integration_id,
    fa.data_corrected->>'report_type' as report_type,
    (fa.data_corrected->>'send_at')::timestamp as send_at,
    MAX(CASE WHEN ans->>'field' = 'evento_descricao' THEN ans->>'value' END) AS evento_descricao,
    MAX(CASE WHEN ans->>'field' = 'evento_qtde_envolvidos' THEN ans->>'value' END) AS evento_qtde_envolvidos,
    MAX(CASE WHEN ans->>'field' = 'evento_afetados' THEN ans->>'value' END) AS evento_afetados,
    MAX(CASE WHEN ans->>'field' = 'evento_sabe_quando_ocorreu' THEN ans->>'value' END) AS evento_sabe_quando_ocorreu,
    MAX(CASE WHEN ans->>'field' = 'evento_data_ocorrencia' THEN ans->>'value' END) AS evento_data_ocorrencia,
    MAX(CASE WHEN ans->>'field' = 'evento_estado_ocorrencia' THEN ans->>'value' END) AS evento_estado_ocorrencia,
    MAX(CASE WHEN ans->>'field' = 'evento_cidade_ocorrencia' THEN ans->>'value' END) AS evento_cidade_ocorrencia,
    MAX(CASE WHEN ans->>'field' = 'evento_local_ocorrencia' THEN ans->>'value' END) AS evento_local_ocorrencia,
    MAX(CASE WHEN ans->>'field' = 'evento_detalhes' THEN ans->>'value' END) AS evento_detalhes
FROM 
    public.flexible_answers_jsonb_view fa
LEFT JOIN LATERAL jsonb_array_elements(fa.data_corrected->'answers') as ans ON true
WHERE 
    fa.data_corrected->>'report_type' IS NOT NULL
    AND fa.data_corrected->>'report_type' != ''
GROUP BY
    fa.id,
    fa.flexible_form_version_id,
    fa.user_id,
    fa.created_at,
    fa.updated_at,
    fa.external_system_integration_id,
    fa.data_corrected->>'report_type',
    fa.data_corrected->>'send_at';

-- Detalhes sinais

DROP VIEW IF EXISTS flexible_answers_signals;
CREATE OR REPLACE VIEW public.flexible_answers_signals AS
SELECT
    fa.id,
    fa.flexible_form_version_id,
    fa.user_id,
    fa.created_at,
    fa.updated_at,
    fa.external_system_integration_id,
    fa.data_corrected->>'report_type' as report_type,
    (fa.data_corrected->>'send_at')::timestamp as send_at,
    MAX(CASE WHEN ans->>'field' = 'evento_descricao' THEN ans->>'value' END) AS evento_descricao,
    MAX(CASE WHEN ans->>'field' = 'evento_qtde_envolvidos' THEN ans->>'value' END) AS evento_qtde_envolvidos,
    MAX(CASE WHEN ans->>'field' = 'evento_afetados' THEN ans->>'value' END) AS evento_afetados,
    MAX(CASE WHEN ans->>'field' = 'evento_sabe_quando_ocorreu' THEN ans->>'value' END) AS evento_sabe_quando_ocorreu,
    MAX(CASE WHEN ans->>'field' = 'evento_data_ocorrencia' THEN ans->>'value' END) AS evento_data_ocorrencia,
    MAX(CASE WHEN ans->>'field' = 'evento_estado_ocorrencia' THEN ans->>'value' END) AS evento_estado_ocorrencia,
    MAX(CASE WHEN ans->>'field' = 'evento_cidade_ocorrencia' THEN ans->>'value' END) AS evento_cidade_ocorrencia,
    MAX(CASE WHEN ans->>'field' = 'in_training' THEN ans->>'value' END) AS in_training,
    MAX(CASE WHEN ans->>'field' = 'evento_local_ocorrencia' THEN ans->>'value' END) AS evento_local_ocorrencia,
    MAX(CASE WHEN ans->>'field' = 'evento_detalhes' THEN ans->>'value' END) AS evento_detalhes
FROM 
    public.flexible_answers_jsonb_view fa
LEFT JOIN LATERAL jsonb_array_elements(fa.data_corrected->'answers') as ans ON true
WHERE 
    fa.flexible_form_version_id = 30
    AND fa.data_corrected->>'report_type' IS NOT NULL
    AND fa.data_corrected->>'report_type' != ''
GROUP BY
    fa.id,
    fa.flexible_form_version_id,
    fa.user_id,
    fa.created_at,
    fa.updated_at,
    fa.external_system_integration_id,
    fa.data_corrected->>'report_type',
    fa.data_corrected->>'send_at';


-- Vbe Eventos Usuários vw_vbe_eventos_usuarios
DROP VIEW IF EXISTS vw_vbe_eventos_usuarios;
CREATE OR REPLACE VIEW public.vw_vbe_eventos_usuarios AS
SELECT fa.id AS evento_id,
    fa.user_id AS usuario_id,
    fa.created_at::date AS evento_data_registro,
    fa.created_at AS evento_datahora_registro,
    fa.updated_at AS evento_data_alteracao,
    fa.external_system_integration_id AS evento_ephem_id,
    fa.data_corrected ->> 'report_type'::text AS evento_tipo_reporte,
    fa.data_corrected ->> 'in_training'::text AS evento_em_treinamento,
    (fa.data_corrected ->> 'send_at'::text)::timestamp without time zone AS evento_enviado_em,
    date_part('year'::text, age(date(u.birthdate)::timestamp with time zone)) AS usuario_idade,
    get_age_group(date_part('year'::text, age(date(u.birthdate)::timestamp with time zone))) AS usuario_faixa_etaria,
    date(u.created_at) AS usuario_data_registro,
    CASE 
        WHEN u.country = 'Brazil' THEN 'Brasil'
        ELSE u.country
    END AS usuario_pais,
    u.state AS usuario_estado,
    u.city AS usuario_cidade,
    u.gender AS usuario_sexo,
    u.race AS usuario_raca,
    u.is_professional AS usuario_lider_comunitario,
    u.is_vbe AS usuario_vbe,
    u.in_training AS usuario_treinamento,
    u.deleted_by AS usuario_deletado,
    max(
        CASE
            WHEN (ans.value ->> 'field'::text) = 'evento_descricao'::text THEN ans.value ->> 'value'::text
            ELSE NULL::text
        END) AS evento_descricao,
    max(
        CASE
            WHEN (ans.value ->> 'field'::text) = 'evento_qtde_envolvidos'::text THEN ans.value ->> 'value'::text
            ELSE NULL::text
        END) AS evento_qtde_envolvidos,
    max(
        CASE
            WHEN (ans.value ->> 'field'::text) = 'evento_afetados'::text THEN ans.value ->> 'value'::text
            ELSE NULL::text
        END) AS evento_afetados,
    max(
        CASE
            WHEN (ans.value ->> 'field'::text) = 'evento_sabe_quando_ocorreu'::text THEN ans.value ->> 'value'::text
            ELSE NULL::text
        END) AS evento_sabe_quando_ocorreu,
    max(
        CASE
            WHEN (ans.value ->> 'field'::text) = 'evento_data_ocorrencia'::text THEN ans.value ->> 'value'::text
            ELSE NULL::text
        END) AS evento_data_ocorrencia,
    max(
        CASE
            WHEN (ans.value ->> 'field'::text) = 'evento_estado_ocorrencia'::text THEN ans.value ->> 'value'::text
            ELSE NULL::text
        END) AS evento_estado_ocorrencia,
    max(
        CASE
            WHEN (ans.value ->> 'field'::text) = 'evento_cidade_ocorrencia'::text THEN ans.value ->> 'value'::text
            ELSE NULL::text
        END) AS evento_cidade_ocorrencia,
    max(
        CASE
            WHEN (ans.value ->> 'field'::text) = 'evento_local_ocorrencia'::text THEN ans.value ->> 'value'::text
            ELSE NULL::text
        END) AS evento_local_ocorrencia,
    max(
        CASE
            WHEN (ans.value ->> 'field'::text) = 'evento_detalhes'::text THEN ans.value ->> 'value'::text
            ELSE NULL::text
        END) AS evento_detalhes
   FROM flexible_answers_jsonb_view fa
     LEFT JOIN LATERAL jsonb_array_elements(fa.data_corrected -> 'answers'::text) ans(value) ON true
     LEFT JOIN users u ON fa.user_id = u.id
  WHERE fa.flexible_form_version_id = 30
  GROUP BY fa.id, 
  fa.user_id, 
  (fa.created_at::date), 
  fa.created_at, fa.updated_at, 
  fa.external_system_integration_id, 
  (fa.data_corrected ->> 'report_type'::text), 
  (fa.data_corrected ->> 'in_training'::text), 
  ((fa.data_corrected ->> 'send_at'::text)::timestamp without time zone), 
  (date_part('year'::text, age(date(u.birthdate)::timestamp with time zone))), 
  (get_age_group(date_part('year'::text, age(date(u.birthdate)::timestamp with time zone)))), 
  (date(u.created_at)), 
  u.country, 
  u.state, 
  u.city, 
  u.gender, 
  u.race, 
  u.is_professional, 
  u.is_vbe,
  u.in_training, 
  u.deleted_by;

-- Detalhes Lideres

DROP VIEW IF EXISTS flexible_answers_leaders;
CREATE OR REPLACE VIEW public.flexible_answers_leaders AS
SELECT
    fa.id,
    fa.flexible_form_version_id,
    fa.user_id,
    fa.created_at,
    fa.updated_at,
    fa.external_system_integration_id,
    MAX(CASE WHEN ans->>'field' = 'perfil_lideranca' THEN ans->>'value' END) AS perfil_lideranca,
    MAX(CASE WHEN ans->>'field' = 'tempo_lideranca' THEN ans->>'value' END) AS tempo_lideranca,
    MAX(CASE WHEN ans->>'field' = 'reside_comunidade' THEN ans->>'value' END) AS reside_comunidade,
    MAX(CASE WHEN ans->>'field' = 'exerce_atividade_laboral' THEN ans->>'value' END) AS exerce_atividade_laboral,
    MAX(CASE WHEN ans->>'field' = 'nivel_escolaridade' THEN ans->>'value' END) AS nivel_escolaridade,
    MAX(CASE WHEN ans->>'field' = 'meio_residencia' THEN ans->>'value' END) AS meio_residencia,
    MAX(CASE WHEN ans->>'field' = 'possui_redes_sociais' THEN ans->>'value' END) AS possui_redes_sociais,
    MAX(CASE WHEN ans->>'field' = 'redes_sociais' THEN ans->>'value' END) AS redes_sociais
FROM 
    public.flexible_answers_jsonb_view fa
LEFT JOIN LATERAL jsonb_array_elements(fa.data_corrected->'answers') as ans ON true
WHERE 
    fa.flexible_form_version_id = 31
GROUP BY
    fa.id,
    fa.flexible_form_version_id,
    fa.user_id,
    fa.created_at,
    fa.updated_at,
    fa.external_system_integration_id;

-- Tabelao usuarios. 

DROP VIEW IF EXISTS leaders_full_data;
CREATE OR REPLACE VIEW public.leaders_full_data AS
SELECT
    fa.id AS flexible_answer_id,
    fa.flexible_form_version_id,
    fa.user_id,
    fa.created_at AS flexible_answer_created_at,
    fa.updated_at AS flexible_answer_updated_at,
    fa.external_system_integration_id,
    MAX(CASE WHEN ans->>'field' = 'perfil_lideranca' THEN ans->>'value' END) AS perfil_lideranca,
    MAX(CASE WHEN ans->>'field' = 'tempo_lideranca' THEN ans->>'value' END) AS tempo_lideranca,
    MAX(CASE WHEN ans->>'field' = 'reside_comunidade' THEN ans->>'value' END) AS reside_comunidade,
    MAX(CASE WHEN ans->>'field' = 'exerce_atividade_laboral' THEN ans->>'value' END) AS exerce_atividade_laboral,
    MAX(CASE WHEN ans->>'field' = 'nivel_escolaridade' THEN ans->>'value' END) AS nivel_escolaridade,
    MAX(CASE WHEN ans->>'field' = 'meio_residencia' THEN ans->>'value' END) AS meio_residencia,
    MAX(CASE WHEN ans->>'field' = 'possui_redes_sociais' THEN ans->>'value' END) AS possui_redes_sociais,
    MAX(CASE WHEN ans->>'field' = 'redes_sociais' THEN ans->>'value' END) AS redes_sociais,
    date_part('year'::text, age(date(u.birthdate)::timestamp with time zone)) AS age,
    get_age_group(date_part('year'::text, age(date(u.birthdate)::timestamp with time zone))) AS age_group,
    date(u.created_at) AS user_created_at,
    CASE 
        WHEN u.country = 'Brazil' THEN 'Brasil'
        ELSE u.country
    END AS country,
    u.state,
    u.city,
    u.gender,
    u.race,
    u.is_professional,
    u.identification_code,
    u.risk_group,
    u.is_vigilance,
    u.is_vbe,
    u.deleted_by
FROM 
    public.flexible_answers_jsonb_view fa
LEFT JOIN LATERAL jsonb_array_elements(fa.data_corrected->'answers') as ans ON true
LEFT JOIN users u ON fa.user_id = u.id
WHERE 
    fa.flexible_form_version_id = 31
    AND u.deleted_by IS NULL
    AND u.is_vbe = true 
    AND u.is_professional = true
GROUP BY
    fa.id,
    fa.flexible_form_version_id,
    fa.user_id,
    fa.created_at,
    fa.updated_at,
    fa.external_system_integration_id,
    u.birthdate,
    u.created_at,
    u.country,
    u.state,
    u.city,
    u.gender,
    u.race,
    u.is_professional,
    u.identification_code,
    u.risk_group,
    u.is_vigilance,
    u.is_vbe,
    u.deleted_by;
	
	
DROP VIEW IF EXISTS daily_engagement_percentage;
CREATE OR REPLACE VIEW daily_engagement_percentage AS
WITH date_range AS (
    SELECT generate_series(
        CURRENT_DATE - INTERVAL '29 days',
        CURRENT_DATE,
        '1 day'::interval
    ) AS date
),
user_daily AS (
    SELECT 
        dr.date,
        CASE 
            WHEN u.country = 'Brazil' THEN 'Brasil'
            ELSE u.country
        END AS country,
        u.state,
        u.city,
        COUNT(DISTINCT u.id) AS total_users
    FROM 
        date_range dr
    CROSS JOIN users u
    WHERE 
        u.is_vbe = true 
        AND u.is_professional = true 
        AND u.deleted_by IS NULL
        AND u.created_at <= dr.date
        AND (u.deleted_at IS NULL OR u.deleted_at > dr.date)
    GROUP BY 
        dr.date, u.country, u.state, u.city
),
user_daily_answers AS (
    SELECT 
        DATE(fa.created_at) AS date,
        CASE 
            WHEN u.country = 'Brazil' THEN 'Brasil'
            ELSE u.country
        END AS country,
        u.state,
        u.city,
        COUNT(DISTINCT fa.user_id) AS users_answered,
        COUNT(*) FILTER (WHERE fa.report_type = 'positive') AS positive_answers,
        COUNT(*) FILTER (WHERE fa.report_type = 'negative') AS negative_answers
    FROM 
        flexible_answers_extracted fa
    JOIN 
        users u ON fa.user_id = u.id
    WHERE 
        u.is_vbe = true 
        AND u.is_professional = true 
        AND u.deleted_by IS NULL
        AND fa.created_at >= CURRENT_DATE - INTERVAL '29 days'
    GROUP BY 
        DATE(fa.created_at), u.country, u.state, u.city
)
SELECT 
    ud.date,
    ud.country,
    ud.state,
    ud.city,
    ud.total_users,
    COALESCE(uda.users_answered, 0) AS users_answered,
    COALESCE(uda.positive_answers, 0) AS positive_answers,
    COALESCE(uda.negative_answers, 0) AS negative_answers,
    CASE 
        WHEN ud.total_users = 0 THEN 0
        ELSE ROUND(CAST(COALESCE(uda.users_answered, 0) AS NUMERIC) / ud.total_users, 2)
    END AS percentage_answered
FROM 
    user_daily ud
LEFT JOIN 
    user_daily_answers uda ON ud.date = uda.date 
    AND ud.country = uda.country 
    AND ud.state = uda.state 
    AND ud.city = uda.city
WHERE 
    ud.total_users > 0
ORDER BY 
    ud.date, ud.country, ud.state, ud.city;


-- Sumario do engajamento

DROP VIEW IF EXISTS daily_engagement_summary;
CREATE OR REPLACE VIEW daily_engagement_summary AS
WITH date_range AS (
    SELECT generate_series(
        CURRENT_DATE - INTERVAL '29 days',
        CURRENT_DATE,
        '1 day'::interval
    ) AS date
),
user_daily AS (
    SELECT 
        dr.date,
        CASE 
            WHEN u.country = 'Brazil' THEN 'Brasil'
            ELSE u.country
        END AS country,
        u.state,
        u.city,
        COUNT(DISTINCT u.id) AS total_users
    FROM 
        date_range dr
    CROSS JOIN users u
    WHERE 
        u.is_vbe = true 
        AND u.is_professional = true 
        AND u.deleted_by IS NULL
        AND u.created_at <= dr.date
        AND (u.deleted_at IS NULL OR u.deleted_at > dr.date)
    GROUP BY 
        dr.date, u.country, u.state, u.city
),
user_daily_answers AS (
    SELECT 
        DATE(fa.created_at) AS date,
        CASE 
            WHEN u.country = 'Brazil' THEN 'Brasil'
            ELSE u.country
        END AS country,
        u.state,
        u.city,
        COUNT(DISTINCT fa.user_id) AS total_answers
    FROM 
        flexible_answers_extracted fa
    JOIN 
        users u ON fa.user_id = u.id
    WHERE 
        u.is_vbe = true 
        AND u.is_professional = true 
        AND u.deleted_by IS NULL
        AND fa.created_at >= CURRENT_DATE - INTERVAL '29 days'
    GROUP BY 
        DATE(fa.created_at), u.country, u.state, u.city
)
SELECT 
    ud.date,
    ud.city,
    ud.state,
    ud.country,
    ud.total_users,
    COALESCE(uda.total_answers, 0) AS total_answers,
    CASE 
        WHEN ud.total_users = 0 THEN 0
        ELSE ROUND(CAST(COALESCE(uda.total_answers, 0) AS NUMERIC) / ud.total_users, 4)
    END AS total_percent
FROM 
    user_daily ud
LEFT JOIN 
    user_daily_answers uda ON ud.date = uda.date 
    AND ud.country = uda.country 
    AND ud.state = uda.state 
    AND ud.city = uda.city
ORDER BY 
    ud.date DESC, ud.country, ud.state, ud.city;
```

## Exemplo de deploy na Digital Ocean

Como exemplo de deploy na Digital Ocean, para fins de testes, pode ser utilizado o
repositorio [gds-ephem-etl-deploy](https://github.com/GleytonLima/gds-ephem-etl-deploy).

## Tecnologias utilizadas

- Java 11
- Spring Boot
- PostgreSQL

## Documentação da Api

Ao subir a aplicação localmente a documentação da API estará disponível
em: http://localhost:8081/api-etl/v1/swagger-ui/#/
