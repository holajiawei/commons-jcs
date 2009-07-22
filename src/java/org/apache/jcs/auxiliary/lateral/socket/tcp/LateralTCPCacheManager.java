package org.apache.jcs.auxiliary.lateral.socket.tcp;

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

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jcs.auxiliary.AuxiliaryCache;
import org.apache.jcs.auxiliary.lateral.LateralCache;
import org.apache.jcs.auxiliary.lateral.LateralCacheAbstractManager;
import org.apache.jcs.auxiliary.lateral.LateralCacheAttributes;
import org.apache.jcs.auxiliary.lateral.LateralCacheMonitor;
import org.apache.jcs.auxiliary.lateral.LateralCacheNoWait;
import org.apache.jcs.auxiliary.lateral.LateralCacheWatchRepairable;
import org.apache.jcs.auxiliary.lateral.ZombieLateralCacheService;
import org.apache.jcs.auxiliary.lateral.ZombieLateralCacheWatch;
import org.apache.jcs.auxiliary.lateral.behavior.ILateralCacheListener;
import org.apache.jcs.auxiliary.lateral.behavior.ILateralCacheManager;
import org.apache.jcs.auxiliary.lateral.behavior.ILateralCacheService;
import org.apache.jcs.auxiliary.lateral.socket.tcp.behavior.ITCPLateralCacheAttributes;
import org.apache.jcs.engine.behavior.ICompositeCacheManager;
import org.apache.jcs.engine.behavior.IElementSerializer;
import org.apache.jcs.engine.logging.behavior.ICacheEventLogger;

/**
 * Creates lateral caches. Lateral caches are primarily used for removing non laterally configured
 * caches. Non laterally configured cache regions should still be able to participate in removal.
 * But if there is a non laterally configured cache hub, then lateral removals may be necessary. For
 * flat webserver production environments, without a strong machine at the app server level,
 * distribution and search may need to occur at the lateral cache level. This is currently not
 * implemented in the lateral cache.
 * <p>
 * TODO: - need freeCache, release, getStats - need to find an interface acceptable for all - cache
 * managers or a manager within a type
 */
public class LateralTCPCacheManager
    extends LateralCacheAbstractManager
{
    /** Don't change. */
    private static final long serialVersionUID = -9213011856644392480L;

    /** The logger. */
    private final static Log log = LogFactory.getLog( LateralTCPCacheManager.class );

    /** The monitor */
    private static LateralCacheMonitor monitor;

    /** Address to instance map. */
    protected static Map instances = Collections.synchronizedMap( new HashMap() );

    /** ITCPLateralCacheAttributes */
    protected ITCPLateralCacheAttributes lca;

    /** number of clients, we can remove this. */
    private int clients;

    /**
     * Handle to the lateral cache service; or a zombie handle if failed to connect.
     */
    private ILateralCacheService lateralService;

    /**
     * Wrapper of the lateral cache watch service; or wrapper of a zombie service if failed to
     * connect.
     */
    private LateralCacheWatchRepairable lateralWatch;

    /** This is set in the constructor. */
    private ICompositeCacheManager cacheMgr;

    /**
     * Returns an instance of the LateralCacheManager.
     * <p>
     * @param lca
     * @param cacheMgr this allows the auxiliary to be passed a cache manager.
     * @param cacheEventLogger
     * @param elementSerializer
     * @return LateralTCPCacheManager
     */
    public static LateralTCPCacheManager getInstance( ITCPLateralCacheAttributes lca, ICompositeCacheManager cacheMgr,
                                                      ICacheEventLogger cacheEventLogger,
                                                      IElementSerializer elementSerializer )
    {
        synchronized ( instances )
        {
            String key = lca.getTcpServer();
            LateralTCPCacheManager ins = (LateralTCPCacheManager) instances.get( key );
            if ( ins == null )
            {
                log.info( "Instance for [" + key + "] is null, creating" );

                ins = (LateralTCPCacheManager) instances.get( lca.getTcpServer() );
                if ( ins == null )
                {
                    ins = new LateralTCPCacheManager( lca, cacheMgr, cacheEventLogger, elementSerializer );
                    instances.put( key, ins );
                }

                createMonitor( ins );
            }
            ins.clients++;

            return ins;
        }
    }

    /**
     * The monitor needs reference to one instance, actually just a type.
     * <p>
     * TODO refactor this.
     * <p>
     * @param instance
     */
    private static synchronized void createMonitor( ILateralCacheManager instance )
    {
        // only want one monitor per lateral type
        // Fires up the monitoring daemon.
        if ( monitor == null )
        {
            monitor = new LateralCacheMonitor( instance );
            // Should never be null
            if ( monitor != null )
            {
                Thread t = new Thread( monitor );
                t.setDaemon( true );
                t.start();
            }
        }
    }

    /**
     * Constructor for the LateralCacheManager object.
     * <p>
     * @param lcaA
     * @param cacheMgr
     * @param cacheEventLogger
     * @param elementSerializer
     */
    private LateralTCPCacheManager( ITCPLateralCacheAttributes lcaA, ICompositeCacheManager cacheMgr,
                                    ICacheEventLogger cacheEventLogger, IElementSerializer elementSerializer )
    {
        this.lca = lcaA;
        this.cacheMgr = cacheMgr;
        this.cacheEventLogger = cacheEventLogger;
        this.elementSerializer = elementSerializer;

        if ( log.isDebugEnabled() )
        {
            log.debug( "Creating lateral cache service, lca = " + this.lca );
        }

        // Create the service
        try
        {
            if ( log.isInfoEnabled() )
            {
                log.info( "Creating TCP service, lca = " + this.lca );
            }
            this.lateralService = new LateralTCPService( this.lca );

            if ( this.lateralService == null )
            {
                log.error( "No service created, must zombie" );
                throw new Exception( "No service created for lateral cache." );
            }

            this.lateralWatch = new LateralCacheWatchRepairable();
            this.lateralWatch.setCacheWatch( new ZombieLateralCacheWatch() );
        }
        catch ( Exception ex )
        {
            // Failed to connect to the lateral server.
            // Configure this LateralCacheManager instance to use the
            // "zombie" services.
            log.error( "Failure, lateral instance will use zombie service", ex );

            this.lateralService = new ZombieLateralCacheService();
            this.lateralWatch = new LateralCacheWatchRepairable();
            this.lateralWatch.setCacheWatch( new ZombieLateralCacheWatch() );

            // Notify the cache monitor about the error, and kick off
            // the recovery process.
            createMonitor( this );
            monitor.notifyError();
        }
    }

    /**
     * Adds the lateral cache listener to the underlying cache-watch service.
     * <p>
     * @param cacheName The feature to be added to the LateralCacheListener attribute
     * @param listener The feature to be added to the LateralCacheListener attribute
     * @exception IOException
     */
    public void addLateralCacheListener( String cacheName, ILateralCacheListener listener )
        throws IOException
    {
        synchronized ( this.caches )
        {
            lateralWatch.addCacheListener( cacheName, listener );
        }
    }

    /**
     * Called to access a precreated region or construct one with defaults. Since all aux cache
     * access goes through the manager, this will never be called.
     * <p>
     * After getting the manager instance for a server, the factory gets a cache for the region name
     * it is constructing.
     * <p>
     * There should be one manager per server and one cache per region per manager.
     * <p>
     * @return AuxiliaryCache
     * @param cacheName
     */
    public AuxiliaryCache getCache( String cacheName )
    {
        LateralCacheNoWait lateralNoWait = null;
        synchronized ( caches )
        {
            lateralNoWait = (LateralCacheNoWait) caches.get( cacheName );
            if ( lateralNoWait == null )
            {
                LateralCacheAttributes attr = (LateralCacheAttributes) lca.copy();
                attr.setCacheName( cacheName );

                LateralCache cache = new LateralCache( attr, this.lateralService, monitor );
                cache.setCacheEventLogger( cacheEventLogger );
                cache.setElementSerializer( elementSerializer );

                if ( log.isDebugEnabled() )
                {
                    log.debug( "Created cache for noWait, cache [" + cache + "]" );
                }

                lateralNoWait = new LateralCacheNoWait( cache );
                lateralNoWait.setCacheEventLogger( cacheEventLogger );
                lateralNoWait.setElementSerializer( elementSerializer );

                caches.put( cacheName, lateralNoWait );

                if ( log.isInfoEnabled() )
                {
                    log.info( "Created LateralCacheNoWait for [" + lca + "] LateralCacheNoWait = [" + lateralNoWait
                        + "]" );
                }

                // this used to be called every getCache. i move it in here.
                addListenerIfNeeded( cacheName );
            }
        }

        return lateralNoWait;
    }

    /**
     * TODO see if this belongs here or in the factory.
     * <p>
     * @param cacheName
     */
    private void addListenerIfNeeded( String cacheName )
    {
        // don't create a listener if we are not receiving.
        if ( lca.isReceive() )
        {
            try
            {
                addLateralCacheListener( cacheName, LateralTCPListener.getInstance( lca, cacheMgr ) );
            }
            catch ( IOException ioe )
            {
                log.error( "Problem creating lateral listener", ioe );
            }
            catch ( Exception e )
            {
                log.error( "Problem creating lateral listener", e );
            }
        }
        else
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( "Not creating a listener since we are not receiving." );
            }
        }
    }

    /**
     * @return Map
     */
    public Map getInstances()
    {
        return instances;
    }

    /**
     * @return a new service
     * @throws IOException
     */
    public Object fixService()
        throws IOException
    {
        Object service = null;
        try
        {
            service = new LateralTCPService( lca );
        }
        catch ( Exception ex )
        {
            log.error( "Can't fix " + ex.getMessage() );
            throw new IOException( "Can't fix " + ex.getMessage() );
        }
        return service;
    }

    /**
     * Shutsdown the lateral.
     */
    public void shutdown()
    {
        // TODO revist this.
        // the name here doesn't matter.
        try
        {
            lateralService.dispose( "ALL" );
        }
        catch ( IOException e )
        {
            log.error( "Problem disposing of service", e );
        }
    }
}
