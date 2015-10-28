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

import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;


/**
 * @author vanhalbert
 *
 */
public class NamedCacheProxy implements InvocationHandler {
    private NamedCache wrapped;
    public NamedCacheProxy(NamedCache r) {
        wrapped = r;
    }
    @Override
	public Object invoke(Object proxy, Method method, Object[] args)
           throws Throwable {
//       if ("workOn".equals(method.getName())) {
//            args[1] = Tool.RATCHET;
//        }
        return method.invoke(wrapped, args);
    }
    public static NamedCache createBuilderRobot(CoherenceManagedConnectionFactory factory) {
    	NamedCache nc = createConnection(factory);
        return (NamedCache)(Proxy.newProxyInstance(factory.getClassLoader(),
            new Class[] {NamedCache.class},
                new NamedCacheProxy(nc)));
    }
//    public static final void main(String[] args) {
//        Robot r = createBuilderRobot(new MyRobot());
//        r.workOn("scrap", Tool.CUTTING_TORCH);
//    }
	private static NamedCache createConnection(CoherenceManagedConnectionFactory factory) {
		CacheFactory.ensureCluster();
		
		String configFileName = factory.getCacheConfigFilename();
		if (configFileName != null && configFileName.length() > 0) {
			System.setProperty("tangosol.coherence.cacheconfig", configFileName);
		} 
		
	//	CacheFactory.getConfigurableCacheFactory().ensureService("ExtendTcpInvocationService").getCluster().
					
		NamedCache nc = CacheFactory.getCache(factory.getCacheName(), factory.getClassLoader());
		return nc;
	}
}