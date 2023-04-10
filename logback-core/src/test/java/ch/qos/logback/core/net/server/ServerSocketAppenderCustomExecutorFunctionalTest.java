/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * Copyright (C) 1999-2023, QOS.ch. All rights reserved.
 *
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *
 *   or (per the licensee's choosing)
 *
 * under the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation.
 */
package ch.qos.logback.core.net.server;

import ch.qos.logback.core.net.mock.MockContext;
import ch.qos.logback.core.net.server.test.ServerSocketUtil;
import ch.qos.logback.core.util.ExecutorServiceUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Test {@link AbstractServerSocketAppender} with a custom connection executor.
 */
public class ServerSocketAppenderCustomExecutorFunctionalTest {
    private static final String TEST_EVENT = "test event";

    private static final int EVENT_COUNT = 10;

    private ScheduledExecutorService defaultExecutor = ExecutorServiceUtil.newScheduledExecutorService();
    private ScheduledExecutorService connectionExecutor = new ScheduledThreadPoolExecutor(1);
    private MockContext context = new MockContext(defaultExecutor);
    private ServerSocket serverSocket;
    private InstrumentedServerSocketAppenderBase appender;

    @BeforeEach
    public void setUp() throws Exception {

        serverSocket = ServerSocketUtil.createServerSocket();

        appender = new InstrumentedServerSocketAppenderBase(serverSocket)
        {
            @Override
            protected Executor getConnectionExecutor()
            {
                return connectionExecutor;
            }
        };
        appender.setContext(context);
    }

    @AfterEach
    public void tearDown() throws Exception {
        defaultExecutor.shutdownNow();
        defaultExecutor.awaitTermination(10000, TimeUnit.MILLISECONDS);
        Assertions.assertTrue(defaultExecutor.isTerminated());

        connectionExecutor.shutdownNow();
        connectionExecutor.awaitTermination(10000, TimeUnit.MILLISECONDS);
        Assertions.assertTrue(connectionExecutor.isTerminated());
    }

    @Test
    public void testLogEventClient() throws Exception {
        appender.start();
        Socket socket = new Socket(InetAddress.getLocalHost(), serverSocket.getLocalPort());

        socket.setSoTimeout(1000);
        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

        for (int i = 0; i < EVENT_COUNT; i++) {
            appender.append(TEST_EVENT + i);
            Assertions.assertEquals(TEST_EVENT + i, ois.readObject());
        }

        socket.close();
        appender.stop();
    }

}
