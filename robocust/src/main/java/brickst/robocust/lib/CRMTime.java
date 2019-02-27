package brickst.robocust.lib;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public final class CRMTime 
{
	public static final String DATE_FORMAT_1 = "MM-dd-yyyy";
	public static final String DATE_FORMAT_2 = "MM/dd/yyyy";
	public static final String DATE_FORMAT_3 = "MM/dd/yyyy h:mm:ss a";

	/** Days of the week for RFC822 dates */
	private static final String RFC822_DAYS[] =	{"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
	/** Months for RFC822 dates */
	private static final String RFC822_MONTHS[] = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
				   									"Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
	/** Calendar used to format RFC822 dates (also used for synchronization) */
    private static Calendar rfc822Calendar = new GregorianCalendar();
	/** Date object used for the current RFC822 date (also used for synchronization) */
	private static Date currentDate = new Date();
	/** Last time we formatted the RFC822 date (we don't want to update it every time) */
	private static long lastRfc822DateFormatTime = 0;
	/** The current formatted RFC822 date */
	private static String currentRfc822Date;
	
	public static Date getCurrentDate()
	{
		return(new Date());
	}
	
	public static long getCurrentMillis()
	{
		return(System.currentTimeMillis());
	}
	
	public static long getRealElapsed(Date dtBeg, Date dtEnd)
	{
		return(getRealElapsed(dtBeg.getTime(), dtEnd.getTime()));
	}
	
	public static long getRealElapsed(long msBeg, long msEnd)
	{
		return(Math.abs(msEnd-msBeg));
	}
	
	public static Date addDaysToDate(Date date, int days)
	{
		return(new Date(date.getTime() + (days * millisInDay)));
	}
	
	public static Date addMinutesToDate(Date date, int minutes)
	{
		//Bug # 59545: the date may be misscalculated if we don't make sure that 
		//minutes * 60 * 1000 is casted to long (instead of int as it did before).
		
		//return(new Date(date.getTime() + (minutes * 60 * 1000)));
		return(new Date(date.getTime() + ((long)minutes * 60 * 1000)));
	}

	private static final long millisInDay = 1000 * 60 * 60 * 24;
	
	public static Date truncToDay(Date date)
	{
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        
        return(cal.getTime());
	}
	
    public static Date truncToSeconds(Date date)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.MILLISECOND, 0);
        return(cal.getTime());
    }

	public static Date truncToWeek(Date date)
	{
		Calendar cal = Calendar.getInstance();
		cal.setTime(truncToDay(date));
		
		int weekOfYear = cal.get(Calendar.WEEK_OF_YEAR);
		while (cal.get(Calendar.WEEK_OF_YEAR) == weekOfYear)
			cal.add(Calendar.DATE, -1);
			
		cal.add(Calendar.DATE, 1);
		
		return(cal.getTime());
	}
	
	public static Date truncToMonth(Date date)
	{
		Calendar cal = Calendar.getInstance();
		cal.setTime(truncToDay(date));
		
		int month = cal.get(Calendar.MONTH);
		while (cal.get(Calendar.MONTH) == month)
			cal.add(Calendar.DATE, -1);
			
		cal.add(Calendar.DATE, 1);
		
		return(cal.getTime());
	}
	
	public static Date max(Date date1, Date date2)
	{
		if (date1 == null) return(date2);
		if (date2 == null) return(date1);
		return(date1.before(date2) ? date2 : date1);
	}
	
	public static Date min(Date date1, Date date2)
	{
		if (date1 == null) return(date2);
		if (date2 == null) return(date1);
		return(date1.after(date2) ? date2 : date1);
	}
	
	public static String getDateString()
	{ return(getDateString(getCurrentDate())); }
	
	public static String getDateString(long ms)
	{ return(getDateString(new Date(ms))); }
	
	public static String getDateString(Date date)
	{ return(getDateFormat().format(date));	}
	
	public synchronized static DateFormat getDateFormat()
	{
		// FUTURE: add additional signatures for styles and/or Locales
		return(DateFormat.getDateTimeInstance());		
	}
	
	public static String getDateString(Date date, String dateFormat)
	{
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
		return simpleDateFormat.format(date);
	}
	
	public static Date getDateFormated(String dateString, String dateFormat)
		throws ParseException
	{
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
		return simpleDateFormat.parse(dateString);
	}
	

    /**
     * Returns a properly formatted RFC822 date string.
	 * A typical date looks like: Tue, 21 Dec 1999 12:48:48 -0800
	 *
     * @param date the Date object to format
     */
    public static String getRfc822Date(Date date)
    {
		// Formats a date in RFC822 format of type
		// Date: Tue, 21 Dec 1999 12:48:48 -0800

        StringBuffer strDate = new StringBuffer(32);
		int weekDay, day, month, year, hour, minute, second, zone;

		// Reuse this calendar
        synchronized (rfc822Calendar)
        {
        	rfc822Calendar.setTime(date);

			weekDay = rfc822Calendar.get(Calendar.DAY_OF_WEEK);
			day 	= rfc822Calendar.get(Calendar.DAY_OF_MONTH);
			month 	= rfc822Calendar.get(Calendar.MONTH);
			year 	= rfc822Calendar.get(Calendar.YEAR);
			hour 	= rfc822Calendar.get(Calendar.HOUR_OF_DAY);
			minute 	= rfc822Calendar.get(Calendar.MINUTE);
			second 	= rfc822Calendar.get(Calendar.SECOND);
			// Get time-zone adjusted for day-light savings time
			TimeZone tz = rfc822Calendar.getTimeZone();
			zone 	= tz.getOffset(rfc822Calendar.get(Calendar.ERA),
						year, month, day, weekDay,
						rfc822Calendar.get(Calendar.MILLISECOND));
		}

		// Weekday
		strDate.append(RFC822_DAYS[weekDay-1]).append(", ");

		// Day of month
		strDate.append(day).append(' ');

		// Month
		strDate.append(RFC822_MONTHS[month]).append(' ');

		// Year
		strDate.append(year).append(' ');

		// Hour
		if (hour < 10)
			strDate.append('0');
		strDate.append(hour).append(':');

		// Minutes
		if (minute < 10)
			strDate.append('0');
		strDate.append(minute).append(':');

		// Seconds
		if (second < 10)
			strDate.append('0');
		strDate.append(second).append(' ');

		// Time zone offset
		if (zone < 0) {
			strDate.append('-');
			zone = -zone;
		}
		else
			strDate.append('+');
		// Zone offset hours
		int zoneHours = zone/3600000;
		if (zoneHours < 10)
			strDate.append('0');
		strDate.append(zoneHours);
		// Zone offset minutes
		int zoneMinutes = zone/60000 - zoneHours*60;
		if (zoneMinutes < 10)
			strDate.append('0');
		strDate.append(zoneMinutes);

        return(strDate.toString());
    }

    /**
     * Returns a properly formatted RFC822 date string using the current time.
	 * A typical date looks like: Tue, 21 Dec 1999 12:48:48 -0800
     */
    public static String getCurrentRfc822Date()
	{
		long now = getCurrentMillis();

		// Redo the date no more than once every second
		if (now - lastRfc822DateFormatTime > 1000) {
			// Only allow one thread at a time to update this
			synchronized (currentDate)
			{
				if (now - lastRfc822DateFormatTime > 1000) {
					currentDate.setTime(now);
					currentRfc822Date = getRfc822Date(currentDate);
					lastRfc822DateFormatTime = now;
				}
			}
		}

		return currentRfc822Date;
	}

	
	// ******* for testing only ********
	
	public static void main(String [] args)
	{
		long ms = System.currentTimeMillis();
		for (int i = 0; i < 100; i++) {
			ms += (long) (Math.random() * millisInDay);
			Date date = new Date(ms);
			System.err.println("Before: " + getTime(date) + " | after: " + getTime(truncToDay(date)));
		}
	}

	private static String getTime(Date date)
	{
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return(date.toString() + " (" + date.getTime() + ")");
	}
	
	public static int getDateOfMonth(Date date)
	{
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		return calendar.DAY_OF_MONTH;
	}
	
	/**
	 * return an calender's month as an int. <br>
	 *
	 * In Java, calender's month is from 0 to 11, this method
	 * will change it to 1 to 12.
	 */
	public static int getCalenderMonth(Calendar calendar)
	{
		switch (calendar.get(Calendar.MONTH)) {
			case Calendar.JANUARY:
				return 1;
			case Calendar.FEBRUARY:
				return 2;
			case Calendar.MARCH:
				return 3;
			case Calendar.APRIL:
				return 4;
			case Calendar.MAY:
				return 5;
			case Calendar.JUNE:
				return 6;
			case Calendar.JULY:
				return 7;
			case Calendar.AUGUST:
				return 8;
			case Calendar.SEPTEMBER:
				return 9;
			case Calendar.OCTOBER:
				return 10;
			case Calendar.NOVEMBER:
				return 11;
			case Calendar.DECEMBER:
				return 12;
			default:
				return 0;
		}
	}
}
