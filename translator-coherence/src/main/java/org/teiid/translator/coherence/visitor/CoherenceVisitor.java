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

package org.teiid.translator.coherence.visitor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.teiid.language.AggregateFunction;
import org.teiid.language.AndOr;
import org.teiid.language.ColumnReference;
import org.teiid.language.Comparison;
import org.teiid.language.Comparison.Operator;
import org.teiid.language.DerivedColumn;
import org.teiid.language.Expression;
import org.teiid.language.Function;
import org.teiid.language.In;
import org.teiid.language.Like;
import org.teiid.language.Literal;
import org.teiid.language.ScalarSubquery;
import org.teiid.language.SearchedCase;
import org.teiid.language.Select;
import org.teiid.logging.LogConstants;
import org.teiid.logging.LogManager;
import org.teiid.metadata.Column;
import org.teiid.translator.TranslatorException;
import org.teiid.translator.coherence.CoherencePlugin;
import org.teiid.translator.coherence.filter.FilterFactory;
import org.teiid.translator.object.ObjectVisitor;
import org.teiid.translator.object.util.ObjectUtil;

import com.tangosol.util.Filter;

/**
 */
public class CoherenceVisitor extends ObjectVisitor {

	protected Map<String, Class<?>> attributeTypes = null;
	
	protected StringBuffer sb = new StringBuffer();
	protected int parmNum = 1;
	protected LinkedList<Object> values = new LinkedList<Object>();

    public CoherenceVisitor() {
        super();        
    }
    
    public List<Object> getValues() {
    	return values;
    }
    
    public Filter createFilter(FilterFactory ff) {
        
        Object[] aEnv = values.toArray();
        return ff.createFilter(sb.toString(), aEnv);
        
    }
    
	@Override
	public void visit(DerivedColumn obj) {
		super.visit(obj);
		ColumnReference expr = (ColumnReference) obj.getExpression();
		Column e = expr.getMetadataObject();
		
		attributeTypes.put(e.getFullName(), e.getJavaType());

	}

    @Override
	public void visit(Select query) {
		attributeTypes = new HashMap<String,Class<?>>(query.getDerivedColumns().size());
		super.visit(query);

		
	}
    
	
	@Override
    public void visit(AndOr obj) {
        visitNode(obj.getLeftCondition());
        
        if (obj.getOperator() == AndOr.Operator.AND) {
        	sb.append(" and ");
        } else {
        	sb.append(" or ");
        }
        visitNode(obj.getRightCondition());
    }
	
    @Override
	public void visit(Comparison obj) {
		LogManager.logTrace(LogConstants.CTX_CONNECTOR, "Parsing Comparison criteria."); //$NON-NLS-1$
		try {
			Comparison.Operator op = obj.getOperator();
	       
			Expression lhs = obj.getLeftExpression();
			Expression rhs = obj.getRightExpression();
	
			String lhsString = getExpressionString(lhs);
			String rhsString = getExpressionString(rhs);
			if(lhsString == null || rhsString == null) {
	            final String msg = CoherencePlugin.UTIL.getString("CoherenceVisitor.missingComparisonExpression"); //$NON-NLS-1$
	            TranslatorException exception = new TranslatorException(msg); 
	            this.addException(exception);
	            return;
			}			

			if (rhs instanceof Literal || lhs instanceof Literal) {
		        if(rhs instanceof Literal) {
		            Literal literal = (Literal) rhs;		            
		    		Column mdIDElement = ((ColumnReference) lhs).getMetadataObject();

		            addCompareCriteria(lhsString, literal.getValue(), op, literal.getType(), mdIDElement );
		            
		        } else {
		            Literal literal = (Literal) lhs;
		    		Column mdIDElement = ((ColumnReference) rhs).getMetadataObject();

		            addCompareCriteria(rhsString, literal.getValue(), op, literal.getType(), mdIDElement);
		        }
			}
		}catch (TranslatorException t) {
			this.addException(t);
		}
    }
    
    @SuppressWarnings("unchecked")
	public void addCompareCriteria(String columnname, Object value, Operator op, Class<?> type, Column element) throws TranslatorException {
    	
    	sb.append(columnname + " ");
        
        String opString = " = ";
        
		switch(op) {
	    case NE:
	    	opString = " <> ";
	    	break;
		case EQ:
			opString = " = ";
			break;
		case GT:
			opString = " > ";
			break;
		case GE:
			opString = " >= "; 
			break;
		case LT:
			opString = " < ";
			break;
		case LE:
			opString = " <= "; 
			break;
		default:
            final String msg = CoherencePlugin.UTIL.getString("CoherenceVisitor.criteriaNotSupportedError"); 
			throw new TranslatorException(msg); 
			
		}
		
		sb.append(opString + " ?" + +parmNum);
		
		values.add(ObjectUtil.convertValueToObjectType(value, element));
			
    }
    
    @Override
	public void visit(Like obj) {
    	
		LogManager.logTrace(LogConstants.CTX_CONNECTOR, "Parsing LIKE criteria."); //$NON-NLS-1$
		boolean isNegated = obj.isNegated();
		// Convert LIKE to Equals, where any "%" symbol is replaced with "*".
		try {
//			Comparison.Operator op = Operator.EQ;
			Expression lhs = obj.getLeftExpression();
			Expression rhs = obj.getRightExpression();
		
			String lhsString = getExpressionString(lhs);
			String rhsString = getExpressionString(rhs);
			
			if(lhsString == null || rhsString == null) {
	            final String msg = CoherencePlugin.UTIL.getString("CoherenceVisitor.missingLikeExpression"); //$NON-NLS-1$
	            TranslatorException exception = new TranslatorException(msg); 
	            this.addException(exception);
	            return;
			}			

			if (rhs instanceof Literal || lhs instanceof Literal) {
		        if(rhs instanceof Literal) {
//		            Literal literal = (Literal) rhs;		            
//		    		Column mdIDElement = ((ColumnReference) lhs).getMetadataObject();

		    		sb.append(lhsString + (isNegated ? " not" : "") + " like ?" + +parmNum);
		    		values.add(rhsString);
		            
		        } else {
//		            Literal literal = (Literal) lhs;
//		    		Column mdIDElement = ((ColumnReference) rhs).getMetadataObject();

		    		sb.append(rhsString + (isNegated ? " not" : "") + " like ?" + +parmNum);
		    		values.add(lhsString);	
		    	}
			}
			    	
//			rhsString = rhsString.replace("%", "*"); //$NON-NLS-1$ //$NON-NLS-2$
//			filter = CoherenceFilterUtil.createFilter(lhsString + " LIKE \'" + rhsString + "\'");		
		}catch (TranslatorException t) {
			this.addException(t);
		}
    }

    
    @Override
	public void visit(In obj) {
		LogManager.logTrace(LogConstants.CTX_CONNECTOR, "Parsing IN criteria."); //$NON-NLS-1$
//		isNegated = ((In) criteria).isNegated();
		try {
			Expression lhs = obj.getLeftExpression();
			Column mdIDElement = ((ColumnReference)lhs).getMetadataObject();

			String lhsString = getExpressionString(lhs);
			
			List<Expression> rhsList = obj.getRightExpressions();
	
//			Class type = null;
//			List parms = new ArrayList(rhsList.size());
	        Iterator iter = rhsList.iterator();
	        while(iter.hasNext()) {
	            Expression expr = (Expression) iter.next();
	            Literal literal = (Literal) expr;
	            
//	            type = addParmFromExpression(expr, parms);
	            
//	    		sb.append(lhsString + " = " + +parmNum);
//	    		
//	    		values.add(ObjectUtil.convertValueToObjectType(literal.getValue(), mdIDElement));
	    		
	    		addInCriteria(lhsString, literal.getValue(), mdIDElement);

	            
	        }
	        
//	        addInCriteria(lhsString, parms, type);
//	        filter = CoherenceFilterUtil.createInFilter(lhsString, parms, type);
		}catch (TranslatorException t) {
			this.addException(t);
		}
	        
    }
    
    public void addInCriteria(String columnname, Object value, Column mdIDElement) throws TranslatorException {
		sb.append(columnname + " = " + +parmNum);
		
		values.add(ObjectUtil.convertValueToObjectType(value, mdIDElement));

    }
 
	/** 
	 * Method to get name from the supplied Element
	 * @param e the supplied Element
	 * @return the name
	 */
    // GHH 20080326 - found that code to fall back on Name if NameInSource
	// was null wasn't working properly, so replaced with tried and true
	// code from another custom connector.
//	public String getNameFromElement(Column e) {
//		String attributeName = null;
//		Object p = e.getParent();
//		if (p instanceof Table) {
//			Table t = (Table)p;
//			if (t.getForeignKeys() != null && !t.getForeignKeys().isEmpty()) {
//				ForeignKey fk = t.getForeignKeys().get(0);
//				String fk_nis = fk.getNameInSource();
//				attributeName = fk_nis + "." + e.getNameInSource();
//			}
//		}
//		if (attributeName == null) {
//			attributeName = e.getNameInSource();
//		}
//		if (attributeName == null || attributeName.equals("")) { //$NON-NLS-1$
//			attributeName = e.getName();
//			// If name in source is not set, then fall back to the column name.
//		}
//		return attributeName;
//	}

//	public String getNameFromTable(Table e) {
//		String tableName = e.getNameInSource();
//		if (tableName == null || tableName.equals("")) { //$NON-NLS-1$
//			tableName = e.getName();
//			// If name in source is not set, then fall back to the column name.
//		}
//		return tableName;
//	}

    /**
     * Helper method for getting runtime <code>Element</code> from a
     * {@link org.teiid.language.DerivedColumn}.
     * @param e Input ISelectSymbol
     * @return Element returned metadata runtime Element
     * @throws TranslatorException 
     */
//    private Column getElementFromSymbol(DerivedColumn symbol) {
//        ColumnReference expr = (ColumnReference) symbol.getExpression();
//        return expr.getMetadataObject();
//    }
	    
	// GHH 20080326 - found that code to fall back on Name if NameInSource
	// was null wasn't working properly, so replaced with tried and true
	// code from another custom connector.
	private String getExpressionString(Expression e) throws TranslatorException {
		String expressionName = null;
		// GHH 20080326 - changed around the IElement handling here
		// - the rest of this method is unchanged
		if(e instanceof ColumnReference) {
			Column mdIDElement = ((ColumnReference)e).getMetadataObject();
			expressionName = mdIDElement.getNameInSource();
			if(expressionName == null || expressionName.equals("")) {  //$NON-NLS-1$
				expressionName = mdIDElement.getName();
			}
		} else if(e instanceof Literal) {
//			try {
//				if(((Literal)e).getType().equals(Class.forName(Timestamp.class.getName()))) {
//					LogManager.logTrace(LogConstants.CTX_CONNECTOR, "Found an expression that uses timestamp; converting to LDAP string format."); //$NON-NLS-1$
//					Timestamp ts = (Timestamp)((Literal)e).getValue();
//					Date dt = new Date(ts.getTime());
//					//TODO: Fetch format if provided.
//					SimpleDateFormat sdf = new SimpleDateFormat(LDAPConnectorConstants.ldapTimestampFormat);
//					expressionName = sdf.format(dt);
//					LogManager.logTrace(LogConstants.CTX_CONNECTOR, "Timestamp to stsring is: " + expressionName); //$NON-NLS-1$
//				}
//				else {
//					expressionName = ((Literal)e).getValue().toString();
//				}
				
				expressionName = ((Literal)e).getValue().toString();
//			} catch (ClassNotFoundException cce) {
//	            final String msg = LDAPPlugin.Util.getString("IQueryToLdapSearchParser.timestampClassNotFoundError"); //$NON-NLS-1$
//				throw new TranslatorException(cce, msg); 
//			}
//				
		} else {
			if(e instanceof AggregateFunction) {
				LogManager.logError(LogConstants.CTX_CONNECTOR, "Received IAggregate, but it is not supported. Check capabilities."); //$NON-NLS-1$
			} else if(e instanceof Function) {
				LogManager.logError(LogConstants.CTX_CONNECTOR, "Received IFunction, but it is not supported. Check capabilties."); //$NON-NLS-1$
			} else if(e instanceof ScalarSubquery) {
				LogManager.logError(LogConstants.CTX_CONNECTOR, "Received IScalarSubquery, but it is not supported. Check capabilties."); //$NON-NLS-1$
			} else if (e instanceof SearchedCase) {
				LogManager.logError(LogConstants.CTX_CONNECTOR, "Received ISearchedCaseExpression, but it is not supported. Check capabilties."); //$NON-NLS-1$
			}
            final String msg = CoherencePlugin.UTIL.getString("CoherenceVisitory.unsupportedElementError" , e.toString()); //$NON-NLS-1$
			throw new TranslatorException(msg); 
		}
		expressionName = escapeReservedChars(expressionName);
		return expressionName;
	}
	
	private String escapeReservedChars(String expr) {
		StringBuffer sb = new StringBuffer();
        for (int i = 0; i < expr.length(); i++) {
            char curChar = expr.charAt(i);
            switch (curChar) {
                case '\\':
                    sb.append("\\5c"); //$NON-NLS-1$
                    break;
                case '*':
                    sb.append("\\2a"); //$NON-NLS-1$
                    break;
                case '(':
                    sb.append("\\28"); //$NON-NLS-1$
                    break;
                case ')':
                    sb.append("\\29"); //$NON-NLS-1$
                    break;
                case '\u0000': 
                    sb.append("\\00"); //$NON-NLS-1$
                    break;
                default:
                    sb.append(curChar);
            }
        }
        return sb.toString();
	}	
	
}
