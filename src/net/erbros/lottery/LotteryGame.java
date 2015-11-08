package net.erbros.lottery;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.regex.Matcher;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.google.common.collect.Lists;


public class LotteryGame
{

	final private Lottery plugin;
	final private LotteryConfig lConfig;
	private List<String> winners;

	public LotteryGame(final Lottery plugin)
	{
		this.plugin = plugin;
		lConfig = plugin.getLotteryConfig();
		winners = plugin.getFiles().getTickets().getStringList("past winners");
		if (winners == null)
		{
			winners = new ArrayList<String>();
		}
	}

	public boolean addPlayer(final Player player, final int maxAmountOfTickets, final int numberOfTickets)
	{
		// Does the player have enough money?
		if (plugin.getEconomy().has(player, lConfig.getCost() * numberOfTickets))
		{
			plugin.getEconomy().withdrawPlayer(player, lConfig.getCost() * numberOfTickets);
		}
		else
		{
			return false;
		}

		// If the user paid, continue. Else we would already have sent returned false.

		int previousTickets = plugin.getFiles().getTickets().getInt("values." + player.getUniqueId());
		plugin.getFiles().getTickets().set("values." + player.getUniqueId(), previousTickets + numberOfTickets);
		plugin.getFiles().saveTickets();

		return true;
	}

	public int playerInList(final Player player)
	{
		return playerInList(player.getUniqueId());
	}

	public int playerInList(final UUID player)
	{
		return plugin.getFiles().getTickets().getInt("values." + player);
	}

	public List<UUID> playersInFile(String what)
	{
		List<UUID> ret = Lists.newArrayList();
		if (plugin.getFiles().getTickets().getConfigurationSection(what) == null)
		{
			return ret;
		}
		for (String uuidString : plugin.getFiles().getTickets().getConfigurationSection(what).getKeys(false))
		{
			int ticketAmount = plugin.getFiles().getTickets().getInt(what + "." + uuidString);
			UUID uuid = UUID.fromString(uuidString);
			for (int i = 0; i < ticketAmount; i++)
			{
				ret.add(uuid);
			}
		}

		return ret;
	}
	
	public List<String> playersStringInFile(String what)
	{
		List<String> ret = Lists.newArrayList();
		if (plugin.getFiles().getTickets().getConfigurationSection(what) == null)
		{
			return ret;
		}

		for (String uuidString : plugin.getFiles().getTickets().getConfigurationSection(what).getKeys(false))
		{
			int ticketAmount = plugin.getFiles().getTickets().getInt(what + "." + uuidString);
			for (int i = 0; i < ticketAmount; i++)
			{
				ret.add(uuidString);
			}
		}

		return ret;
	}

	public double winningAmount()
	{
		double amount;
		final List<String> players = playersStringInFile("values");
		amount = players.size() * Etc.formatAmount(lConfig.getCost());
		// Set the net payout as configured in the config.
		if (lConfig.getNetPayout() > 0)
		{
			amount = amount * lConfig.getNetPayout() / 100;
		}
		// Add extra money added by admins and mods?
		amount += lConfig.getExtraInPot();
		// Any money in jackpot?

		amount += lConfig.getJackpot();

		// format it once again.
		amount = Etc.formatAmount(amount);

		return amount;
	}

	public double taxAmount()
	{
		double amount = 0;

		// we only have tax is the net payout is between 0 and 100.
		if (lConfig.getNetPayout() >= 100 || lConfig.getNetPayout() <= 0)
		{
			return amount;
		}

		final List<String> players = playersStringInFile("values");
		amount = players.size() * Etc.formatAmount(lConfig.getCost());

		// calculate the tax.
		amount = amount * (1 - (lConfig.getNetPayout() / 100));

		// format it once again.
		amount = Etc.formatAmount(amount);

		return amount;
	}

	public int ticketsSold()
	{
		int sold;
		final List<String> players = playersStringInFile("values");
		sold = players.size();
		return sold;
	}

	public void addToWinnerList(final String playerName, final Double winningAmount)
	{
		List<String> winners = plugin.getFiles().getTickets().getStringList("past winners");
		if (winners == null)
		{
			winners = new ArrayList<String>();
		}
		winners.add(0, playerName + ":" + winningAmount);
		if (winners.size() > 9)
		{
			winners.remove(9);
		}
		plugin.getFiles().getTickets().set("past winners", winners);
		plugin.getFiles().saveTickets();
		this.winners = winners;
	}

	public List<String> getWinners()
	{
		return winners;
	}

	public long timeUntil()
	{
		final long nextDraw = lConfig.getNextexec();
		return ((nextDraw - System.currentTimeMillis()) / 1000);
	}

	public String timeUntil(final boolean mini)
	{
		final long timeLeft = timeUntil();
		// If negative number, just tell them its DRAW TIME!
		if (timeLeft < 0)
		{
			// Lets make it draw at once.. ;)
			plugin.startTimerSchedule(true);
			// And return some string to let the user know we are doing our best ;)
			if (mini)
			{
				return "Soon";
			}
			return "Draw will occur soon!";

		}

		return Etc.timeUntil(timeLeft, mini, lConfig);
	}

	public boolean getWinner()
	{
		final List<UUID> players = playersInFile("values");

		if (players.isEmpty())
		{
			broadcastMessage("NoWinnerTickets");
			return false;
		}
		else
		{
			// Find rand. Do minus 1 since its a zero based array.
			int rand;

			// is max number of tickets 0? If not, include empty tickets not sold.
			if (lConfig.getTicketsAvailable() > 0 && ticketsSold() < lConfig.getTicketsAvailable())
			{
				rand = new SecureRandom().nextInt(lConfig.getTicketsAvailable());
				// If it wasn't a player winning, then do some stuff. If it was a player, just continue below.
				if (rand > players.size() - 1)
				{
					// No winner this time, pot goes on to jackpot!
					final double jackpot = winningAmount();

					lConfig.setJackpot(jackpot);

					addToWinnerList("Jackpot", jackpot);
					lConfig.setLastwinner("Jackpot");
					lConfig.setLastwinneramount(jackpot);
					broadcastMessage("NoWinnerRollover", Etc.formatCost(jackpot, lConfig));
					clearAfterGettingWinner();
					return true;
				}
			}
			else
			{
				// Else just continue
				rand = new SecureRandom().nextInt(players.size());
			}



			double amount = winningAmount();
			OfflinePlayer winner = Bukkit.getOfflinePlayer(players.get(rand));
			int ticketsBought = playerInList(winner.getUniqueId());
			// Give the player his/her money:
			plugin.getEconomy().depositPlayer(Bukkit.getOfflinePlayer(players.get(rand)), amount);
			// Announce the winner:
			broadcastMessage("WinnerCongrat", winner.getName(), Etc.formatCost(amount, lConfig), ticketsBought, lConfig.getPlural("ticket", ticketsBought));
			addToWinnerList(winner.getName(), amount);
			broadcastMessage(
					"WinnerSummary", Etc.realPlayersFromList(players).size(), lConfig.getPlural(
              "player", Etc.realPlayersFromList(players).size()), players.size(), lConfig.getPlural("ticket", players.size()));

			// Add last winner to config.
			lConfig.setLastwinner(winner.getName());
			lConfig.setLastwinneramount(amount);

			lConfig.setJackpot(0);


			clearAfterGettingWinner();
		}
		return true;
	}

	public void clearAfterGettingWinner()
	{
		// extra money in pot added by admins and mods?
		// Should this be removed?
		if (lConfig.clearExtraInPot())
		{
			lConfig.setExtraInPot(0);
		}
		// Clear file.
		plugin.getFiles().getTickets().set("values", null);
		plugin.getFiles().saveTickets();
	}

	public void broadcastMessage(final String topic, final Object... args)
	{
		try
		{
			for (String message : lConfig.getMessage(topic))
			{
				String outMessage = formatCustomMessageLive(message, args);
				for (Player player : plugin.getServer().getOnlinePlayers())
				{
					outMessage = outMessage.replaceAll("%player%", player.getDisplayName());
					player.sendMessage(outMessage);
				}
			}
		}
		catch (Exception e)
		{
			plugin.getLogger().log(Level.WARNING, "Invalid Translation Key: " + topic, e);
		}
	}

	public void sendMessage(final CommandSender player, final String topic, final Object... args)
	{
		try
		{
			for (String message : lConfig.getMessage(topic))
			{
				String outMessage = formatCustomMessageLive(message, args);
				if (player instanceof Player) {
					outMessage = outMessage.replaceAll("%player%", Matcher.quoteReplacement(((Player)player).getDisplayName()));
				}
				player.sendMessage(outMessage);
			}
		}
		catch (Exception e)
		{
			plugin.getLogger().log(Level.WARNING, "Invalid Translation Key: " + topic, e);
		}
	}

	public String formatCustomMessageLive(final String message, final Object... args) throws Exception
	{
		//Lets give timeLeft back if user provie %draw%
		String outMessage = message.replaceAll("%draw%", Matcher.quoteReplacement(timeUntil(true)));

		//Lets give timeLeft with full words back if user provie %drawLong%
		outMessage = outMessage.replaceAll("%drawLong%", Matcher.quoteReplacement(timeUntil(false)));

		// %cost% = cost
		outMessage = outMessage.replaceAll("%cost%", Matcher.quoteReplacement(Etc.formatCost(lConfig.getCost(), lConfig)));

		// %pot%
		outMessage = outMessage.replaceAll("%pot%", Matcher.quoteReplacement(Etc.formatCost(winningAmount(), lConfig)));

		// %prefix%
		outMessage = outMessage.replaceAll("%prefix%", Matcher.quoteReplacement(lConfig.getMessage("prefix").get(0)));

		for (int i = 0; i < args.length; i++)
		{
			outMessage = outMessage.replaceAll("%" + i + "%", Matcher.quoteReplacement(args[i].toString()));
		}

		// Lets get some colors on this, shall we?
		outMessage = outMessage.replaceAll("(&([a-fk-or0-9]))", "\u00A7$2");
		return outMessage;
	}
}
