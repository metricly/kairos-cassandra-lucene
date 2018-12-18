CONSISTENCY ALL;
CREATE CUSTOM INDEX row_keys_index ON row_keys ()
USING 'com.stratio.cassandra.lucene.Index'
WITH OPTIONS = {
  'refresh_seconds': '1',
  'schema': '{
     fields: {
        metric: {type: "string"},
        row_time: {type: "long"},
        tags:   {type: "string"}
      }
    }'
  };

