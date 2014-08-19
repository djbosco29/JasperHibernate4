JasperHibernate4
================

JasperReports 5.6.0 / Hibernate 4 compatibility.

Example of use:

		    1) Add the following line to the jasperreports.properties
		      
		      net.sf.jasperreports.query.executer.factory.hql=com.redb.utils.report.jasper.hibernate4.JRHibernate4QueryExecuterFactory

        2) In your code, simply use Hibernate session in JasperReports as usual:
        
				parameters[JRHibernate4QueryExecuterFactory.PARAMETER_HIBERNATE_SESSION] = session;
				JasperPrint jasperPrint = fillmgr.fill(this.jasperCompiledReport, parameters);

The source files can be easily converted to java.

If you have any question, you can contact to me at djbosco29@redbsistemas.com
Enjoy!
