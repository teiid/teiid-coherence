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
import static org.mockito.Mockito.mock;

import java.io.File;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.teiid.core.util.UnitTestUtil;
import org.teiid.language.Command;
import org.teiid.language.Select;
import org.teiid.metadata.RuntimeMetadata;
import org.teiid.translator.ExecutionContext;
import org.teiid.translator.coherence.CoherenceExecutionFactory;
import org.teiid.translator.object.ObjectConnection;
import org.teiid.translator.object.ObjectExecution;
import org.teiid.translator.object.ObjectUpdateExecution;
import org.teiid.translator.object.testdata.trades.Trade;

/**
 * This will test against a remote Coherence Cache
 * 
 * To use, do the following:
 * -  Server config file:  use the src/test/resources/coherence_server_files/tangosol-coherence.xml 
 * -  example for starting the server:
 *  
 * java -cp "$COHERENCE_HOME/lib/coherence.jar:$COHERENCE_HOME/lib/translator-object-{version}-tests.jar" -Dtangosol.coherence.cacheconfig=$COHERENCE_HOME/config/tangosol-coherence.xml com.tangosol.net.DefaultCacheServer
 *
 *-  Uncomment @Ignore to enable running
 *
 * 
 * @author vanhalbert
 *
 */
@SuppressWarnings("nls")
@Ignore
public class TestCoherenceRemoteCache extends BaseTestCoherence {
	

	@BeforeClass
    public static void beforeEachClass() throws Exception {  
	    
		context = mock(ExecutionContext.class);
	}
	
    @Before
    public void testCacheFactory1() throws Exception {
    	factory = new CoherenceManagedConnectionFactory();
 
    	File f = UnitTestUtil.getTestDataFile("client-coherence-config.xml");
    	assertEquals(f.exists(), true);
    
    	factory.setCacheClassName(Trade.class.getName());
    	factory.setCacheName("dist-extend");
    	factory.setPrimaryKeyFieldName("longValue");
    	factory.setPrimaryKeyFieldClassName("long");
    	factory.setCacheConfigFilename(f.getAbsolutePath());
    	
    	
    	translator_factory = new CoherenceExecutionFactory();
    			
    	translator_factory.start();
		
    }
    
	@Test
	@Ignore
	public void process() throws Exception {
		// load the cache with 100 objects
		
		long start = 5000;
		long end = 5100;
		performRange(start, end);	
		
	}
	@Override
	protected ObjectUpdateExecution createUpdateExecution(Command command, RuntimeMetadata metadata, ObjectConnection connection, ExecutionContext executionContext  ) throws Exception {

		return (ObjectUpdateExecution) translator_factory.createUpdateExecution(command, executionContext, metadata, connection);
	}
	
	@Override
	protected ObjectExecution createExecution(Select command, RuntimeMetadata metadata, ObjectConnection connection, ExecutionContext executionContext  ) throws  Exception {
	
		return  (ObjectExecution) translator_factory.createExecution(command, context, metadata, connection);
	}

}
