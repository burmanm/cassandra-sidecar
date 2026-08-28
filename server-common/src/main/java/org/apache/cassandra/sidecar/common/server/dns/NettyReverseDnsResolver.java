/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.cassandra.sidecar.common.server.dns;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.dns.DefaultDnsQuestion;
import io.netty.handler.codec.dns.DnsPtrRecord;
import io.netty.handler.codec.dns.DnsQuestion;
import io.netty.handler.codec.dns.DnsRecord;
import io.netty.handler.codec.dns.DnsRecordType;
import io.netty.resolver.dns.DnsNameResolver;
import io.netty.resolver.dns.DnsNameResolverBuilder;
import io.netty.resolver.dns.DnsServerAddressStreamProviders;
import io.netty.util.concurrent.DefaultThreadFactory;

/**
 * Queries for all PTR records using Netty and returns the one that matches our partial DNS name we give it.
 *
 * While vertx has DnsClientImpl, it sadly hides the queryAll() method so we can't use it for the PTR resolve and
 * as such we have some unnecessary duplication here with silly eventloopGroups added to it.
 */
final class NettyReverseDnsResolver implements AutoCloseable
{
    private static final String DNS_EVENT_LOOP_NAME = "sidecar-dns-resolver";

    private final String hostnamePart;
    private final DnsNameResolver resolver;
    private final EventLoopGroup eventLoopGroup;

    private NettyReverseDnsResolver(String hostnamePart,
                                    DnsNameResolver resolver,
                                    EventLoopGroup eventLoopGroup)
    {
        this.hostnamePart = hostnamePart;
        this.resolver = resolver;
        this.eventLoopGroup = eventLoopGroup;
    }

    static NettyReverseDnsResolver create(String hostnamePart)
    {
        EventLoopGroup eventLoopGroup = new NioEventLoopGroup(1, new DefaultThreadFactory(DNS_EVENT_LOOP_NAME, true));
        DnsNameResolver resolver = new DnsNameResolverBuilder(eventLoopGroup.next())
                                   .datagramChannelType(NioDatagramChannel.class)
                                   .nameServerProvider(DnsServerAddressStreamProviders.platformDefault())
                                   .build();
        NettyReverseDnsResolver reverseDnsResolver = new NettyReverseDnsResolver(hostnamePart, resolver,
            eventLoopGroup);

        Runtime.getRuntime().addShutdownHook(new Thread(reverseDnsResolver::close, DNS_EVENT_LOOP_NAME + "-shutdown"));
        return reverseDnsResolver;
    }

    static NettyReverseDnsResolver createForTesting(String hostnamePart, DnsNameResolver resolver)
    {
        return new NettyReverseDnsResolver(hostnamePart, resolver, null);
    }

    String reverseResolve(String address) throws UnknownHostException
    {
        DnsQuestion question = new DefaultDnsQuestion(reverseName(InetAddress.getByName(address)), DnsRecordType.PTR);
        List<DnsRecord> records;
        try
        {
            records = resolver.resolveAll(question).get();
        }
        catch (InterruptedException exception)
        {
            Thread.currentThread().interrupt();
            throw unknownHost(address, "Interrupted while resolving PTR records", exception);
        }
        catch (ExecutionException exception)
        {
            throw unknownHost(address, "Unable to resolve PTR records", exception.getCause());
        }

        return records.stream()
                      .filter(DnsPtrRecord.class::isInstance)
                      .map(DnsPtrRecord.class::cast)
                      .map(DnsPtrRecord::hostname)
                      .filter(hostname -> hostname.contains(hostnamePart))
                      .min(Comparator.naturalOrder())
                      .orElseThrow(() -> unknownHost(address,
                                                     "No PTR record matches service name " + hostnamePart,
                                                     null));
    }

    @Override
    public void close()
    {
        resolver.close();
        if (eventLoopGroup != null)
        {
            eventLoopGroup.shutdownGracefully();
        }
    }

    static String reverseName(InetAddress address)
    {
        byte[] bytes = address.getAddress();
        if (bytes.length == 4)
        {
            return unsigned(bytes[3]) + "." +
                   unsigned(bytes[2]) + "." +
                   unsigned(bytes[1]) + "." +
                   unsigned(bytes[0]) + ".in-addr.arpa.";
        }

        StringBuilder reverseName = new StringBuilder(73);
        for (int index = bytes.length - 1; index >= 0; index--)
        {
            int value = unsigned(bytes[index]);
            reverseName.append(Character.forDigit(value & 0x0f, 16))
                       .append('.')
                       .append(Character.forDigit((value >>> 4) & 0x0f, 16))
                       .append('.');
        }
        return reverseName.append("ip6.arpa.").toString();
    }

    private static int unsigned(byte value)
    {
        return value & 0xff;
    }

    private static UnknownHostException unknownHost(String address, String message, Throwable cause)
    {
        UnknownHostException exception = new UnknownHostException(message + " for " + address);
        if (cause != null)
        {
            exception.initCause(cause);
        }
        return exception;
    }
}
