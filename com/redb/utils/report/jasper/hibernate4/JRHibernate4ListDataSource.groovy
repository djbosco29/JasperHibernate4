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