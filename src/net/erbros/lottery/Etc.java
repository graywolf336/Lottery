package net.erbros.lottery;



public class Etc
{
	public static final int DAY = 60 * 60 * 24;
	public static final int HOUR = 60 * 60;
	public static final int MINUTE = 60;

	public static String formatCost(double cost, LotteryConfig lConfig)
	{
		return lConfig.formatCurrency((formatAmount(cost)));
	}

	public static double formatAmount(double amount)
	{
		return Math.floor(amount * 100) / 100;
	}

	public static String timeUntil(final long time, final boolean mini, LotteryConfig lConfig)
	{
		long timeLeft = time;
		// How many days left?
		String stringTimeLeft = "";

		if (timeLeft >= 60 * 60 * 24)
		{
			final int days = (int)Math.floor(timeLeft / (DAY));
			timeLeft -= 60 * 60 * 24 * days;
			if (mini)
			{
				stringTimeLeft += Integer.toString(days) + "d ";
			}
			else
			{
				stringTimeLeft += Integer.toString(days) + " " + lConfig.getPlural("day", days) + ", ";
			}
		}
		if (timeLeft >= 60 * 60)
		{
			final int hours = (int)Math.floor(timeLeft / (HOUR));
			timeLeft -= 60 * 60 * hours;
			if (mini)
			{
				stringTimeLeft += Integer.toString(hours) + "h ";
			}
			else
			{
				stringTimeLeft += Integer.toString(hours) + " " + lConfig.getPlural("hour", hours) + ", ";
			}
		}
		if (timeLeft >= 60)
		{
			final int minutes = (int)Math.floor(timeLeft / (MINUTE));
			timeLeft -= 60 * minutes;
			if (mini)
			{
				stringTimeLeft += Integer.toString(minutes) + "m ";

			}
			else
			{
				stringTimeLeft += Integer.toString(minutes) + " " + lConfig.getPlural("minute", minutes) + ", ";
			}
		}
		else
		{
			// Lets remove the last comma, since it will look bad with 2 days, 3
			// hours, and 14 seconds.
			if (!stringTimeLeft.equalsIgnoreCase("") && !mini)
			{
				stringTimeLeft = stringTimeLeft.substring(
						0, stringTimeLeft.length() - 1);
			}
		}
		final int secs = (int)timeLeft;
		if (mini)
		{
			stringTimeLeft += secs + "s";
		}
		else
		{
			if (!stringTimeLeft.equalsIgnoreCase(""))
			{
				stringTimeLeft += "and ";
			}
			stringTimeLeft += Integer.toString(secs) + " " + lConfig.getPlural("second", secs);
		}

		return stringTimeLeft;
	}

	public static int parseInt(final String arg)
	{
		int newInt = 0;
		try
		{
			newInt = Integer.parseInt(arg);
		}
		catch (NumberFormatException e)
		{
		}
		return newInt > 0 ? newInt : 0;
	}

	public static double parseDouble(final String arg)
	{
		double newDouble = 0;
		try
		{
			newDouble = Double.parseDouble(arg);
		}
		catch (NumberFormatException e)
		{
		}
		return newDouble > 0 ? newDouble : 0;
	}
}
