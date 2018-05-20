package com.hs.java.pdf.service;

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.hs.java.pdf.SystemException;
import com.hs.java.pdf.dao.CheckInvoiceDAO;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;

/**
 * This class contains the business logic for parsing the PDF file
 * @since v1.0
 * @author Hasan Sunasara
 *
 */
public class PdfParserService {
	
	private static String CHECK_NO = "Check #:";
	private static int INVOICE_NUMBER_MIN_LENGTH = 6;
	private static int INVOICE_NUMBER_INDEX = 0;
	private static int INVOICE_DATE_INDEX = 1;
	private static int STORE_INDEX = 3;

	private CheckInvoiceDAO checkInvoiceDAO;
	
	/**
	 * Parses the PDF file containing the check and invoice data
	 * @param strPdfPath PDF file path
	 */
	public void parsePdf(String strPdfPath)
	{
		
		 PdfReader pdfReader;
		 StringBuilder strBuilder = new StringBuilder();
		 try {

	            pdfReader = new PdfReader(strPdfPath);
	            
	            //Iterate through all the PDF pages and read the text
	            for(int i = 1; i <= pdfReader.getNumberOfPages(); i++)
	            {
	            	strBuilder.append(PdfTextExtractor.getTextFromPage(pdfReader, i));
	            }
	                  
	            //Tokenize the string with new line character to separate each PDF line
	            StringTokenizer strTokenizer = new StringTokenizer(strBuilder.toString(), "\n");
	            
	            String strCheckNumber = null;
	            ArrayList<String[]> invoiceList = new ArrayList<String[]>();
	            
	            boolean bInvoiceDataBeingRead = false;
	            //Iterate through each PDF line to extract check number and invoice list
	            while (strTokenizer.hasMoreElements()) {
					String strPdfLine = (String) strTokenizer.nextElement();
					
					//Extract check number
					if(isCheckNumberLine(strPdfLine))
					{
						strCheckNumber = parseCheckNumber(strPdfLine);
						if((null == strCheckNumber) || (!StringUtils.isNumeric(strCheckNumber)))
						{
							System.err.println("ERROR: PDF doesn't contain valid check number.");
							System.exit(1);
						}
						else
						{
							continue;
						}
					}
					
					//Validate invoice data line and extract invoice fields
					String arrInvoiceFields[] = getInvoiceFields(strPdfLine);
					
					if(null != arrInvoiceFields)
					{
						/* When the pdf line is valid invoice data line, prepare the invoce fields list for database entry */
						
						/* Mark the begining of the invoice data in the pdf used to correctly process the invoice description 
						 * text wrapped to new line */
						bInvoiceDataBeingRead = true;
						
						//Process invoice fields for correctness
						arrInvoiceFields = processInvoiceFields(arrInvoiceFields);
						
						invoiceList.add(arrInvoiceFields);
					}
					else
					{
						/*When the pdf line is not a valid invoice data line that means it's the invoice description field text 
						 *which is wrapped to the new line */
						
						//Handle the invoice description wrapped to the newline 
						handleWrappedInvoiceDescription(invoiceList, bInvoiceDataBeingRead, strPdfLine);
					}
					
				}
	            
	            if(null == strCheckNumber)
	            {
	            	System.err.println("ERROR: Check number is missing in the PDF.");
					System.exit(1);
	            }
	            else
	            {
	            	saveInvoices(invoiceList, strCheckNumber);
	            }
	           
	            pdfReader.close();

	        } catch (Exception e) {
	            e.printStackTrace();
	            System.exit(1);
	        }

	}
	
	
	public void setCheckInvoiceDAO(CheckInvoiceDAO checkInvoiceDAO) {
		this.checkInvoiceDAO = checkInvoiceDAO;
	}
	
	private void handleWrappedInvoiceDescription(ArrayList<String[]> invoiceList, boolean bInvoiceDataBeingRead,
			String strPdfLine) {
		
		if(bInvoiceDataBeingRead)
		{
			/*Check that it is a valid description text wrapped on the next line but not the other text such as Date and page number at the footer, 
			 * table header, total at the end of the document etc.*/ 
			if(!isValidWrappedDescriptionText(strPdfLine))
			{
				return;
			}
			
			//Pick the last invoice data line and its field value array
			String arrInvFields[] = invoiceList.get(invoiceList.size()-1);
			
			String strInvDescription = arrInvFields[4];
			
			if(strInvDescription.endsWith("-"))
			{	
				/*If the description on the earlier line ends with "-" representing the date text, append the remaining 
				 * date text which is wrapped on new line wthout adding a white space */
				strInvDescription = strInvDescription + strPdfLine;
			}
			else
			{
				/*If the description on the earlier line ends with anything other than "-", append the text which is 
				 * wrapped on new line prefixed with a white space to make the whole description text readable and meaningful*/
				strInvDescription = strInvDescription + " " + strPdfLine;
			}
			arrInvFields[4] = strInvDescription;
		}
	}


	private boolean isValidWrappedDescriptionText(String strPdfLine) {
		
		if(null == strPdfLine)
		{
			return false;
		}
		
		//Check for the table headers and total
		if(strPdfLine.startsWith("Invoice# Invoice Date PO#") || strPdfLine.startsWith("Amount Amount")
				|| strPdfLine.startsWith("Total"))
		{
			
			return false;
		}
			
		if(strPdfLine.length() >= 20)
		{
			//Check for the date in the footer
			String strDateAtFooter = strPdfLine.substring(0, 20);
			if(Pattern.matches("(Mon|Tues|Wednes|Thurs|Fri|Satur|Sun)day, (Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec) [\\d]{1,3}, 20[\\d]{2}", strDateAtFooter))
			{
				return false;
			}
		}
		
		return true;
	}

	private String[] processInvoiceFields(String[] arrInvoiceFields) {

		/*Due to the description field text's varying  length, total number of invoice field tokens separated by white space is not fixed. 
		 *Hence, merge the description related tokens and copy in a new array. Also detect the three of the amount field values by 
		 *traversing from the end of the array to remove non digit characters from it.*/
			
		int iMaxFieldIndex = arrInvoiceFields.length - 1;
		String strAmount;
		
		String[] arrCorrectedInvoiceFields = new String[8];
		arrCorrectedInvoiceFields[0]=arrInvoiceFields[0];
		arrCorrectedInvoiceFields[1]=arrInvoiceFields[1];
		arrCorrectedInvoiceFields[2]=arrInvoiceFields[2];
		arrCorrectedInvoiceFields[3]=arrInvoiceFields[3];
		arrCorrectedInvoiceFields[4]=arrInvoiceFields[4];
		
		//Merge the tokens related to the invoice description to make one meaningful description text
		int iExtraTokens = arrInvoiceFields.length - 8;
		for(int i=0,j=5; i<iExtraTokens; i++,j++)
		{
			arrCorrectedInvoiceFields[4]=arrCorrectedInvoiceFields[4] + " " + arrInvoiceFields[j];
		}
		
		
		//Process Net Amount value to remove '$' and "," from the text
		strAmount = arrInvoiceFields[iMaxFieldIndex];
		strAmount = strAmount.replace("$", "");
		strAmount = strAmount.replace(",", "");
		arrCorrectedInvoiceFields[5] = strAmount;
		
		//Process Discount Amount value to remove '$' and "," from the text
		strAmount = arrInvoiceFields[iMaxFieldIndex - 1];
		strAmount = strAmount.replace("$", "");
		strAmount = strAmount.replace(",", "");
		arrCorrectedInvoiceFields[6] = strAmount;
		
		//Process Invoice Amount value to remove '$' and "," from the text
		strAmount = arrInvoiceFields[iMaxFieldIndex - 2];
		strAmount = strAmount.replace("$", "");
		strAmount = strAmount.replace(",", "");
		arrCorrectedInvoiceFields[7] = strAmount;
		
		
		return arrCorrectedInvoiceFields;
		
		
	}

	private void saveInvoices(ArrayList<String[]> invoiceList, String strCheckNumber) throws SystemException {
		
		if(null == invoiceList || invoiceList.size()==0)
		{
			System.out.println("INFO: PDF contains no invoices.");
			return;
		}
		
		try {
			
			if(!checkInvoiceDAO.isCheckNumberAvailable(strCheckNumber))
			{
				checkInvoiceDAO.saveCheck(strCheckNumber);
			}
			
			checkInvoiceDAO.saveInvoices(invoiceList, strCheckNumber);
			
		} catch (SystemException e) {
			System.err.println("ERROR: " + e.getMessage());
			throw e;
		}
	}

	private String[] getInvoiceFields(String strPdfLine) {
		if(null == strPdfLine)
		{
			return null;
		}
		
		String arrTokens[] = strPdfLine.split(" ");
		int iMaxTokenIndex = arrTokens.length - 1;
		if(arrTokens.length < 8)
		{
			return null;
		}
		
		if(!isValidInvoiceNumber(arrTokens[INVOICE_NUMBER_INDEX]) || !isValidInvoiceDateFormat(arrTokens[INVOICE_DATE_INDEX]) || !isValidStore(arrTokens[STORE_INDEX])
				|| !isValidAmount(arrTokens[iMaxTokenIndex - 2]) || !isValidAmount(arrTokens[iMaxTokenIndex - 1]) || !isValidAmount(arrTokens[iMaxTokenIndex]))
		{
			return null;
		}
		
		return arrTokens;
	}

	private boolean isValidAmount(String strAmount) {
		return strAmount.contains("$");
	}

	private boolean isValidInvoiceDateFormat(String strInvoiceDate) {
		return Pattern.matches("[\\d]{2}/[\\d]{2}/[\\d]{4}",strInvoiceDate);
	}

	private boolean isValidInvoiceNumber(String strInvoiceNumber) {
		return strInvoiceNumber.length()>=INVOICE_NUMBER_MIN_LENGTH;
	}

	private boolean isValidStore(String strStoreID) {
		return StringUtils.isNumeric(strStoreID);
	}

	private String parseCheckNumber(String strPdfLine) {
		return strPdfLine.substring(CHECK_NO.length(), strPdfLine.length()).trim();
	}

	private boolean isCheckNumberLine(String strPdfLine) {
		if((null != strPdfLine) && (strPdfLine.startsWith(CHECK_NO)))
		{
			return true;
		}
		return false;
	}

}
