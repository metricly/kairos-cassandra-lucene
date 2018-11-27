kairos-cassandra-lucene
=====================
The kairos-cassandra-lucene plugin is meant to accelerate and enhance the row_keys index lookup for metrics with a large number of unique tag combinations. 
This plugin effectively pushes the filtering of the row_keys by tag onto the cassandra nodes by the use of a secondary index `cassandra-lucene-index` 
https://github.com/instaclustr/cassandra-lucene-index 

This plugin adds support for a metric QueryPlugin that will use a `contains` search https://github.com/instaclustr/cassandra-lucene-index/blob/3.11.3/doc/documentation.rst#contains-search 
when doing the query against the row_keys table. 


Using the Cassandra Lucene Plugin
----------------------
First, you must ensure the plugin is installed into `<kairosdb-installdir>/lib`

On your cassandra nodes, you must have the `cassandra-lucene-index` plugin installed and `row_keys_index` index created from `src/main/resources/queries.cql`

Then, when querying kairosdb, add the following plugin to your query (per metric): 

`"plugins": [{"name": "cassandra-lucene"}]`

And that's it, if you add that plugin to your query, kairosdb will execute the custom query path that will add the `contains` search expr to the query.


Todo Items:
-----------
* the index name `row_key_index` is hard coded 
* schema and refresh rate are hard coded 
* the keys are accumulated in memory and then returned as an Iterator, this could be improved to stream the results through the iterator 
as they become available.  This is the behavior found in the default CQLFilteredRowKeyIterator.


