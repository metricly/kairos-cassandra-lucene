-- original query on row_key_time_index
  select row_time
  from row_key_time_index
  where metric = 'demo_data'
  and table_name = 'data_points'
  and row_time >= '2018-11-01 00:00:00'
  and row_time <= '2018-11-16 00:00:00' ;

-- original query on row_keys
query row_keys
  select *
  from row_keys
  where metric = 'demo_data'
  and table_name = 'data_points'
  and row_time = '2018-11-15 00:00:00';

-- lucene index
CREATE CUSTOM INDEX row_keys_index ON row_keys ()
USING 'com.stratio.cassandra.lucene.Index'
WITH OPTIONS = {
  'refresh_seconds': '30',
  'schema': '{
     fields: {
        metric: {type: "string"},
        row_time: {type: "long"},
        tags:   {type: "string"}
      }
    }'
  };

-- prefer filters over queries since queries hit every node for relevance
-- https://github.com/Stratio/cassandra-lucene-index/blob/branch-3.0.7/doc/resources/extended-search-examples.cql
--
-- examples
--
-- single elementId match
select * from row_keys where expr(row_keys_index, '{
  filter: {
     type: "match",
     field: "tags$elementId",
     value: "12345-67890-xxxxx-aaaaa"
  }
}');

-- same key, multiple values match
select * from row_keys where expr(row_keys_index, '{
  filter: {
     type: "contains",
     field: "tags$elementId",
     values: ["12345-67890-xxxxx-aaaaa", "abc123"]
  }
}');

-- multiple tag key/value matches
select * from row_keys where expr(row_keys_index, '{
  filter: {
     type: "match",
     field: "tags$host",
     value: "4f34fa2d4def"
  },
  filter: {
     type: "match",
     field: "tags$table",
     value: "data_points"
  }
}');


-- curl queries for testing
--  no tags
curl 'http://localhost:8080/api/v1/datapoints/query' -H 'Content-Type: application/json' --data-binary '{"metrics":[{"plugins": [{"name": "cassandra-lucene"}], "tags":{},"name":"demo_data","aggregators":[{"name":"sum","sampling":{"value":"1","unit":"milliseconds"}}]}],"plugins":[],"cache_time":0,"start_relative":{"value":"1","unit":"hours"}}' --compressed

-- with tags
curl 'http://localhost:8080/api/v1/datapoints/query' -H 'Content-Type: application/json' --data-binary '{"metrics":[{"plugins": [{"name": "cassandra-lucene"}], "tags":{"host":["demo_server_0"]},"name":"demo_data","aggregators":[{"name":"sum","sampling":{"value":"1","unit":"milliseconds"}}]}],"plugins":[],"cache_time":0,"start_relative":{"value":"1","unit":"hours"}}' --compressed


