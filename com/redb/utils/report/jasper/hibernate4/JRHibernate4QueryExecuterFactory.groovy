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

import net.sf.jasperreports.engine.JRDataset
import net.sf.jasperreports.engine.JRException
import net.sf.jasperreports.engine.JRValueParameter
import net.sf.jasperreports.engine.JasperReportsContext
import net.sf.jasperreports.engine.query.AbstractQueryExecuterFactory
import net.sf.jasperreports.engine.query.JRQueryExecuter

class JRHibernate4QueryExecuterFactory extends
			AbstractQueryExecuterFactory {
 
	public static final String QUERY_LANGUAGE_HQL = "hql";
	public static final String PARAMETER_HIBERNATE_SESSION = "HIBERNATE_SESSION";
	public static final String PARAMETER_HIBERNATE_FILTER_COLLECTION = "HIBERNATE_FILTER_COLLECTION";
	private static final Object[] HIBERNATE_BUILTIN_PARAMETERS = [
			"HIBERNATE_SESSION", "org.hibernate.Session",
			"HIBERNATE_FILTER_COLLECTION", "java.lang.Object" ];
	public static final String PROPERTY_HIBERNATE_QUERY_RUN_TYPE = "net.sf.jasperreports.hql.query.run.type";
	public static final String PROPERTY_HIBERNATE_QUERY_LIST_PAGE_SIZE = "net.sf.jasperreports.hql.query.list.page.size";
	public static final String PROPERTY_HIBERNATE_CLEAR_CACHE = "net.sf.jasperreports.hql.clear.cache";
	public static final String PROPERTY_HIBERNATE_FIELD_MAPPING_DESCRIPTIONS = "net.sf.jasperreports.hql.field.mapping.descriptions";
	public static final String VALUE_HIBERNATE_QUERY_RUN_TYPE_LIST = "list";
	public static final String VALUE_HIBERNATE_QUERY_RUN_TYPE_ITERATE = "iterate";
	public static final String VALUE_HIBERNATE_QUERY_RUN_TYPE_SCROLL = "scroll";

	public Object[] getBuiltinParameters() {
		return HIBERNATE_BUILTIN_PARAMETERS;
	}

	public JRQueryExecuter createQueryExecuter(
			JasperReportsContext jasperReportsContext, JRDataset dataset,
			Map<String, ? extends JRValueParameter> parameters)
			throws JRException {
		return new JRHibernate4QueryExecuter(jasperReportsContext, dataset,
				parameters);
	}

	public boolean supportsQueryParameterType(String className) {
		return true;
	}
}
