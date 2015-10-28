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


import static org.mockito.Mockito.mock;

import org.junit.BeforeClass;
import org.junit.Test;
import org.teiid.language.Command;
import org.teiid.language.Select;
import org.teiid.metadata.RuntimeMetadata;
import org.teiid.translator.ExecutionContext;
import org.teiid.translator.TranslatorException;
import org.teiid.translator.coherence.CoherenceExecutionFactory;
import org.teiid.translator.object.ObjectConnection;
import org.teiid.translator.object.ObjectExecution;
import org.teiid.translator.object.ObjectUpdateExecution;
import org.teiid.translator.object.ObjectVisitor;
import org.teiid.translator.object.simpleMap.SearchByKey;
import org.teiid.translator.object.simpleMap.SimpleKeyVisitor;
import org.teiid.translator.object.testdata.trades.Trade;

@SuppressWarnings("nls")

public class TestCoherenceLocalCacheSearchByKey extends BaseTestCoherence {

	@BeforeClass
    public static void beforeEachClass() throws Exception {  
	    
		context = mock(ExecutionContext.class);

		factory = new CoherenceManagedConnectionFactory();
 
    	factory.setCacheClassName(Trade.class.getName());
    	factory.setCacheName("dist-extend");
    	factory.setPrimaryKeyFieldName("longValue");
    	factory.setPrimaryKeyFieldClassName("long");   	
    	
    	translator_factory = new CoherenceExecutionFactory();
    	translator_factory.setSearchType(new SearchByKey());
    			
    	translator_factory.start();
    }
	
	@Test
	public void process() throws Exception {
		// load the cache with 100 objects
		
		long start = 2000;
		long end = 2100;
		this.performRange(start, end);		
		
	}

	@Override
	protected ObjectUpdateExecution createUpdateExecution(Command command, RuntimeMetadata metadata, ObjectConnection connection, ExecutionContext executionContext  ) {

		return new ObjectUpdateExecution(command, connection, executionContext, translator_factory) {
			@Override
			protected ObjectVisitor createVisitor() {
		        return new SimpleKeyVisitor();
		    }
		};
	}
	
	@Override
	protected ObjectExecution createExecution(Select command, RuntimeMetadata metadata, ObjectConnection connection, ExecutionContext executionContext  ) throws  TranslatorException {

		return new ObjectExecution(command, metadata, translator_factory, connection, executionContext) {
			@Override
			protected ObjectVisitor createVisitor() {
		        return new SimpleKeyVisitor();
		    }
		};
	}

}
