package com.englishtown.vertx.cassandra.impl;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy;
import com.datastax.driver.core.policies.LoadBalancingPolicy;
import com.englishtown.vertx.cassandra.CassandraConfigurator;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Json configuration based implementation of {@link com.englishtown.vertx.cassandra.CassandraConfigurator}
 */
public class JsonCassandraConfigurator implements CassandraConfigurator {

    private List<String> seeds;
    private LoadBalancingPolicy loadBalancingPolicy;
    private ConsistencyLevel consistency;

    public static final String CONFIG_SEEDS = "seeds";
    public static final String CONFIG_CONSISTENCY_LEVEL = "consistency_level";

    public static final String CONSISTENCY_ANY = "ANY";
    public static final String CONSISTENCY_ONE = "ONE";
    public static final String CONSISTENCY_TWO = "TWO";
    public static final String CONSISTENCY_THREE = "THREE";
    public static final String CONSISTENCY_QUORUM = "QUORUM";
    public static final String CONSISTENCY_ALL = "ALL";
    public static final String CONSISTENCY_LOCAL_QUORUM = "LOCAL_QUORUM";
    public static final String CONSISTENCY_EACH_QUORUM = "EACH_QUORUM";

    @Inject
    public JsonCassandraConfigurator(Container container) {
        this(container.config().getObject("cassandra", new JsonObject()));
    }

    public JsonCassandraConfigurator(JsonObject config) {
        init(config);
    }

    @Override
    public List<String> getSeeds() {
        return seeds;
    }

    @Override
    public LoadBalancingPolicy getLoadBalancingPolicy() {
        return loadBalancingPolicy;
    }

    @Override
    public ConsistencyLevel getConsistency() {
        return consistency;
    }

    protected void init(JsonObject config) {

        // Get array of IPs, default to localhost
        JsonArray seeds = config.getArray(CONFIG_SEEDS);
        if (seeds == null || seeds.size() == 0) {
            seeds = new JsonArray().addString("127.0.0.1");
        }

        this.seeds = new ArrayList<>();
        for (int i = 0; i < seeds.size(); i++) {
            this.seeds.add(seeds.<String>get(i));
        }

        initPolicies(config);
        consistency = getConsistency(config);

    }

    protected void initPolicies(JsonObject config) {

        JsonObject policyConfig = config.getObject("policies");

        if (policyConfig == null) {
            return;
        }

        JsonObject loadBalancing = policyConfig.getObject("load_balancing");
        if (loadBalancing != null) {
            String name = loadBalancing.getString("name");

            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("A load balancing policy must have a class name field");

            } else if ("DCAwareRoundRobinPolicy".equalsIgnoreCase(name)
                    || "com.datastax.driver.core.policies.DCAwareRoundRobinPolicy".equalsIgnoreCase(name)) {

                String localDc = loadBalancing.getString("local_dc");
                int usedHostsPerRemoteDc = loadBalancing.getInteger("used_hosts_per_remote_dc", 0);

                if (localDc == null || localDc.isEmpty()) {
                    throw new IllegalArgumentException("A DCAwareRoundRobinPolicy requires a local_dc in configuration.");
                }

                loadBalancingPolicy = new DCAwareRoundRobinPolicy(localDc, usedHostsPerRemoteDc);

            } else {

                Class<?> clazz;
                try {
                    clazz = Thread.currentThread().getContextClassLoader().loadClass(name);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
                if (LoadBalancingPolicy.class.isAssignableFrom(clazz)) {
                    try {
                        loadBalancingPolicy = (LoadBalancingPolicy) clazz.newInstance();
                    } catch (IllegalAccessException | InstantiationException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    throw new IllegalArgumentException("Class " + name + " does not implement LoadBalancingPolicy");
                }

            }
        }
    }

    protected ConsistencyLevel getConsistency(JsonObject config) {
        String consistency = config.getString(CONFIG_CONSISTENCY_LEVEL);

        if (consistency == null || consistency.isEmpty()) {
            return null;
        }

        if (consistency.equalsIgnoreCase(CONSISTENCY_ANY)) {
            return ConsistencyLevel.ANY;
        }
        if (consistency.equalsIgnoreCase(CONSISTENCY_ONE)) {
            return ConsistencyLevel.ONE;
        }
        if (consistency.equalsIgnoreCase(CONSISTENCY_TWO)) {
            return ConsistencyLevel.TWO;
        }
        if (consistency.equalsIgnoreCase(CONSISTENCY_THREE)) {
            return ConsistencyLevel.THREE;
        }
        if (consistency.equalsIgnoreCase(CONSISTENCY_QUORUM)) {
            return ConsistencyLevel.QUORUM;
        }
        if (consistency.equalsIgnoreCase(CONSISTENCY_ALL)) {
            return ConsistencyLevel.ALL;
        }
        if (consistency.equalsIgnoreCase(CONSISTENCY_LOCAL_QUORUM)) {
            return ConsistencyLevel.LOCAL_QUORUM;
        }
        if (consistency.equalsIgnoreCase(CONSISTENCY_EACH_QUORUM)) {
            return ConsistencyLevel.EACH_QUORUM;
        }

        throw new IllegalArgumentException("'" + consistency + "' is not a valid consistency level.");
    }

}