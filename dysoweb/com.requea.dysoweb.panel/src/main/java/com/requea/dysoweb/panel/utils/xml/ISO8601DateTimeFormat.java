package com.requea.dysoweb.panel.utils.xml;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Format and parse an ISO 8601 DateTimeFormat used in XML documents.
 * This lexical representation is the ISO 8601 extended format CCYY-MM-DDThh:mm:ss 
 * where "CC" represents the century, "YY" the year, "MM" the month and "DD" the day,
 * preceded by an optional leading "-" sign to indicate a negative number. 
 * If the sign is omitted, "+" is assumed. 
 * The letter "T" is the date/time separator and "hh", "mm", "ss" represent hour, minute and second respectively. 
 * This representation may be immediately followed by a "Z" to indicate Coordinated Universal Time (UTC) or, 
 * to indicate the time zone, i.e. the difference between the local time and Coordinated Universal Time, 
 * immediately followed by a sign, + or -, followed by the difference from UTC represented as hh:mm. 
 * 
 */
public class ISO8601DateTimeFormat extends DateFormat {

	/**
     * Comment for <code>serialVersionUID</code>
     */
    private static final long serialVersionUID = 228536525190863606L;
    private boolean dateOnly = false;

    public ISO8601DateTimeFormat() {
        setCalendar(Calendar.getInstance(TimeZone.getTimeZone("GMT")));
    }
    
    public ISO8601DateTimeFormat(boolean dateonly) {
    	dateOnly = dateonly;
    	if(dateOnly == false) {
    		setCalendar(Calendar.getInstance(TimeZone.getTimeZone("GMT")));
    	} else {
    		// use default server timezone
    		setCalendar(Calendar.getInstance());
    	}
    }
    
    public ISO8601DateTimeFormat(TimeZone tz) {
        setCalendar(Calendar.getInstance(tz));
    }

    /**
     * @see DateFormat#parse(String, ParsePosition)
     */
    public Date parse(String text, ParsePosition pos) {

        int i = pos.getIndex();

        try {
            int year = Integer.valueOf(text.substring(i, i + 4)).intValue();
            i += 4;

            if (text.charAt(i) != '-') {
                throw new NumberFormatException();
            }
            i++;

            int month = Integer.valueOf(text.substring(i, i + 2)).intValue() - 1;
            i += 2;

            if (text.charAt(i) != '-') {
                throw new NumberFormatException();
            }
            i++;

            int day = Integer.valueOf(text.substring(i, i + 2)).intValue();
            i += 2;

            // check if we are done (day only format)
            int hour = 0;
            int mins = 0;
            int secs = 0;
            if(!dateOnly && i < text.length()) {
                
                if (text.charAt(i) != 'T' && text.charAt(i) != ' ') {
                    throw new NumberFormatException();
                }
                i++;
    
                hour = Integer.valueOf(text.substring(i, i + 2)).intValue();
                i += 2;
    
                if (text.charAt(i) != ':') {
                    throw new NumberFormatException();
                }
                i++;
    
                mins = Integer.valueOf(text.substring(i, i + 2)).intValue();
                i += 2;
    
    			secs = 0;
                if (i < text.length() && text.charAt(i) == ':') {
                	// handle seconds flexible
    	            i++;
    
    	            secs = Integer.valueOf(text.substring(i, i + 2)).intValue();
     	           	i += 2;
                }
            }
            calendar.set(year, month, day, hour, mins, secs);
            calendar.set(Calendar.MILLISECOND, 0); // no parts of a second

            if (!dateOnly) {
            	int idx = text.indexOf('Z', i);
            	if(idx < 0) {
            		idx = text.indexOf('+', i);
            	}
            	if(idx < 0) {
            		idx = text.indexOf('-', i);
            	}
            	if(idx > 0)
            		i = parseTZ(idx, text);
            }

        }
        catch (NumberFormatException ex) {
        	pos.setErrorIndex(i);
            return null;
        }
        catch (IndexOutOfBoundsException ex) {
        	pos.setErrorIndex(i);
            return null;
        }
        finally {
        	pos.setIndex(i);
        }

        return calendar.getTime();
    }

	protected int parseTZ(int i, String text) throws NumberFormatException {
		if (i < text.length()) {
			// check and handle the zone/dst offset       
			int offset = 0;
			if (text.charAt(i) == 'Z') {
		    	offset = 0;
		    	i++;
			}
			else {
		    	int sign = 1;
		    	if (text.charAt(i) == '-') {
		        	sign = -1;
		    	}
		    	else if (text.charAt(i) != '+') {
		        	throw new NumberFormatException();
		    	}
		    	i++;
		
		    	int offset_h = Integer.valueOf(text.substring(i, i + 2)).intValue();
		    	i += 2;
		
		    	if (text.charAt(i) != ':') {
		        	throw new NumberFormatException();
		    	}
		    	i++;
		
		    	int offset_min = Integer.valueOf(text.substring(i, i + 2)).intValue();
		    	i += 2;
		    	offset = ((offset_h * 60) + offset_min) * 60000 * sign;
			}
			int offset_cal =
		    	calendar.get(Calendar.ZONE_OFFSET) + calendar.get(Calendar.DST_OFFSET);
		
			calendar.add(Calendar.MILLISECOND, offset_cal - offset);
		}
		return i;
	}

    /**
     * @see DateFormat#format(Date, StringBuffer, FieldPosition)
     */
    public StringBuffer format(
        Date date,
        StringBuffer sbuf,
        FieldPosition fieldPosition) {

        calendar.setTime(date);

		writeCCYYMM(sbuf);

		if (!dateOnly) {
	        sbuf.append('T');
	
			writehhmmss(sbuf);
	
			writeTZ(sbuf);
		}
        
        return sbuf;
    }

	protected void writeTZ(StringBuffer sbuf) {
		int offset =
		    calendar.get(Calendar.ZONE_OFFSET) + calendar.get(Calendar.DST_OFFSET);
		if (offset == 0) {
		    sbuf.append('Z');
		}
		else {
		    int offset_h = offset / 3600000;
		    int offset_min = (offset % 3600000) / 60000;
		    if (offset >= 0) {
		        sbuf.append('+');
		    }
		    else {
		        sbuf.append('-');
		        offset_h = 0 - offset_h;     
		        offset_min = 0 - offset_min;
		    }
		    appendInt(sbuf, offset_h, 2);
		    sbuf.append(':');
			appendInt(sbuf, offset_min, 2);
		}
	}

	protected void writehhmmss(StringBuffer sbuf) {
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		appendInt(sbuf, hour, 2);
		sbuf.append(':');
		
		int mins = calendar.get(Calendar.MINUTE);
		appendInt(sbuf, mins, 2);
		sbuf.append(':');
		
		int secs = calendar.get(Calendar.SECOND);
		appendInt(sbuf, secs, 2);
	}
    
	protected void writeCCYYMM(StringBuffer sbuf) {
		int year = calendar.get(Calendar.YEAR);
		appendInt(sbuf, year, 4);
		
		String month;
		switch (calendar.get(Calendar.MONTH)) {
		    case Calendar.JANUARY :
		        month = "-01-";
		        break;
		    case Calendar.FEBRUARY :
		        month = "-02-";
		        break;
		    case Calendar.MARCH :
		        month = "-03-";
		        break;
		    case Calendar.APRIL :
		        month = "-04-";
		        break;
		    case Calendar.MAY :
		        month = "-05-";
		        break;
		    case Calendar.JUNE :
		        month = "-06-";
		        break;
		    case Calendar.JULY :
		        month = "-07-";
		        break;
		    case Calendar.AUGUST :
		        month = "-08-";
		        break;
		    case Calendar.SEPTEMBER :
		        month = "-09-";
		        break;
		    case Calendar.OCTOBER :
		        month = "-10-";
		        break;
		    case Calendar.NOVEMBER :
		        month = "-11-";
		        break;
		    case Calendar.DECEMBER :
		        month = "-12-";
		        break;
		    default :
		        month = "-NA-";
		        break;
		}
		sbuf.append(month);
		
		int day = calendar.get(Calendar.DAY_OF_MONTH);
		appendInt(sbuf, day, 2);
	}
	
	protected void appendInt(StringBuffer buf, int value, int length) {
		int len1 = buf.length();
		buf.append(value);
		int len2 = buf.length();
		for (int i = len2; i < len1 + length; ++i) {
			buf.insert(len1, '0');
		}
	}
}
