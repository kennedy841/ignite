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

package org.apache.ignite.spi.discovery.zk.internal;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.curator.test.TestingCluster;
import org.apache.ignite.internal.IgniteInternalFuture;
import org.apache.ignite.internal.util.future.GridFutureAdapter;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.lang.IgniteRunnable;
import org.apache.ignite.testframework.GridTestUtils;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;

/**
 *
 */
public class ZookeeperClientTest extends GridCommonAbstractTest {
    /** */
    private TestingCluster zkCluster;

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        closeZK();

        super.afterTest();
    }

    /**
     * @throws Exception If failed.
     */
    public void testConnectionLoss1() throws Exception {
        ZookeeperClient client = new ZookeeperClient(log, "localhost:2200", 3000, null);

        try {
            client.createIfNeeded("/apacheIgnite", null, CreateMode.PERSISTENT);

            fail();
        }
        catch (ZookeeperClientFailedException e) {
            info("Expected error: " + e);
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testConnectionLoss2() throws Exception {
        startZK(1);

        ZookeeperClient client = new ZookeeperClient(log, zkCluster.getConnectString(), 3000, null);

        client.createIfNeeded("/apacheIgnite1", null, CreateMode.PERSISTENT);

        closeZK();

        try {
            client.createIfNeeded("/apacheIgnite2", null, CreateMode.PERSISTENT);

            fail();
        }
        catch (ZookeeperClientFailedException e) {
            info("Expected error: " + e);
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testConnectionLoss3() throws Exception {
        startZK(1);

        CallbackFuture cb = new CallbackFuture();

        ZookeeperClient client = new ZookeeperClient(log, zkCluster.getConnectString(), 3000, cb);

        client.createIfNeeded("/apacheIgnite1", null, CreateMode.PERSISTENT);

        closeZK();

        final AtomicBoolean res = new AtomicBoolean();

        client.getChildrenAsync("/apacheIgnite1", false, new AsyncCallback.Children2Callback() {
            @Override public void processResult(int rc, String path, Object ctx, List<String> children, Stat stat) {
                if (rc == 0)
                    res.set(true);
            }
        }, null);

        cb.get(10_000);

        assertFalse(res.get());
    }

    /**
     * @throws Exception If failed.
     */
    public void testReconnect1() throws Exception {
        startZK(1);

        ZookeeperClient client = new ZookeeperClient(log, zkCluster.getConnectString(), 30_000, null);

        client.createIfNeeded("/apacheIgnite1", null, CreateMode.PERSISTENT);

        zkCluster.getServers().get(0).stop();

        IgniteInternalFuture fut = GridTestUtils.runAsync(new Callable<Void>() {
            @Override public Void call() throws Exception {
                U.sleep(2000);

                info("Restart zookeeper server");

                zkCluster.getServers().get(0).restart();

                info("Zookeeper server restarted");

                return null;
            }
        }, "start-zk");

        client.createIfNeeded("/apacheIgnite2", null, CreateMode.PERSISTENT);

        fut.get();
    }

    /**
     * @throws Exception If failed.
     */
    public void testReconnect2() throws Exception {
        startZK(1);

        ZookeeperClient client = new ZookeeperClient(log, zkCluster.getConnectString(), 30_000, null);

        client.createIfNeeded("/apacheIgnite1", null, CreateMode.PERSISTENT);

        zkCluster.getServers().get(0).restart();

        client.createIfNeeded("/apacheIgnite2", null, CreateMode.PERSISTENT);
    }

    /**
     * @throws Exception If failed.
     */
    public void testReconnect3() throws Exception {
        startZK(3);

        ZookeeperClient client = new ZookeeperClient(log, zkCluster.getConnectString(), 30_000, null);

        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        for (int i = 0; i < 30; i++) {
            int idx = rnd.nextInt(3);

            zkCluster.getServers().get(idx).restart();

            doSleep(rnd.nextLong(100) + 1);

            client.createIfNeeded("/apacheIgnite" + i, null, CreateMode.PERSISTENT);
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testReconnect4() throws Exception {
        startZK(3);

        ZookeeperClient client = new ZookeeperClient(log,
            zkCluster.getServers().get(2).getInstanceSpec().getConnectString(),
            30_000,
            null);

        client.createIfNeeded("/apacheIgnite1", null, CreateMode.PERSISTENT);

        zkCluster.getServers().get(0).stop();
        zkCluster.getServers().get(1).stop();

        IgniteInternalFuture fut = GridTestUtils.runAsync(new Callable<Void>() {
            @Override public Void call() throws Exception {
                U.sleep(2000);

                info("Restart zookeeper server");

                zkCluster.getServers().get(0).restart();

                info("Zookeeper server restarted");

                return null;
            }
        }, "start-zk");

        client.createIfNeeded("/apacheIgnite2", null, CreateMode.PERSISTENT);

        fut.get();
    }

    /**
     * @param instances Number of servers in ZK ensemble.
     * @throws Exception If failed.
     */
    private void startZK(int instances) throws Exception {
        assert zkCluster == null;

        zkCluster = new TestingCluster(instances);

        zkCluster.start();
    }

    /**
     *
     */
    private void closeZK() {
        if (zkCluster != null) {
            try {
                zkCluster.close();
            }
            catch (Exception e) {
                U.error(log, "Failed to stop Zookeeper client: " + e, e);
            }

            zkCluster = null;
        }
    }

    /**
     *
     */
    private static class CallbackFuture extends GridFutureAdapter<Void> implements IgniteRunnable {
        /** {@inheritDoc} */
        @Override public void run() {
            onDone();
        }
    }
}