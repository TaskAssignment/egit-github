package extractor;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public final class Dates {
	private static SimpleDateFormat isoFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
	static boolean timeZoneIsSet = false;
	private static void setTimeZone(){
	    TimeZone utc = TimeZone.getTimeZone("UTC");
	    isoFormatter.setTimeZone(utc);
	    timeZoneIsSet = true;
	}

	public static String toIsoString(Date d){
	    if (!timeZoneIsSet)
	    	setTimeZone();
		return isoFormatter.format(d)+"Z";
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
