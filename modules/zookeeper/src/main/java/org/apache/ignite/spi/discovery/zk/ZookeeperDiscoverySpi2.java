/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.spi.discovery.zk;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.ignite.IgniteException;
import org.apache.ignite.IgniteLogger;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.internal.managers.discovery.JoiningNodesAware;
import org.apache.ignite.internal.util.tostring.GridToStringInclude;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.lang.IgniteProductVersion;
import org.apache.ignite.marshaller.jdk.JdkMarshaller;
import org.apache.ignite.resources.LoggerResource;
import org.apache.ignite.spi.IgniteSpiAdapter;
import org.apache.ignite.spi.IgniteSpiException;
import org.apache.ignite.spi.IgniteSpiMultipleInstancesSupport;
import org.apache.ignite.spi.discovery.DiscoveryDataBag;
import org.apache.ignite.spi.discovery.DiscoveryMetricsProvider;
import org.apache.ignite.spi.discovery.DiscoverySpi;
import org.apache.ignite.spi.discovery.DiscoverySpiCustomMessage;
import org.apache.ignite.spi.discovery.DiscoverySpiDataExchange;
import org.apache.ignite.spi.discovery.DiscoverySpiHistorySupport;
import org.apache.ignite.spi.discovery.DiscoverySpiListener;
import org.apache.ignite.spi.discovery.DiscoverySpiNodeAuthenticator;
import org.apache.ignite.spi.discovery.DiscoverySpiOrderSupport;
import org.apache.ignite.spi.discovery.zk.internal.ZkDiscoveryImpl;
import org.apache.ignite.spi.discovery.zk.internal.ZookeeperClient;
import org.apache.ignite.spi.discovery.zk.internal.ZookeeperClusterNode;
import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.data.Stat;
import org.jetbrains.annotations.Nullable;

/**
 *
 */
@IgniteSpiMultipleInstancesSupport(true)
@DiscoverySpiOrderSupport(true)
@DiscoverySpiHistorySupport(true)
public class ZookeeperDiscoverySpi2 extends IgniteSpiAdapter implements DiscoverySpi, JoiningNodesAware {
    /** */
    private String connectString;

    /** */
    private DiscoverySpiListener lsnr;

    /** */
    private DiscoverySpiDataExchange exchange;

    /** */
    private DiscoveryMetricsProvider metricsProvider;

    /** */
    private int sesTimeout = 5000;

    /** */
    private ZkDiscoveryImpl impl;

    /** */
    private ZookeeperClusterNode locNode;

    /** */
    private Map<String, Object> locNodeAttrs;

    /** */
    private IgniteProductVersion locNodeVer;

    /** */
    private long gridStartTime;

    /** */
    @LoggerResource
    private IgniteLogger log;

    public int getSessionTimeout() {
        return sesTimeout;
    }

    public void setSessionTimeout(int sesTimeout) {
        this.sesTimeout = sesTimeout;
    }

    public String getConnectString() {
        return connectString;
    }

    public void setConnectString(String connectString) {
        this.connectString = connectString;
    }

    /** */
    private Serializable consistentId;

    /** {@inheritDoc} */
    @Override public boolean knownNode(UUID nodeId) {
//        try {
//            for (String child : zkCurator.getChildren().forPath(ALIVE_NODES_PATH)) {
//                ZKNodeData nodeData = parseNodePath(child);
//
//                if (nodeData.nodeId.equals(nodeId))
//                    return true;
//            }
//
//            return false;
//        }
//        catch (Exception e) {
//            U.error(log, "Failed to read alive nodes: " + e, e);
//
//            return false;
//        }

        return false;
    }

    /** {@inheritDoc} */
    @Nullable @Override public Serializable consistentId() throws IgniteSpiException {
        if (consistentId == null)
            consistentId = ignite.configuration().getConsistentId();

        return consistentId;
    }

    /** {@inheritDoc} */
    @Override public Collection<ClusterNode> getRemoteNodes() {
        // TODO ZK
        List<ClusterNode> nodes;

        synchronized (curTop) {
            nodes = new ArrayList<>((Collection)curTop.values());

            for (ClusterNode node : curTop.values()) {
                if (!locNode.id().equals(node.id()))
                    nodes.add(node);
            }
        }

        return nodes;
    }

    /** {@inheritDoc} */
    @Override public ClusterNode getLocalNode() {
        return locNode;
    }

    /** {@inheritDoc} */
    @Nullable @Override public ClusterNode getNode(UUID nodeId) {
        // TODO ZK 
        synchronized (curTop) {
            for (ClusterNode node : curTop.values()) {
                if (node.id().equals(nodeId))
                    return node;
            }
        }

        return null;
    }

    /** {@inheritDoc} */
    @Override public boolean pingNode(UUID nodeId) {
        // TODO ZK
        return getNode(nodeId) != null;
    }

    /** {@inheritDoc} */
    @Override public void setNodeAttributes(Map<String, Object> attrs, IgniteProductVersion ver) {
        assert locNodeAttrs == null;
        assert locNodeVer == null;

        if (log.isDebugEnabled()) {
            log.debug("Node attributes to set: " + attrs);
            log.debug("Node version to set: " + ver);
        }

        locNodeAttrs = attrs;
        locNodeVer = ver;
    }

    /** {@inheritDoc} */
    @Override public void setListener(@Nullable DiscoverySpiListener lsnr) {
        this.lsnr = lsnr;
    }

    /** {@inheritDoc} */
    @Override public void setDataExchange(DiscoverySpiDataExchange exchange) {
        this.exchange = exchange;
    }

    /** {@inheritDoc} */
    @Override public void setMetricsProvider(DiscoveryMetricsProvider metricsProvider) {
        this.metricsProvider = metricsProvider;
    }

    /** {@inheritDoc} */
    @Override public void disconnect() throws IgniteSpiException {
        // TODO ZK
    }

    /** {@inheritDoc} */
    @Override public void setAuthenticator(DiscoverySpiNodeAuthenticator auth) {
        // TODO ZK
    }

    /** {@inheritDoc} */
    @Override public long getGridStartTime() {
        return gridStartTime;
    }

    /** {@inheritDoc} */
    @Override public void sendCustomEvent(DiscoverySpiCustomMessage msg) throws IgniteException {
        // TODO ZK
//        try {
//            zkCurator.create().withMode(CreateMode.PERSISTENT_SEQUENTIAL).forPath(CUSTOM_EVTS_PATH, marshal(msg));
//        }
//        catch (Exception e) {
//            throw new IgniteSpiException(e);
//        }
    }

    /** {@inheritDoc} */
    @Override public void failNode(UUID nodeId, @Nullable String warning) {
        // TODO ZK
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override public boolean isClientMode() throws IllegalStateException {
        return locNode.isClient();
    }

    /**
     *
     */
    private void initLocalNode() {
        assert ignite != null;

        Serializable consistentId = consistentId();

        UUID nodeId = ignite.configuration().getNodeId();

        // TODO ZK
        if (consistentId == null)
            consistentId = nodeId;

        locNode = new ZookeeperClusterNode(nodeId,
            locNodeVer,
            locNodeAttrs,
            consistentId,
            ignite.configuration().isClientMode());

        locNode.local(true);

        DiscoverySpiListener lsnr = this.lsnr;

        if (lsnr != null)
            lsnr.onLocalNodeInitialized(locNode);

        if (log.isDebugEnabled())
            log.debug("Local node initialized: " + locNode);
    }

    private boolean igniteClusterStarted() throws Exception {
        return false;
//        return zkCurator.checkExists().forPath(IGNITE_PATH) != null &&
//            zkCurator.checkExists().forPath(ALIVE_NODES_PATH) != null &&
//            !zk.getChildren(ALIVE_NODES_PATH, false).isEmpty();
    }

    private void startConnect(DiscoveryDataBag discoDataBag) throws Exception {
//        ZKClusterData clusterData = unmarshal(zk.getData(CLUSTER_PATH, false, null));
//
//        gridStartTime = clusterData.gridStartTime;
//
//        zk.getData(EVENTS_PATH, zkWatcher, dataUpdateCallback, null);
//        //zk.getChildren(JOIN_HIST_PATH, zkWatcher, zkChildrenUpdateCallback, null);
//        zk.getChildren(ALIVE_NODES_PATH, zkWatcher, zkChildrenUpdateCallback, null);
//
//        ZKJoiningNodeData joinData = new ZKJoiningNodeData(locNode, discoDataBag.joiningNodeData());
//
//        byte[] nodeData = marshal(joinData);
//
//        String zkNode = "/" + locNode.id().toString() + "-";
//
//        zkCurator.inTransaction().
//                create().withMode(CreateMode.PERSISTENT_SEQUENTIAL).withACL(ZooDefs.Ids.OPEN_ACL_UNSAFE).forPath(JOIN_HIST_PATH + zkNode, nodeData).
//                and().
//                create().withMode(CreateMode.EPHEMERAL_SEQUENTIAL).withACL(ZooDefs.Ids.OPEN_ACL_UNSAFE).forPath(ALIVE_NODES_PATH + zkNode).
//                and().commit();
//
//        connectStart.countDown();
    }

    /** {@inheritDoc} */
    @Override public void spiStart(@Nullable String igniteInstanceName) throws IgniteSpiException {
        initLocalNode();

        DiscoveryDataBag discoDataBag = new DiscoveryDataBag(locNode.id());

        exchange.collect(discoDataBag);

        impl = new ZkDiscoveryImpl(log, lsnr);

        try {
            impl.joinTopology(igniteInstanceName, connectString, sesTimeout);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new IgniteSpiException("Failed to join cluster, thread was interrupted", e);
        }
    }

    /**
     * For testing only.
     *
     * @throws Exception If failed.
     */
    public void waitConnectStart() throws Exception {
        //connectStart.await();
    }

    /** {@inheritDoc} */
    @Override public void spiStop() throws IgniteSpiException {
        if (impl != null)
            impl.stop();
    }

    /** */
    private static final int ID_LEN = 36;

    /**
     *
     */
    static class ZKNodeData implements Serializable {
        /** */
        @GridToStringInclude
        final long order;

        /** */
        @GridToStringInclude
        final UUID nodeId;

        /** */
        //transient ZKJoiningNodeData joinData;

        /**
         * @param order Node order.
         * @param nodeId Node ID.
         */
        ZKNodeData(long order, UUID nodeId) {
            this.order = order;
            this.nodeId = nodeId;
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(ZKNodeData.class, this);
        }
    }

    /**
     *
     */
    static class ZKAliveNodes implements Serializable {
        /** */
        @GridToStringInclude
        final int ver;

        /** */
        @GridToStringInclude
        final TreeMap<Long, ZKNodeData> nodesByOrder;

        /**
         * @param ver
         * @param nodesByOrder
         */
        ZKAliveNodes(int ver, TreeMap<Long, ZKNodeData> nodesByOrder) {
            this.ver = ver;
            this.nodesByOrder = nodesByOrder;
        }

        ZKNodeData nodeById(UUID nodeId) {
            for (ZKNodeData nodeData : nodesByOrder.values()) {
                if (nodeId.equals(nodeData.nodeId))
                    return nodeData;
            }

            return null;
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(ZKAliveNodes.class, this);
        }
    }

    /**
     * @param path Zookeeper node path.
     * @return Ignite node data.
     */
    private static ZKNodeData parseNodePath(String path) {
        String idStr = path.substring(0, ID_LEN);

        UUID nodeId = UUID.fromString(idStr);

        int nodeOrder = Integer.parseInt(path.substring(ID_LEN + 1)) + 1;

        return new ZKNodeData(nodeOrder, nodeId);
    }

    /**
     * For testing only.
     */
    public void closeClient() {
    }

    /** */
    private final TreeMap<Long, ZookeeperClusterNode> curTop = new TreeMap<>();
}