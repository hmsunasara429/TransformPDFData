# TransformPDFData

Steps to build and run the project:
----------------------------------

- Import the project as a gradle project in to the Eclipse IDE
- Run the build task of gradle which would resolve the dependencies
- Create a postgres database and change the DB connection properties in the src/main/java/jdbc.properties file.
- Execute resources/database.sql file to create the table structure in the database. 
- Run main() method of HSTransformPDFData class which would parse the PDF file located at "resources/buyBuyBabyRemittance_866559.pdf" and transform the data in to the database.


A. Coding Convention:
--------------------

1. Variable Naming:

- String literals should be prefixed with "str" 
- Integer primitive should be prefixed with "i"
- Array variable should be prefixed with "arr"
- List type variable should be suffixed "list"

2. Code Commenting

- Logical comments should be added to help understand the business logic
- Javadoc should be added to public methods
- Javadoc comment is optional for the private methods

3. Exception Handling

- Code should be handled for possible exceptions
- Data operation methods should through SystemException (custom exception type)


B. Class Structure:
------------------

1. Application execution starts from the main method of HSTransformPDFData class
2. PdfParserService class contains the logic to parse the pdf file and extract check and invoice data
3. CheckInvoiceDAO class is responsible to perform database operations to insert check and invoice data

C. PDF Parsing 
--------------

1. Validation and Data Handling

- System would look for data line starting with "Check #:" on any of the pages
- System would correctly read the description text wrapping on the next line to make correct description field value

2. Assumptions

- PDF would contain a single instance of data line with numeric check number on any of the pages
- Invoice data table once begun, would contain only same data and continue until the end of the document without having any other data in between
- Only description field text is wrapped on the next line among the invoice data fields
- Invoice date text is only in a single format and valid date value 
- Check number is a unique, numeric value
- Store number is a numeric value
- Store number is pre-existing in the Store master table and is the primary key in that table. 
- PO# is pre-existing in the Purchase Order master table and is the primary key in that table. 
- The format of the date present at the footer of each page is same as "Monday, Jul 17, 2017" where date part is always two digit number.

Note: Store and Purchase Order tables are not designed as part of this POC, hence foreign keys for the respective fields in the invoice table are not created.

3. Tests performed

- Test blank PDF file
- Test PDF file without check number
- Test PDF file with check number but without invoice data table
