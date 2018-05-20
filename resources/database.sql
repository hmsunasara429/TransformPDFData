CREATE TABLE public."check"
(
   checknumber integer NOT NULL, 
   CONSTRAINT pk_checknumber PRIMARY KEY (checknumber)
) 
WITH (
  OIDS = FALSE
)
;


CREATE TABLE public.invoice
(
   id integer NOT NULL,	
   invoiceno text NOT NULL, 
   date date NOT NULL, 
   purchaseorder text, 
   store integer NOT NULL, 
   description text, 
   invoiceamount double precision NOT NULL, 
   discountamount double precision NOT NULL, 
   netamount double precision NOT NULL, 
   checknumber integer NOT NULL, 
   CONSTRAINT pk_id PRIMARY KEY (id),
   CONSTRAINT fk_checknumber FOREIGN KEY (checknumber) REFERENCES public."check" (checknumber) ON UPDATE CASCADE ON DELETE CASCADE
) 
WITH (
  OIDS = FALSE
)
;

CREATE SEQUENCE invoice_seq START 1;



