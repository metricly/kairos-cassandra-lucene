package com.metricly.kairosdb.cassandra.lucene;

import com.datastax.driver.core.*;
import com.google.common.collect.SetMultimap;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import org.kairosdb.core.annotation.PluginName;
import org.kairosdb.core.datastore.DatastoreMetricQuery;
import org.kairosdb.core.datastore.QueryPlugin;
import org.kairosdb.datastore.cassandra.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Allow the use of a secondary index `row_keys_index` that's backed by
 * cassandra-lucene-index plugin.  This plugin assume you have setup the row_keys_index
 * index. See src/main/resources/queries.cql
 *
 * It aims to improve the query time of metrics with many tag combinations by pushing the filtering
 * onto the cassandra nodes and allowing for powerful filtering via lucene.
 *
 */
@PluginName(name = "cassandra-lucene", description = "Cassandra Lucene Search Plugin")
public class CassandraLuceneIndexQueryPlugin implements QueryPlugin, CassandraRowKeyPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(CassandraLuceneIndexQueryPlugin.class);

    private Schema m_schema;
    private CassandraConfiguration m_cassandraConfiguration;
    private Session m_session;

    @Inject
    CassandraLuceneIndexQueryPlugin(
            Schema schema,
            Session session,
            CassandraConfiguration cassandraConfiguration) {

        this.m_schema = schema;
        this.m_session = session;
        this.m_cassandraConfiguration = cassandraConfiguration;
    }

    @Override
    public String getName() {
        return "cassandra-lucene";
    }

    @Override
    public Iterator<DataPointsRowKey> getKeysForQueryIterator(DatastoreMetricQuery query) {

        ListenableFuture<List<ResultSet>> listListenableFuture = Futures.allAsList(getResultSetFutures(query));
        List<DataPointsRowKey> rowKeys = new ArrayList<>();
        try {
            Iterator<ResultSet> m_resultSets = listListenableFuture.get().iterator();
            while(m_resultSets.hasNext()) {

                ResultSet next = m_resultSets.next();
                Row one = next.one();
                if(one == null) {
                    // empty results
                    break;
                }
                DataPointsRowKey dataPointsRowKey = new DataPointsRowKey(query.getName(),
                        one.getTimestamp(0).getTime(),
                        one.getString(1),
                        new TreeMap<>(one.getMap(2, String.class, String.class)));

                rowKeys.add(dataPointsRowKey);
            }
        } catch (InterruptedException e) {
            LOG.error("row_keys_index query interrupted", e) ;
        } catch (ExecutionException e) {
            LOG.error("failed reading row_keys index ", e);
        }

        return rowKeys.iterator();
    }

    private List<ResultSetFuture> getResultSetFutures(DatastoreMetricQuery query) {
        List<ResultSetFuture> resultSetFutures = new ArrayList<>();
        String cql = m_schema.ROW_KEY_QUERY;

        List<Long> queryKeyList = createQueryKeyList(query.getName(), query.getStartTime(), query.getEndTime());
        for (Long keyTime : queryKeyList) {

            boolean hasTags = query.getTags().size() > 0;
            Object[] params = new Object[]{query.getName(), new Date(keyTime)};
            if(hasTags) {
                cql += " AND expr(row_keys_index, '{ " + getFilters(query.getTags()) + " }')";
            }

            SimpleStatement statement = new SimpleStatement(cql, params);
            statement.setConsistencyLevel(m_cassandraConfiguration.getDataReadLevel());
            ResultSetFuture future = m_session.executeAsync(statement);
            resultSetFutures.add(future);
        }
        return resultSetFutures;
    }

    /**
     * Given a set of tags, convert to a cassandra-lucene set of filter{}
     * objects to query the row_key_index with.
     *
     * For example, a kairosdb query with a tags section defined, such as:
     *
     * "tags":{"host":["demo_server_0","demo_server_1"]}
     *
     * this method would generate the following filter query:
     *
     * filter : {
     *      type: "contains",
     *      field: "tags$host",
     *      values: ["demo_server_0", "demo_server_1"]
     * }
     * ...
     *
     * @param tags
     * @return
     */
    private String getFilters(SetMultimap<String, String> tags) {


        Map<String, Collection<String>> groupedByKey = tags.asMap();
        List<String> filters = new ArrayList<>();

        for (String key : groupedByKey.keySet()) {

            Collection<String> values = groupedByKey.get(key);
            if(null == values ||
                    values.size() == 0) {
                throw new IllegalStateException("tags must have a key and value");
            }

            StringBuilder containsFilter = new StringBuilder();
            containsFilter.append("filter: {");
            containsFilter.append("   type: \"contains\",");
            containsFilter.append("   field: \"tags$" + key + "\",");
            containsFilter.append("   values: ");
                        containsFilter.append("[");
                        containsFilter.append(
                            groupedByKey.get(key).stream()
                                .map(i -> "\"" + i + "\"")
                                .collect(Collectors.joining(", ")).toString()
                        );
                        containsFilter.append("]");
            containsFilter.append("}");
            filters.add(containsFilter.toString());
        }


        return filters.stream().collect(Collectors.joining(", "));
    }

    /**
     * Query row_key_time_index for row_keys matching the startTime, endTime and metricName
     *
     * @param metricName
     * @param startTime
     * @param endTime
     * @return
     */
    private List<Long> createQueryKeyList(String metricName, long startTime, long endTime) {

        List<Long> ret = new ArrayList<>();

        BoundStatement statement = new BoundStatement(m_schema.psRowKeyTimeQuery);
        statement.setString(0, metricName);
        statement.setTimestamp(1, new Date(CassandraDatastore.calculateRowTime(startTime)));
        statement.setTimestamp(2, new Date(endTime));
        statement.setConsistencyLevel(m_cassandraConfiguration.getDataReadLevel());

        ResultSet rows = m_session.execute(statement);

        while (!rows.isExhausted())
        {
            ret.add(rows.one().getTimestamp(0).getTime());
        }

        return ret;
    }

}