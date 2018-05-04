/*
 * Copyright 2018 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.resolver.dns;

import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.NetUtil;
import org.junit.Test;

import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class DefaultDnsCacheTest {

    @Test
    public void testExpire() throws Throwable {
        InetAddress addr1 = InetAddress.getByAddress(new byte[] { 10, 0, 0, 1 });
        InetAddress addr2 = InetAddress.getByAddress(new byte[] { 10, 0, 0, 2 });
        EventLoopGroup group = new DefaultEventLoopGroup(1);

        try {
            EventLoop loop = group.next();
            final DefaultDnsCache cache = new DefaultDnsCache();
            cache.cache("netty.io", null, addr1, 1, loop);
            cache.cache("netty.io", null, addr2, 10000, loop);

            Throwable error = loop.schedule(new Callable<Throwable>() {
                @Override
                public Throwable call() {
                    try {
                        assertNull(cache.get("netty.io", null));
                        return null;
                    } catch (Throwable cause) {
                        return cause;
                    }
                }
            }, 1, TimeUnit.SECONDS).get();
            if (error != null) {
                throw error;
            }
        } finally {
            group.shutdownGracefully();
        }
    }

    @Test
    public void testExpireWithDifferentTTLs() {
        testExpireWithTTL0(1);
        testExpireWithTTL0(1000);
        testExpireWithTTL0(1000000);
    }

    private static void testExpireWithTTL0(int days) {
        EventLoopGroup group = new NioEventLoopGroup(1);

        try {
            EventLoop loop = group.next();
            final DefaultDnsCache cache = new DefaultDnsCache();
            assertNotNull(cache.cache("netty.io", null, NetUtil.LOCALHOST, days, loop));
        } finally {
            group.shutdownGracefully();
        }
    }

    @Test
    public void testAddMultipleAddressesForSameHostname() throws Exception {
        InetAddress addr1 = InetAddress.getByAddress(new byte[] { 10, 0, 0, 1 });
        InetAddress addr2 = InetAddress.getByAddress(new byte[] { 10, 0, 0, 2 });
        EventLoopGroup group = new DefaultEventLoopGroup(1);

        try {
            EventLoop loop = group.next();
            final DefaultDnsCache cache = new DefaultDnsCache();
            cache.cache("netty.io", null, addr1, 1, loop);
            cache.cache("netty.io", null, addr2, 10000, loop);

            List<? extends DnsCacheEntry> entries = cache.get("netty.io", null);
            assertEquals(2, entries.size());
            assertEntry(entries.get(0), addr1);
            assertEntry(entries.get(1), addr2);
        } finally {
            group.shutdownGracefully();
        }
    }

    @Test
    public void testAddSameAddressForSameHostname() throws Exception {
        InetAddress addr1 = InetAddress.getByAddress(new byte[] { 10, 0, 0, 1 });
        EventLoopGroup group = new DefaultEventLoopGroup(1);

        try {
            EventLoop loop = group.next();
            final DefaultDnsCache cache = new DefaultDnsCache();
            cache.cache("netty.io", null, addr1, 1, loop);
            cache.cache("netty.io", null, addr1, 10000, loop);

            List<? extends DnsCacheEntry> entries = cache.get("netty.io", null);
            assertEquals(1, entries.size());
            assertEntry(entries.get(0), addr1);
        } finally {
            group.shutdownGracefully();
        }
    }

    private static void assertEntry(DnsCacheEntry entry, InetAddress address) {
        assertEquals(address, entry.address());
        assertNull(entry.cause());
    }
}