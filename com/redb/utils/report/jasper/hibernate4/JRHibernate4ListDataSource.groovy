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
import net.sf.jasperreports.engine.JRRewindableDataSource


class JRHibernate4ListDataSource extends JRHibernate4AbstractDataSource
		implements JRRewindableDataSource {
	private final int pageSize;
	private int pageCount;
	private boolean nextPage;
	private List<?> returnValues;
	private Iterator<?> iterator;

	public JRHibernate4ListDataSource(JRHibernate4QueryExecuter queryExecuter,
			boolean useFieldDescription, int pageSize) {
		super(queryExecuter, useFieldDescription, false);

		this.pageSize = pageSize;

		this.pageCount = 0;
		fetchPage();
	}

	protected void fetchPage() {
		if (this.pageSize <= 0) {
			this.returnValues = this.queryExecuter.list();
			this.nextPage = false;
		} else {
			this.returnValues = this.queryExecuter.list(this.pageCount
					* this.pageSize, this.pageSize);
			this.nextPage = (this.returnValues.size() == this.pageSize);
		}

		this.pageCount += 1;

		initIterator();
	}

	public boolean next() {
		if (this.iterator == null) {
			return false;
		}

		boolean hasNext = this.iterator.hasNext();
		if ((!(hasNext)) && (this.nextPage)) {
			fetchPage();
			hasNext = (this.iterator != null) && (this.iterator.hasNext());
		}

		if (hasNext) {
			setCurrentRowValue(this.iterator.next());
		}

		return hasNext;
	}

	public void moveFirst() {
		if (this.pageCount == 1) {
			initIterator();
		} else {
			this.pageCount = 0;
			fetchPage();
		}
	}

	private void initIterator() {
		this.iterator = ((this.returnValues == null) ? null : this.returnValues
				.iterator());
	}
}