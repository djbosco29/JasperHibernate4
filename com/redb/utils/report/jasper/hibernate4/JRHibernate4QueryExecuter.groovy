/*
 * JasperReports - Free Java Reporting Library.
 * Copyright (C) 2001 - 2009 Jaspersoft Corporation. All rights reserved.
 * http://www.jaspersoft.com
 *
 * Unless you have purchased a commercial license agreement from Jaspersoft,
 * the following license terms apply:
 *
 * This program is part of JasperReports.
 *
 * JasperReports is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JasperReports is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with JasperReports. If not, see <http://www.gnu.org/licenses/>.
 * 
 * This file was modified by Dario Bosco (djbosco29@redbsistemas.com)
 * For adding Hibernate4 support.
 */
package com.redb.utils.report.jasper.hibernate4

import java.sql.Time
import java.sql.Timestamp

import net.sf.jasperreports.engine.DefaultJasperReportsContext
import net.sf.jasperreports.engine.JRDataSource
import net.sf.jasperreports.engine.JRDataset
import net.sf.jasperreports.engine.JRException
import net.sf.jasperreports.engine.JRRuntimeException
import net.sf.jasperreports.engine.JRValueParameter
import net.sf.jasperreports.engine.JasperReportsContext
import net.sf.jasperreports.engine.data.JRHibernateIterateDataSource
import net.sf.jasperreports.engine.data.JRHibernateScrollDataSource
import net.sf.jasperreports.engine.query.JRAbstractQueryExecuter
import net.sf.jasperreports.engine.query.JRHibernateQueryExecuter
import net.sf.jasperreports.engine.util.JRStringUtil

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.hibernate.Query
import org.hibernate.ScrollMode
import org.hibernate.ScrollableResults
import org.hibernate.Session
import org.hibernate.type.Type

class JRHibernate4QueryExecuter extends JRAbstractQueryExecuter {
	private static final Log log = LogFactory
			.getLog(JRHibernateQueryExecuter.class);
	protected static final String CANONICAL_LANGUAGE = "HQL";
	private static final Map<Class<?>, Type> hibernateTypeMap = new HashMap();
	private final Integer reportMaxCount;
	private Session session;
	private Query query;
	private boolean queryRunning;
	private ScrollableResults scrollableResults;
	private boolean isClearCache;

	public JRHibernate4QueryExecuter(JasperReportsContext jasperReportsContext,
			JRDataset dataset,
			Map<String, ? extends JRValueParameter> parameters) {
		super(jasperReportsContext, dataset, parameters);

		this.session = ((Session) getParameterValue("HIBERNATE_SESSION"));
		this.reportMaxCount = ((Integer) getParameterValue("REPORT_MAX_COUNT"));
		this.isClearCache = getPropertiesUtil().getBooleanProperty(dataset,
				"net.sf.jasperreports.hql.clear.cache", false);

		if (this.session == null) {
			log.warn("The supplied org.hibernate.Session object is null.");
		}

		parseQuery();
	}

	@Deprecated
	public JRHibernate4QueryExecuter(JRDataset dataset,
			Map<String, ? extends JRValueParameter> parameters) {
		this(DefaultJasperReportsContext.getInstance(), dataset, parameters);
	}

	protected String getCanonicalQueryLanguage() {
		return CANONICAL_LANGUAGE;
	}

	public JRDataSource createDatasource() throws JRException {
		JRDataSource datasource = null;
		String queryString = getQueryString();

		if ((this.session != null) && (queryString != null)
				&& (queryString.trim().length() > 0)) {
			createQuery(queryString);

			datasource = createResultDatasource();
		}

		return datasource;
	}

	protected JRDataSource createResultDatasource() {
		String runType = getPropertiesUtil().getProperty(this.dataset,
				"net.sf.jasperreports.hql.query.run.type");

		boolean useFieldDescriptions = getPropertiesUtil().getBooleanProperty(
				this.dataset,
				"net.sf.jasperreports.hql.field.mapping.descriptions", true);
		JRDataSource resDatasource;
		if ((runType == null) || (runType.equals("list"))) {
			try {
				int pageSize = getPropertiesUtil().getIntegerProperty(
						this.dataset,
						"net.sf.jasperreports.hql.query.list.page.size", 0);

				resDatasource = new JRHibernate4ListDataSource(this,
						useFieldDescriptions, pageSize);
			} catch (NumberFormatException e) {
				throw new JRRuntimeException(
						"The net.sf.jasperreports.hql.query.list.page.size property must be numerical.",
						e);
			}

		} else if (runType.equals("iterate")) {
			resDatasource = new JRHibernateIterateDataSource(this,
					useFieldDescriptions);
		} else {
			//JRDataSource resDatasource;
			if (runType.equals("scroll")) {
				resDatasource = new JRHibernateScrollDataSource(this,
						useFieldDescriptions);
			} else {
				throw new JRRuntimeException(
						"Unknown value for the net.sf.jasperreports.hql.query.run.type property.  Possible values are list, iterate and scroll.");
			}
		}
		
		return resDatasource;
	}

	protected synchronized void createQuery(String queryString) {
		if (log.isDebugEnabled()) {
			log.debug("HQL query: " + queryString);
		}

		Object filterCollection = getParameterValue("HIBERNATE_FILTER_COLLECTION");
		if (filterCollection == null) {
			this.query = this.session.createQuery(queryString);
		} else {
			this.query = this.session.createFilter(filterCollection,
					queryString);
		}
		this.query.setReadOnly(true);

		int fetchSize = getPropertiesUtil().getIntegerProperty(this.dataset,
				"net.sf.jasperreports.jdbc.fetch.size", 0);

		if (fetchSize != 0) {
			this.query.setFetchSize(fetchSize);
		}

		setParameters();
	}

	protected void setParameters() {
		List parameterNames = getCollectedParameterNames();

		if (parameterNames.isEmpty())
			return;
		Set namesSet = new HashSet();

		for (Iterator iter = parameterNames.iterator(); iter.hasNext();) {
			String parameterName = (String) iter.next();
			if (namesSet.add(parameterName)) {
				JRValueParameter parameter = getValueParameter(parameterName);
				setParameter(parameter);
			}
		}
	}

	protected void setParameter(JRValueParameter parameter) {
		String hqlParamName = getHqlParameterName(parameter.getName());
		Class clazz = parameter.getValueClass();
		Object parameterValue = parameter.getValue();

		if (log.isDebugEnabled()) {
			log.debug("Parameter " + hqlParamName + " of type "
					+ clazz.getName() + ": " + parameterValue);
		}


		Type type = hibernateTypeMap.get(clazz).newInstance();

		if (type != null) {
			this.query.setParameter(hqlParamName, parameterValue, type);
		} else if (Collection.class.isAssignableFrom(clazz)) {
			this.query.setParameterList(hqlParamName,
					(Collection) parameterValue);
		} else if (this.session.getSessionFactory().getClassMetadata(clazz) != null) {
			this.query.setEntity(hqlParamName, parameterValue);
		} else {
			this.query.setParameter(hqlParamName, parameterValue);
		}
	}

	public synchronized void close() {
		closeScrollableResults();

		this.query = null;
	}

	public void closeScrollableResults() {
		if (this.scrollableResults == null)
			return;
		try {
			this.scrollableResults.close();
		} finally {
			this.scrollableResults = null;
		}
	}

	public synchronized boolean cancelQuery() throws JRException {
		if (this.queryRunning) {
			this.session.cancelQuery();
			return true;
		}

		return false;
	}

	protected String getParameterReplacement(String parameterName) {
		return ':' + getHqlParameterName(parameterName);
	}

	protected String getHqlParameterName(String parameterName) {
		return '_' + JRStringUtil.getJavaIdentifier(parameterName);
	}

	public Type[] getReturnTypes() {
		return this.query.getReturnTypes();
	}

	public String[] getReturnAliases() {
		return this.query.getReturnAliases();
	}

	public JRDataset getDataset() {
		return super.dataset;
	}

	public List<?> list() {
		setMaxCount();

		setQueryRunning(true);
		try {
			List localList = this.query.list();

			return localList;
		} finally {
			setQueryRunning(false);
		}
	}

	protected synchronized void setQueryRunning(boolean queryRunning) {
		this.queryRunning = queryRunning;
	}

	private void setMaxCount() {
		if (this.reportMaxCount == null)
			return;
		this.query.setMaxResults(this.reportMaxCount.intValue());
	}

	public List<?> list(int firstIndex, int resultCount) {
		if ((this.reportMaxCount != null)
				&& (firstIndex + resultCount > this.reportMaxCount.intValue())) {
			resultCount = this.reportMaxCount.intValue() - firstIndex;
		}

		this.query.setFirstResult(firstIndex);
		this.query.setMaxResults(resultCount);
		if (this.isClearCache) {
			clearCache();
		}
		return this.query.list();
	}

	public Iterator<?> iterate() {
		setMaxCount();

		setQueryRunning(true);
		try {
			Iterator localIterator = this.query.iterate();

			return localIterator;
		} finally {
			setQueryRunning(false);
		}
	}

	public ScrollableResults scroll() {
		setMaxCount();

		setQueryRunning(true);
		try {
			this.scrollableResults = this.query.scroll(ScrollMode.FORWARD_ONLY);
		} finally {
			setQueryRunning(false);
		}

		return this.scrollableResults;
	}

	public void clearCache() {
		this.session.flush();
		this.session.clear();
	}

	static {
		hibernateTypeMap.put(Boolean.class, org.hibernate.type.BooleanType);
		hibernateTypeMap.put(Byte.class, org.hibernate.type.ByteType);
		hibernateTypeMap.put(Double.class, org.hibernate.type.DoubleType);
		hibernateTypeMap.put(Float.class, org.hibernate.type.FloatType);
		hibernateTypeMap.put(Integer.class, org.hibernate.type.IntegerType);
		hibernateTypeMap.put(Long.class, org.hibernate.type.LongType);
		hibernateTypeMap.put(Short.class, org.hibernate.type.ShortType);
		hibernateTypeMap.put(BigDecimal.class, org.hibernate.type.BigDecimalType);
		hibernateTypeMap.put(BigInteger.class, org.hibernate.type.BigIntegerType);
		hibernateTypeMap.put(Character.class, org.hibernate.type.CharacterType);
		hibernateTypeMap.put(String.class, org.hibernate.type.StringType);
		hibernateTypeMap.put(Date.class, org.hibernate.type.DateType);
		hibernateTypeMap.put(Timestamp.class, org.hibernate.type.TimestampType);
		hibernateTypeMap.put(Time.class, org.hibernate.type.TimeType);
//		hibernateTypeMap.put(Boolean.class, Hibernate.BOOLEAN);
//		hibernateTypeMap.put(Byte.class, Hibernate.BYTE);
//		hibernateTypeMap.put(Double.class, Hibernate.DOUBLE);
//		hibernateTypeMap.put(Float.class, Hibernate.FLOAT);
//		hibernateTypeMap.put(Integer.class, Hibernate.INTEGER);
//		hibernateTypeMap.put(Long.class, Hibernate.LONG);
//		hibernateTypeMap.put(Short.class, Hibernate.SHORT);
//		hibernateTypeMap.put(BigDecimal.class, Hibernate.BIG_DECIMAL);
//		hibernateTypeMap.put(BigInteger.class, Hibernate.BIG_INTEGER);
//		hibernateTypeMap.put(Character.class, Hibernate.CHARACTER);
//		hibernateTypeMap.put(String.class, Hibernate.STRING);
//		hibernateTypeMap.put(Date.class, Hibernate.DATE);
//		hibernateTypeMap.put(Timestamp.class, Hibernate.TIMESTAMP);
//		hibernateTypeMap.put(Time.class, Hibernate.TIME);
	}
}
