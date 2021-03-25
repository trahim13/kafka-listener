кафка порт по-умолчанию: 9094
постгрес: 15432

Смотри: java/docker/docker-compose.yml


таблица
`create table if not exists kafaka
(
	id uuid not null
		constraint kafaka_pk
			primary key,
	topic varchar(255) not null,
	is_start boolean not null
);

alter table kafaka owner to postgres;

create unique index if not exists kafaka_id_uindex
	on kafaka (id);

create unique index if not exists kafaka_topic_uindex
	on kafaka (topic);`
	
данные:
`INSERT INTO public.kafaka (id, topic, is_start) VALUES ('7fb4203c-911b-41a3-b2dc-d703cf63a9a1', 'src', false);
INSERT INTO public.kafaka (id, topic, is_start) VALUES ('faa8eaf8-578c-41ce-9a93-faa79bc20d54', 'out', false);	`

запуск-остановка src
`curl --location --request GET 'http://localhost:8082/kafka-service/stop/src'
curl --location --request GET 'http://localhost:8082/kafka-service/start/src'`


запуск-остановка out
`curl --location --request GET 'http://localhost:8082/kafka-service/stop/out'
curl --location --request GET 'http://localhost:8082/kafka-service/start/out'`


остановить всех
`curl --location --request GET 'http://localhost:8082/kafka-service/stop-all'`


создать слушателей
`curl --location --request POST 'http://localhost:8082/kafka-service/register' \
--header 'Content-Type: application/json' \
--data-raw '{
    "topics": ["src1", "out1"]
}'`