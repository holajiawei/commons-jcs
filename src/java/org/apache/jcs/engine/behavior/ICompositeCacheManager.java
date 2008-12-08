package org.apache.jcs.engine.behavior;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.Properties;

import org.apache.jcs.engine.control.CompositeCache;

/**
 * I need the interface so I can plug in mock managers for testing.
 *
 * @author Aaron Smuts
 */
public interface ICompositeCacheManager
{
    /**
     * Gets the cache attribute of the CacheHub object
     *
     * @param cacheName
     * @return CompositeCache
     */
    CompositeCache getCache( String cacheName );
    
    /**
     * This is exposed so other manager can get access to the props.
     * <p>
     * @return the configurationProperties
     */
    Properties getConfigurationProperties();
    
    /**
     * Gets stats for debugging.
     * <p>
     * @return String
     */
    String getStats();
}
