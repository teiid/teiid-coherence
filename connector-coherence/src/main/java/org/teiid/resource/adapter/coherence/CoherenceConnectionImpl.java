/*
 * JBoss, Home of Professional Open Source.
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */

package org.teiid.resource.adapter.coherence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.resource.ResourceException;

import org.teiid.core.BundleUtil;
import org.teiid.logging.LogConstants;
import org.teiid.logging.LogManager;
import org.teiid.resource.spi.BasicConnection;
import org.teiid.translator.TranslatorException;
import org.teiid.translator.coherence.CoherenceConnection;
import org.teiid.translator.object.ClassRegistry;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.util.Filter;
import com.tangosol.util.TransactionMap;

/** 
 * Represents an implementation for the connection to a Coherence data source. 
 */
public class CoherenceConnectionImpl extends BasicConnection implements CoherenceConnection { 
	
	public static final BundleUtil UTIL = BundleUtil.getBundleUtil(CoherenceConnection.class);
	
	private CoherenceManagedConnectionFactory config;
	private NamedCache namedCache;
	
	
	public CoherenceConnectionImpl(CoherenceManagedConnectionFactory config) throws ResourceException {
		this.config = config;
		namedCache=config.getNamedCache();
		LogManager.logDetail(LogConstants.CTX_CONNECTOR, "Coherence Connection has been newly created for cache " +  this.config.getCacheName()); //$NON-NLS-1$
	}

	/** 
	 * Close the connection, if a connection requires closing.
	 * (non-Javadoc)
	 */
	@Override
    public void close() {
		LogManager.logDetail(LogConstants.CTX_CONNECTOR,"Coherence NamedCache " + this.config.getCacheName() + " has been released."); //$NON-NLS-1$
		this.config = null;
		this.namedCache = null;
	}

	/** 
	 * Currently, this method always returns alive. We assume the connection is alive,
	 * and rely on proper timeout values to automatically clean up connections before
	 * any server-side timeout occurs. Rather than incur overhead by rebinding,
	 * we'll assume the connection is always alive, and throw an error when it is actually used,
	 * if the connection fails. This may be a more efficient way of handling failed connections,
	 * with the one tradeoff that stale connections will not be detected until execution time. In
	 * practice, there is no benefit to detecting stale connections before execution time.
	 * 
	 * One possible extension is to implement a UnsolicitedNotificationListener.
	 * (non-Javadoc)
	 */
	@Override
	public boolean isAlive() {
		boolean isAlive = false;
		
		if (this.config == null || namedCache == null) {
			isAlive = false;
		} else { 
			isAlive = namedCache.isActive();
		}

		LogManager.logTrace(LogConstants.CTX_CONNECTOR, "Is Coherence Connection namedCache alive: " + isAlive); //$NON-NLS-1$

		return isAlive;
	}
	
	@Override
	public Object get(Object criteria)  {

		if (! (criteria instanceof Filter)) {
			Object o = getCache().get(criteria);
			if (o != null) {
				return o;
			}
			return null;
		}
		
		Filter filter = (Filter) criteria;
		
		LogManager.logTrace(LogConstants.CTX_CONNECTOR, "Coherence Connection: Filter - " + (criteria != null ? criteria.toString() : "NULL"));
		
		Set setResults = getCache().entrySet(filter);		
		
		for (Iterator it = setResults.iterator(); it.hasNext();) {
			Map.Entry o = (Map.Entry) it.next();
			return o.getValue();
		}
		return null;

	}
	
	@Override
	public void add(Object key, Object value) throws TranslatorException {
		 
			NamedCache sourceCache =  getCache();
			if (sourceCache.containsKey(key)) {
				throw new TranslatorException("Unable to add object for key: " + key + " to cache "  + this.config.getCacheName() + ", because it already exist");
			}
			
			TransactionMap tmap = CacheFactory.getLocalTransaction(sourceCache);

			tmap.setTransactionIsolation(TransactionMap.TRANSACTION_REPEATABLE_GET);
			tmap.setConcurrency(TransactionMap.CONCUR_PESSIMISTIC);
			
			tmap.begin();
			try
			    {
			    tmap.put(key, value);
			    tmap.prepare();
			    tmap.commit();
			    }
			catch (Exception e) {
				throw new TranslatorException(e);
			}
			
			sourceCache = getCache();
			if (!sourceCache.containsKey(key)) {
				throw new TranslatorException("Problem adding object for key: " + key + " to the cache " + this.config.getCacheName() +", object not found after add");
			}
			
			
			LogManager.logTrace(LogConstants.CTX_CONNECTOR,"Coherence object inserted in cache " + this.config.getCacheName() + " for the key " + key); //$NON-NLS-1$

		
	}
	
	@Override
	public void update(Object key, Object object) throws TranslatorException {
		NamedCache sourceCache =  getCache();
		if (!sourceCache.containsKey(key)) {
			throw new TranslatorException("Unable to update object for key: " + key + " to cache "  + this.config.getCacheName() + ", because it doesn't exist");
		}
		
		TransactionMap tmap = CacheFactory.getLocalTransaction(sourceCache);

		tmap.setTransactionIsolation(TransactionMap.TRANSACTION_REPEATABLE_GET);
		tmap.setConcurrency(TransactionMap.CONCUR_PESSIMISTIC);
		
		tmap.begin();
		try
		    {
		    tmap.put(key, object);
		    tmap.prepare();
		    tmap.commit();
		    }
		catch (Exception e) {
			throw new TranslatorException(e);
		}
		
		sourceCache = getCache();
		if (!sourceCache.containsKey(key)) {
			throw new TranslatorException("Problem updating object for key: " + key + " to the cache "  + this.config.getCacheName() +", object not found after the update");
		}

		
		LogManager.logTrace(LogConstants.CTX_CONNECTOR,"Coherence object updated in cache " + this.config.getCacheName() + " for the key " + key); //$NON-NLS-1$

		
	}
	
	@Override
	public Object remove(Object key) throws TranslatorException {
		NamedCache sourceCache =  getCache();
		if (!sourceCache.containsKey(key)) {
			// need to log that it wasn't found, but don't error
			LogManager.logDetail(LogConstants.CTX_CONNECTOR,"WARNING: No object to remove from cache " + this.config.getCacheName() + " for the key " + key); //$NON-NLS-1$
			
			return null;
		}
				
		TransactionMap tmap = CacheFactory.getLocalTransaction(sourceCache);

		tmap.setTransactionIsolation(TransactionMap.TRANSACTION_REPEATABLE_GET);
		tmap.setConcurrency(TransactionMap.CONCUR_OPTIMISTIC);
		
		tmap.begin();
		Object o = null;
		try
		    {
		    o = tmap.remove(key);
		    tmap.prepare();
		    tmap.commit();
		    }
		catch (Exception e) {
			throw new TranslatorException(e);

		}
		
		if (getCache().containsKey(key)) {
			throw new TranslatorException("Unable to remove object for key: " + key + " from the cache " + this.config.getCacheName() );
		}
		
		LogManager.logTrace(LogConstants.CTX_CONNECTOR,"Coherence object removed from cache " + this.config.getCacheName() + " for the key " + key); //$NON-NLS-1$


		return o;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.teiid.translator.object.ObjectConnection#getCache()
	 */
	@Override
	public NamedCache getCache() {
		return namedCache;
		
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.teiid.translator.object.ObjectConnection#getPkField()
	 */
	@Override
	public String getPkField() {
		return this.config.getPrimaryKeyFieldName();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.teiid.translator.object.ObjectConnection#getCacheKeyClassType()
	 */
	@Override
	public Class<?> getCacheKeyClassType() throws TranslatorException {
		return this.config.getPrimaryKeyFieldType();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.teiid.translator.object.ObjectConnection#getCacheName()
	 */
	@Override
	public String getCacheName() {
		return this.config.getCacheName();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.teiid.translator.object.ObjectConnection#getCacheClassType()
	 */
	@Override
	public Class<?> getCacheClassType() {
		return this.config.getCacheClassType();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.teiid.translator.object.ObjectConnection#getClassRegistry()
	 */
	@Override
	public ClassRegistry getClassRegistry() {
		return this.config.getClassRegistry();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.teiid.translator.object.ObjectConnection#getAll()
	 */
	@Override
	public Collection<Object> getAll() {
		Collection<Object> results = new ArrayList<Object>();
		NamedCache c = this.getCache();
		Set<Object> keys = c.keySet();
		for (Object k:keys) {
			results.add(c.get(k));
		}
		return results;
	}

	
}
