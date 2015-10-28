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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import javax.resource.spi.InvalidPropertyException;

import org.junit.Test;
import org.teiid.translator.object.ObjectConnection;
import org.teiid.translator.object.testdata.trades.Trade;
import org.teiid.translator.object.testdata.trades.TradesCacheSource;

@SuppressWarnings("nls")
public class TestCoherenceManagedConnectionFactory {
	
    @Test
    public void testCacheFactory1() throws Exception {
    	CoherenceManagedConnectionFactory factory = new CoherenceManagedConnectionFactory();
 
    	factory.setCacheClassName(Trade.class.getName());
    	factory.setCacheName(TradesCacheSource.TRADES_CACHE_NAME);
    	factory.setPrimaryKeyFieldName("longValue");
    	factory.setPrimaryKeyFieldClassName("long");
    	factory.setCacheConfigFilename("config-file.xml");

    	
    		ObjectConnection conn = factory.createConnectionFactory().getConnection();
    		
    		assertEquals(conn.getCacheName(), TradesCacheSource.TRADES_CACHE_NAME);
    		assertNotNull(conn.getPkField());
    		assertEquals(conn.getPkField(), "longValue");
    		
    		assertEquals(factory.getCacheClassName(), Trade.class.getName());

    		assertNotNull(conn.getCacheKeyClassType());
       		assertEquals(conn.getCacheKeyClassType().getName(), "long");
  		
    		assertNotNull(conn.getClassRegistry());
     		
    }
    
    @Test
    public void testCacheFactory2() throws Exception {
    	CoherenceManagedConnectionFactory factory = new CoherenceManagedConnectionFactory();
 
    	factory.setCacheClassName(Trade.class.getName());
    	factory.setCacheName(TradesCacheSource.TRADES_CACHE_NAME);
    	factory.setPrimaryKeyFieldName("longValue");
 
    	
    		ObjectConnection conn = factory.createConnectionFactory().getConnection();
    		
    		assertEquals(conn.getCacheName(), TradesCacheSource.TRADES_CACHE_NAME);
    		assertNotNull(conn.getPkField());
    		assertEquals(conn.getPkField(), "longValue");
    		
    		assertEquals(factory.getCacheClassName(), Trade.class.getName());

    		assertNull(conn.getCacheKeyClassType());
  		
    		assertNotNull(conn.getClassRegistry());
     		
    }  

    
    @Test (expected = InvalidPropertyException.class)
    public void testCacheFactoryNoCacheName() throws Exception {
    	CoherenceManagedConnectionFactory factory = new CoherenceManagedConnectionFactory();
 
  		factory.createConnectionFactory().getConnection();

    }

    @Test (expected = InvalidPropertyException.class)
    public void testCacheFactoryNoClassName() throws Exception {
    	CoherenceManagedConnectionFactory factory = new CoherenceManagedConnectionFactory();
    	factory.setCacheClassName(Trade.class.getName());
 
  		factory.createConnectionFactory().getConnection();

    }

    @Test (expected = InvalidPropertyException.class)
    public void testCacheFactoryNoPrimaryKey() throws Exception {
    	CoherenceManagedConnectionFactory factory = new CoherenceManagedConnectionFactory();
    	factory.setCacheClassName(Trade.class.getName());
       	factory.setCacheName(TradesCacheSource.TRADES_CACHE_NAME);      
 
  		factory.createConnectionFactory().getConnection();

    }


}
