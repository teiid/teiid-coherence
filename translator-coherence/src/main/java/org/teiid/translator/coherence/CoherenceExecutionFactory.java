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

package org.teiid.translator.coherence;

import org.teiid.core.BundleUtil;
import org.teiid.language.Command;
import org.teiid.language.QueryExpression;
import org.teiid.language.Select;
import org.teiid.metadata.RuntimeMetadata;
import org.teiid.translator.ExecutionContext;
import org.teiid.translator.ResultSetExecution;
import org.teiid.translator.Translator;
import org.teiid.translator.TranslatorException;
import org.teiid.translator.UpdateExecution;
import org.teiid.translator.coherence.filter.FilterFactory;
import org.teiid.translator.coherence.visitor.CoherenceVisitor;
import org.teiid.translator.coherence.visitor.DeleteVisitor;
import org.teiid.translator.object.ObjectConnection;
import org.teiid.translator.object.ObjectExecution;
import org.teiid.translator.object.ObjectExecutionFactory;
import org.teiid.translator.object.ObjectUpdateExecution;
import org.teiid.translator.object.ObjectVisitor;


@Translator(name = "coherence", description = "Transaltor to support accessing Coherence cache")
public class CoherenceExecutionFactory extends ObjectExecutionFactory {
	
	public static final BundleUtil UTIL = BundleUtil.getBundleUtil(CoherenceExecutionFactory.class);

	public static final int MAX_SET_SIZE = 100;
	
//	private ClassRegistry methodUtil = new ClassRegistry();
	
	private FilterFactory filterFactory = null;
	
	public CoherenceExecutionFactory() {
		super();
		this.setMaxInCriteriaSize(MAX_SET_SIZE);
		this.setMaxDependentInPredicates(1);
		this.setSupportsOrderBy(true);
		this.setSupportsSelectDistinct(false);
		this.setSupportsInnerJoins(true);
		this.setSupportsFullOuterJoins(false);
		this.setSupportsOuterJoins(false);
		
		filterFactory = new FilterFactory("CoherenceExecutionFactory");
		
		setSearchType(new CoherenceSearch(filterFactory));

	}
	
//	
//	@Override
//	public void start() throws TranslatorException {
//
//		super.start();
//		
//		this.namedCache = getNamedCache();
//
//	}



	@Override
	public ResultSetExecution createResultSetExecution(QueryExpression command,
			ExecutionContext executionContext, RuntimeMetadata metadata,
			ObjectConnection connection) throws TranslatorException {
		return new ObjectExecution((Select) command, metadata, this, connection, executionContext) {
			@Override
			protected ObjectVisitor createVisitor() {
		        return new CoherenceVisitor();
		    }
		};
	}
	
    @Override
	public UpdateExecution createUpdateExecution(Command command,
			ExecutionContext executionContext, RuntimeMetadata metadata,
			ObjectConnection connection) throws TranslatorException {
    	return new ObjectUpdateExecution(command, connection, executionContext, this) {
			@Override
			protected ObjectVisitor createVisitor() {
		        return new CoherenceVisitor();
		    }  		
    	};
	}	

	@Override
	public boolean isSourceRequired() {
		return true;
	}   
	
	@Override
	public boolean isSourceRequiredForMetadata() {
		return true;
	}
	
	@Override
	public boolean returnsSingleUpdateCount() {
		return true;
	}
	
//	protected ClassRegistry getClassRegistry() {
//		return this.methodUtil;
//	}
//	
//	private NamedCache getNamedCache() {
//		
//		NamedCache sourceCache = CacheFactory.getCache(this.getCacheName(), this.getClass().getClassLoader());
//
//		LogManager.logDetail(LogConstants.CTX_CONNECTOR,"Coherence NamedCache " + this.getCacheName() + " has been obtained."); //$NON-NLS-1$
//
//		return sourceCache;
//		
//	}	

}
