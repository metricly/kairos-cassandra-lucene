# Environment
To use: 
 - `docker-compose up -d --build`
 
# Tests and test environment for this plugin

The environment consists of: 
    * 3 node cassandra cluster v3.11.3
    * includes cassandra-lucene plugin needed for this Kairos plugin 
    * 1 node kairosdb service
        * includes the kairos-cassandra-lucene plugin
        * includes a year’s worth of demo data
        * ports are exposed for remote debugging
        * Note: this should be a locally built image of yours that you are dev’ing against
    * kairosdbindex service which creates the lucene index
    * kairosdbindex-tests service which runs some tests against the environment
 
## Step debugging with IntelliJ

    * setup a remote connection to your kairosdb instance running in docker (see testsAT for environment to setup) 
    * remote debug port is 4000