package de.webis.recipesearch.util;

import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class CustomLoggingFormatter extends Formatter {

    public String format(LogRecord record) {
        StringBuilder builder = new StringBuilder(1000);
//        builder.append(record.getMillis()).append("\t");
        builder.append(formatMessage(record) + ("\t"));
//        if (record.getParameters() != null) {
//        	String ipAddr = (String) record.getParameters()[0];
//        	builder.append(ipAddr);
//    	}
    	builder.append(("\n"));
        return builder.toString();
    }
    
//    public String getHead(Handler h) {
//       return "query type \t query string \n"; 
//    }

    /*public String getTail(Handler h) {
        return  "----------\t----------\n";
    }*/
};