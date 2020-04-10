package org.apache.commons.jcs.log;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */

import java.util.ServiceLoader;

/**
 * This is a borrowed and stripped-down version of the log4j2 LogManager class.
 *
 * The anchor point for the Log4j logging system. The most common usage of this
 * class is to obtain a named {@link Log}.
 */
public class LogManager
{
    /**
     * The name of log subsystem
     */
    private static String logSystem = "jul";

    /**
     * The SPI LogFactory
     */
    private static class LogFactoryHolder
    {
        static final LogFactory INSTANCE = createLogFactory();

        /**
         * Scans the classpath to find a logging implementation.
         *
         * @return the LogFactory
         * @throws RuntimeException, if no factory implementation could be found
         */
        private static LogFactory createLogFactory()
        {
            ServiceLoader<LogFactory> factories = ServiceLoader.load(LogFactory.class);
            LogManager.logSystem = System.getProperty("jcs.logSystem", LogManager.logSystem);

            for (LogFactory factory : factories)
            {
                if (logSystem.equalsIgnoreCase(factory.getName()))
                {
                    return factory;
                }
            }

            throw new RuntimeException("Could not find factory implementation for log subsystem " + logSystem);
        }
    }

    /**
     * Set the log system. Must be called before getLog is called
     *
     * @param logSystem the logSystem to set
     */
    public static void setLogSystem(String logSystem)
    {
        LogManager.logSystem = logSystem;
    }

    /**
     * Return the LogFactory
     */
    private static LogFactory getLogFactory()
    {
        return LogFactoryHolder.INSTANCE;
    }

    /**
     * Prevents instantiation
     */
    protected LogManager()
    {
    }

    /**
     * Shutdown the logging system if the logging system supports it.
     */
    public static void shutdown()
    {
        getLogFactory().shutdown();
    }

    /**
     * Returns a Log using the fully qualified name of the Class as the Log
     * name.
     *
     * @param clazz
     *            The Class whose name should be used as the Log name. If null
     *            it will default to the calling class.
     * @return The Log.
     * @throws UnsupportedOperationException
     *             if {@code clazz} is {@code null} and the calling class cannot
     *             be determined.
     */
    public static Log getLog(final Class<?> clazz)
    {
        return getLogFactory().getLog(clazz);
    }

    /**
     * Returns a Log with the specified name.
     *
     * @param name
     *            The logger name. If null the name of the calling class will be
     *            used.
     * @return The Log.
     * @throws UnsupportedOperationException
     *             if {@code name} is {@code null} and the calling class cannot
     *             be determined.
     */
    public static Log getLog(final String name)
    {
        return getLogFactory().getLog(name);
    }

    /**
     * Returns the root logger.
     *
     * @return the root logger, named {@link LogFactory.ROOT_LOGGER_NAME}.
     */
    public static Log getRootLogger()
    {
        return getLog(LogFactory.ROOT_LOGGER_NAME);
    }
}
