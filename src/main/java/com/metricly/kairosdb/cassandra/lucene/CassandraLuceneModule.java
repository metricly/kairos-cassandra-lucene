package com.metricly.kairosdb.cassandra.lucene;


import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CassandraLuceneModule extends AbstractModule {

    public static final Logger logger = LoggerFactory.getLogger(CassandraLuceneModule.class);

    @Override
    protected void configure() {
        System.out.println("Loading CassandraLuceneModule");
        logger.info("Loading CassandraLuceneModule");

        bind(CassandraLuceneIndexQueryPlugin.class).in(Singleton.class);
    }
}
