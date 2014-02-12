package com.englishtown.vertx.cassandra;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.policies.LoadBalancingPolicy;

import java.util.List;

/**
 * Provides cassandra configuration for the session
 */
public interface CassandraConfigurator {

    List<String> getSeeds();

    LoadBalancingPolicy getLoadBalancingPolicy();

    ConsistencyLevel getConsistency();

}