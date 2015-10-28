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

import javax.resource.ResourceException;
import javax.resource.spi.InvalidPropertyException;

import org.teiid.core.BundleUtil;
import org.teiid.resource.spi.BasicConnectionFactory;
import org.teiid.resource.spi.BasicManagedConnectionFactory;
import org.teiid.translator.TranslatorException;
import org.teiid.translator.TranslatorProperty;
import org.teiid.translator.object.ClassRegistry;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

public class CoherenceManagedConnectionFactory extends BasicManagedConnectionFactory {
	
	private static final long serialVersionUID = -1832915223199053471L;
	
	public static final BundleUtil UTIL = BundleUtil.getBundleUtil(CoherenceManagedConnectionFactory.class);
	
	private ClassRegistry methodUtil = new ClassRegistry();
	
	private NamedCache namedCache;
	private ClassLoader cl;
	private Class<?> cacheTypeClass;
	private Class<?> pkCacheKeyJavaType;
	
	// Parameters to be set by the resource-adapter configuration
	private String cacheName;
	private String cacheConfigFilename;
	private String cacheClassName;
	private String primaryKeyFieldName;
	private String primaryKeyFieldType;
	
	@Override
	public BasicConnectionFactory<CoherenceConnectionImpl> createConnectionFactory() throws ResourceException {
		if (this.getCacheName() == null) {
			throw new InvalidPropertyException(UTIL.getString("CoherenceManagedConnectionFactory.cachename_not_set")); //$NON-NLS-1$
		}
		
		if (this.getCacheClassName() == null) {
			throw new InvalidPropertyException(UTIL.getString("CoherenceManagedConnectionFactory.cacheclassname_not_set")); //$NON-NLS-1$
		}
		
		if (this.getPrimaryKeyFieldName() == null) {
			throw new InvalidPropertyException(UTIL.getString("CoherenceManagedConnectionFactory.primaryKeyFieldNames_not_set")); //$NON-NLS-1$			
		}
		
		return new BasicConnectionFactory<CoherenceConnectionImpl>() {

			@Override
			public CoherenceConnectionImpl getConnection() throws ResourceException {
//				return CoherenceManagedConnectionFactory.this.createCacheConnection();
				return new CoherenceConnectionImpl(CoherenceManagedConnectionFactory.this);
			}
		};
	}	
	
	public String getCacheName() {
		return this.cacheName;
	}
	
	public void setCacheName(String cacheName) {
		this.cacheName = cacheName;
	}
	

	/**
	 * Get the JNDI Name of the cache.
	 * 
	 * @return JNDI Name of cache
	 */
	public String getCacheConfigFilename() {
		return cacheConfigFilename;
	}

	/**
	 * Set the Coniguration File Name that will be used to configure the cache.
	 * 
	 * @param fileName
	 *            the file name of the configuration file that is to be
	 *            used to configure the cache
	 * @see #setCacheConfigFilename(String)
	 */
	public void setCacheConfigFilename(String fileName) {
		this.cacheConfigFilename = fileName;
	}
	
	/**
	 * Returns the name of the class thats being stored in the cache
	 * @return String
	 * 
	 */
	public String getCacheClassName() {
		return this.cacheClassName;
	}

	public void setCacheClassName(String className) {
		this.cacheClassName = className;
	}	
	
	/**
	 * Returns the name of the field that represents the primary key
	 * @return String name
	 * 
	 */
	@TranslatorProperty(display = "PrimaryKeyFieldName", description = "The attribute name that represents the primary key, pkFieldName ")
	public String getPrimaryKeyFieldName() {
		return this.primaryKeyFieldName;
	}

	/**
	 * Call to set the name of the attribute that represents the primary key, pkFieldName
	 * @param fieldName
	 */
	public void setPrimaryKeyFieldName(String fieldName) {
		if (fieldName == null || fieldName.length() < 1) return;
		
		this.primaryKeyFieldName = fieldName;
	
//		primaryKeyFieldNameList = StringUtil.getTokens(fieldNames, ","); //$NON-NLS-1$
	}	
	
	/**
	 * Returns the name of the class name for the object type field that represents the primary key
	 * @return String
	 * 
	 */
	public String getPrimaryKeyFieldClassName() {
		return this.primaryKeyFieldType;
	}

	public void setPrimaryKeyFieldClassName(String fieldKeyType) {
		this.primaryKeyFieldType = fieldKeyType;
	}	
	
	public Class<?> getPrimaryKeyFieldType() {
		return pkCacheKeyJavaType;
	}
	
	public Class<?> getCacheClassType() {
		return this.cacheTypeClass;
	}
	
	public ClassRegistry getClassRegistry() {
		return methodUtil;
	}	
	
	protected synchronized NamedCache getNamedCache() throws ResourceException {
		if (namedCache != null) return namedCache;
		
		createCache();
			
		return namedCache;
	}
	
	public ClassLoader getClassLoader() {
		return this.cl;
	}
	
	protected Class<?> loadClass(String className) throws ResourceException {
		try {
			return Class.forName(className, true, getClassLoader());
		} catch (ClassNotFoundException e) {
			throw new ResourceException(e);
		}
	}
	
	
	protected synchronized CoherenceConnectionImpl createCacheConnection() throws ResourceException {
		return new CoherenceConnectionImpl(this);

//		if (this.namedCache == null) {
//			ClassLoader clh = Thread.currentThread().getContextClassLoader();
//			try {
//				this.cl = this.getClass().getClassLoader();
//				Thread.currentThread().setContextClassLoader(this.cl);
//				loadClasses();
//							
//				namedCache = CacheFactory.getCache(getCacheName(), getClassLoader());
//				
//			} catch (Exception e) {
//				throw new ResourceException(e);
//			} finally {
//				Thread.currentThread().setContextClassLoader(clh);
//			}
//		}
//		return new CoherenceConnectionImpl(this);
	}
	
	protected synchronized void createCache() throws ResourceException {
			ClassLoader clh = Thread.currentThread().getContextClassLoader();
			try {
				this.cl = this.getClass().getClassLoader();
				Thread.currentThread().setContextClassLoader(this.cl);
				loadClasses();
							
				namedCache = CacheFactory.getCache(getCacheName(), getClassLoader());
				
			} catch (Exception e) {
				throw new ResourceException(e);
			} finally {
				Thread.currentThread().setContextClassLoader(clh);
			}
	}	

	protected synchronized void loadClasses() throws ResourceException {
		ClassLoader chl = Thread.currentThread().getContextClassLoader();
		try {
			this.cl = this.getClass().getClassLoader();
			Thread.currentThread().setContextClassLoader(this.cl);
			this.cacheTypeClass = loadClass(this.getCacheClassName());
			try {
				this.methodUtil.registerClass(cacheTypeClass);
			} catch (TranslatorException e1) {
				throw new ResourceException(e1);
			}
			
			if (this.getPrimaryKeyFieldClassName() != null) {
				this.pkCacheKeyJavaType = getPrimaryKeyClass(this.getPrimaryKeyFieldClassName());
			}
		} catch (Exception e) {
			throw new ResourceException(e);
		} finally {
			Thread.currentThread().setContextClassLoader(chl);
		}
	}
	
	private Class<?> getPrimaryKeyClass(String className) throws ResourceException {
		if (className.contains(".")) {
			return loadClass(className);
		}
		if (className.equalsIgnoreCase("int")) {
			return int.class;
		}
		if (className.equalsIgnoreCase("long")) {
			return long.class;
		}
		if (className.equalsIgnoreCase("double")) {
			return double.class;
		}
		if (className.equalsIgnoreCase("short")) {
			return short.class;
		}
		if (className.equalsIgnoreCase("char")) {
			return char.class;
		}		
		if (className.equalsIgnoreCase("float")) {
			return float.class;
		}
		if (className.equalsIgnoreCase("boolean")) {
			return boolean.class;
		}
		
		return loadClass(className);
	}
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((cacheName == null) ? 0 : cacheName.hashCode());
		result = prime
				* result
				+ ((cacheClassName == null) ? 0
						: cacheClassName.hashCode());
		result = prime
				* result
				+ ((primaryKeyFieldName == null) ? 0
						: primaryKeyFieldName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CoherenceManagedConnectionFactory other = (CoherenceManagedConnectionFactory) obj;

		if (!checkEquals(this.getCacheName(), other.getCacheName())) {
			return false;
		}
		if (!checkEquals(this.getCacheClassName(),
				other.getCacheClassName())) {
			return false;
		}
		if (!checkEquals(this.getPrimaryKeyFieldName(),
				other.getPrimaryKeyFieldName())) {
			return false;
		}

		return true;

	}
	

}
