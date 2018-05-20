package com.hs.java.pdf;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import com.hs.java.pdf.service.PdfParserService;

/**
 * This class is responsible to read PDF file, parse it and store its data in the database.
 * @since v1.0
 * @author Hasan Sunasara
 *
 */
public class HSTransformPDFData {
	
    public static void main(String[] args) {
        ApplicationContext applicationContext=new ClassPathXmlApplicationContext("applicationcontext.xml");  
        PdfParserService pdfParserService=(PdfParserService)applicationContext.getBean("pdfParserService");  
        pdfParserService.parsePdf("resources/buyBuyBabyRemittance_866559.pdf");
        
        System.out.println("INFO! PDF data parsing has been completed.");
    }

}