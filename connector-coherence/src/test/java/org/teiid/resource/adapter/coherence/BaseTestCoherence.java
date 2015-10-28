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

import java.util.List;

import javax.resource.ResourceException;

import org.mockito.Mock;
import org.teiid.cdk.api.TranslationUtility;
import org.teiid.language.Command;
import org.teiid.language.Select;
import org.teiid.metadata.RuntimeMetadata;
import org.teiid.translator.ExecutionContext;
import org.teiid.translator.coherence.CoherenceExecutionFactory;
import org.teiid.translator.object.ObjectConnection;
import org.teiid.translator.object.ObjectExecution;
import org.teiid.translator.object.ObjectUpdateExecution;
import org.teiid.translator.object.testdata.trades.TradesCacheSource;
import org.teiid.translator.object.testdata.trades.VDBUtility;

@SuppressWarnings("nls")

public abstract class BaseTestCoherence{
	protected static TranslationUtility translationUtility = VDBUtility.TRANSLATION_UTILITY;
	protected static int SELECT_STAR_COL_COUNT = TradesCacheSource.NUM_OF_ALL_COLUMNS;


	@Mock
	protected static ExecutionContext context;
	
	static CoherenceExecutionFactory translator_factory;
	static CoherenceManagedConnectionFactory factory;
	
	protected void performRange(long start, long end) throws Exception {

		insertObjects(start, end);
		selectAllObjects(100);
		updateObjects(start, end);
		deleteObjects(start, end);
		selectAllObjects(0);
		
	}
	
	public void insertObjects(long start, long end) throws Exception {
		
		boolean settled = false;
		for (long i = start; i < end; ++i) {
			
			Command command = translationUtility
					.parseCommand("Insert into Trade_Object.Trade (tradeId, TradeName, settled) VALUES (" + i + ", 'TestName " + i + "', '" + settled + "' )");
			settled = !settled;
	
			ObjectUpdateExecution ie = createUpdateExecution(command, VDBUtility.RUNTIME_METADATA, getConnection(), context );

			ie.execute();		
			
			Select scommand = (Select)VDBUtility.TRANSLATION_UTILITY.parseCommand("select * From Trade_Object.Trade as T where TradeID = " + i); //$NON-NLS-1$
			
			submitQuery(scommand, 1, SELECT_STAR_COL_COUNT);

		}
	}
	
	public void updateObjects(long start, long end) throws Exception {
		for (long i = start; i < end; ++i) {
			
			Command command = translationUtility
					.parseCommand("Update Trade  SET TradeName='Person " + i + " Changed', settled='true' WHERE TradeId=" + i);
			
			ObjectUpdateExecution ie = createUpdateExecution(command, VDBUtility.RUNTIME_METADATA, getConnection(), context );

			ie.execute();		
					
			Select scommand = (Select)VDBUtility.TRANSLATION_UTILITY.parseCommand("select * From Trade_Object.Trade as T where TradeID = " + i); //$NON-NLS-1$
			
			submitQuery(scommand, 1, SELECT_STAR_COL_COUNT);			

		}
	}
	
	public void deleteObjects(long start, long end) throws Exception {
		for (long i = start; i < end; ++i) {
			
			Command command = translationUtility
					.parseCommand("Delete From Trade Where TradeId = " + i);
			
			ObjectUpdateExecution ie = createUpdateExecution(command, VDBUtility.RUNTIME_METADATA, getConnection(), context );

			ie.execute();		
					
			Select scommand = (Select)VDBUtility.TRANSLATION_UTILITY.parseCommand("select * From Trade_Object.Trade as T where TradeID = " + i); //$NON-NLS-1$
			
			submitQuery(scommand, 0, SELECT_STAR_COL_COUNT);		

		}
	}
	
	public void selectAllObjects(int rowcnt) throws Exception {
		Select scommand = (Select)VDBUtility.TRANSLATION_UTILITY.parseCommand("select * From Trade_Object.Trade"); //$NON-NLS-1$
		
		submitQuery(scommand, rowcnt, SELECT_STAR_COL_COUNT);
		
	}
	
	public void submitQuery(Select command, int rowcnt, int colCount) throws Exception {
		ObjectExecution ie = createExecution(command, VDBUtility.RUNTIME_METADATA, getConnection(), context );

		ie.execute();
		
		int cnt = 0;
		List<Object> row = ie.next();
	
		while (row != null) {
			assertEquals("column count doesnt match", colCount, row.size());
			++cnt;
			row = ie.next();
		}
		
		assertEquals(command.toString(), rowcnt, cnt); //$NON-NLS-1$
		
		ie.close();		
	}
	
	
	protected abstract ObjectUpdateExecution createUpdateExecution(Command command, RuntimeMetadata metadata, ObjectConnection connection, ExecutionContext executionContext  ) throws Exception ;
	
	protected abstract ObjectExecution createExecution(Select command, RuntimeMetadata metadata, ObjectConnection connection, ExecutionContext executionContext  ) throws  Exception ;

	protected static ObjectConnection getConnection() throws ResourceException {
		return factory.createConnectionFactory().getConnection();
	}

}
