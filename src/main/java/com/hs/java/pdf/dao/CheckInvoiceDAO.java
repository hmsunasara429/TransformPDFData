package com.hs.java.pdf.dao;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

import com.hs.java.pdf.SystemException;

/**
 * This class is responsible to perform the database operations pertaining to the Check and Invoices.
 * @since v1.0
 * @author Hasan Sunasara
 *
 */
public class CheckInvoiceDAO {
	
	private static final String INVOICE_DATE_FORMAT = "MM/dd/yyyy";
	private JdbcTemplate jdbcTemplate;  

	/**
	 * Checks if particular check number exists in the 'check' table
	 * @param strCheckNumber Check number
	 * @return true if particular check number exists in the 'check' table, false otherwise
	 * @throws SystemException Database operation related system exception
	 */
	public boolean isCheckNumberAvailable(String strCheckNumber) throws SystemException
	{
		 
		
		String strSql = "SELECT COUNT(*) FROM \"check\" WHERE checknumber = ?";
		int iRowCount = 0;
		try {
			iRowCount = (int)jdbcTemplate.queryForObject(
					strSql, new Object[] { new Integer(strCheckNumber) }, Integer.class);
		}
		catch(Exception e)
		{
			SystemException systemException = new SystemException(e);
			systemException.setMessage("Some problem has occurred while reading from 'check' database table. Please contact the administrator.");
			throw systemException;
		}
		
		
		return (iRowCount>0);
	}

	/**
	 * Saves check number in the 'check' table
	 * @param strCheckNumber Check number
	 * @throws SystemException Database operation related system exception
	 */
	public void saveCheck(String strCheckNumber) throws SystemException{
		String strSql="INSERT INTO \"check\" VALUES('"+strCheckNumber+"')";  
		try {
			jdbcTemplate.update(strSql); 
		}
		catch(Exception e)
		{
			SystemException systemException = new SystemException(e);
			systemException.setMessage("Some problem has occurred while inserting in the 'check' database table. Please contact the administrator.");
			throw systemException;
		}
		 
	}

	/**
	 * Saves invoice data in the 'invoice' table
	 * @param invoiceList List of invoices to be saved
	 * @param strCheckNumber Check number against which the invoices should be saved
	 * @throws SystemException Database operation related system exception
	 */
	public void saveInvoices(final ArrayList<String[]> invoiceList, String strCheckNumber) throws SystemException{
		try {
				
			String strSql = "INSERT INTO invoice " +
				"(id, invoiceno, date, purchaseorder, store, description, invoiceamount, discountamount, netamount, checknumber) VALUES (nextval('invoice_seq'), ?, ?, ?, ?, ?, ?, ?, ?, ?)";
						
			jdbcTemplate.batchUpdate(strSql, new BatchPreparedStatementSetter() {
			
				private SimpleDateFormat dateFormat = new SimpleDateFormat(INVOICE_DATE_FORMAT);
				private Date invoiceDate = new Date(0);
				
				@Override
				public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
					try {
					String[] arrInvoiceFields = invoiceList.get(i);
					int iMaxFieldIndex = arrInvoiceFields.length - 1;
					
					preparedStatement.setString(1, arrInvoiceFields[0]);
					invoiceDate.setTime(dateFormat.parse(arrInvoiceFields[1]).getTime());
					preparedStatement.setDate(2, invoiceDate);
					preparedStatement.setString(3, arrInvoiceFields[2]);
					preparedStatement.setInt(4, Integer.parseInt(arrInvoiceFields[3]));
					preparedStatement.setString(5, arrInvoiceFields[4]);
					
					/*Due to the description filed text's varying  length, total number of invoice field tokens separated by white space is not fixed. 
					 *Hence, detect the three of the amount field values by traversing from the end of the array */
					preparedStatement.setDouble(6, Double.parseDouble(arrInvoiceFields[iMaxFieldIndex - 2]));
					preparedStatement.setDouble(7, Double.parseDouble(arrInvoiceFields[iMaxFieldIndex - 1]));
					preparedStatement.setDouble(8, Double.parseDouble(arrInvoiceFields[iMaxFieldIndex]));
					
					preparedStatement.setInt(9, Integer.parseInt(strCheckNumber));
					
					} catch (ParseException e) {
						System.err.println("ERROR: Some problem has occurred while parsing the date before inserting into the 'invoice' database table. Please contact the administrator.");
						e.printStackTrace();
						System.exit(1);
					}
					
				}
							
				@Override
				public int getBatchSize() {
					return invoiceList.size();
				}
		  });
			
		  System.out.println("SUCCESS: Invoices are saved successfully.");	
			
		} catch (Exception e) {
			SystemException systemException = new SystemException(e);
			systemException.setMessage("ERROR: Some problem has occurred while inserting in the 'invoice' database table. Please contact the administrator.");
			throw systemException;
		}
	}

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {  
	    this.jdbcTemplate = jdbcTemplate;  
	}  

}
