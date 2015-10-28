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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.teiid.translator.TranslatorException;
import org.teiid.translator.coherence.filter.FilterFactory;
import org.teiid.translator.coherence.visitor.CoherenceVisitor;
import org.teiid.translator.object.ObjectConnection;
import org.teiid.translator.object.ObjectVisitor;
import org.teiid.translator.object.SearchType;

import com.tangosol.net.NamedCache;
import com.tangosol.util.Filter;

/**
 * CoherenceSearch will parse the WHERE criteria and build the search query(s)
 * that's used to retrieve the results from a Coherence cache .
 * 
 * Note: As of JDG 6.2, DSL is supported
 * 
 * @author vhalbert
 * 
 */
public class CoherenceSearch implements SearchType {

	private FilterFactory ff;

	public CoherenceSearch(FilterFactory filterFactory) {
		this.ff = filterFactory;
	}

	@Override
	/** 
	 * Calling to make a key value search on the cache.
	 * 
	 * The assumption is the <code>value</code> has already been converted the key object type
	 * {@inheritDoc}
	 *
	 * @see org.teiid.translator.object.SearchType#performKeySearch(java.lang.String, java.lang.Object, org.teiid.translator.object.ObjectConnection)
	 */
	public Object performKeySearch(String columnNameInSource, Object value,
			ObjectConnection conn) {

		Object[] values = new Object[] { value };

		@SuppressWarnings("rawtypes")
		Set setResults = ((NamedCache) conn.getCache()).entrySet(ff.createFilter(columnNameInSource + "= ?1", values));

		if (setResults != null && setResults.size() > 0) {
			Map.Entry entry = (Map.Entry)setResults.iterator().next();
			return entry.getValue();

		}

		return null;
	}

	@Override
	public List<Object> performSearch(ObjectVisitor visitor,
			ObjectConnection conn) throws TranslatorException {

		CoherenceVisitor cv = (CoherenceVisitor) visitor;
		
		if (cv.getWhereCriteria() == null) {
			List<Object> results = new ArrayList<Object>();
			results.addAll(conn.getAll());
			return results;
		}

		Filter f = cv.createFilter(ff);

		@SuppressWarnings("unchecked")
		Set<Object> setResults = ((NamedCache) conn.getCache()).entrySet(f);

		if (setResults != null && setResults.size() > 0) {
			List<Object> results = new ArrayList<Object>(setResults.size());
			Iterator it = setResults.iterator();
			while(it.hasNext()) {				
				@SuppressWarnings("rawtypes")
				Map.Entry entry = (Map.Entry)it.next();
		//		Object key = entry.getKey();
				results.add(entry.getValue());
			}
			
			return results;
		}

		return Collections.EMPTY_LIST;

	}

}
