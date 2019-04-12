package net.erbros.lottery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;


public class MainCommandExecutor implements CommandExecutor, TabCompleter
{

    final private static String[] userOptions = new String[] { "buy", "winners", "help" };
	final private Lottery plugin;
	final private LotteryConfig lConfig;
	final private LotteryGame lGame;

	public MainCommandExecutor(final Lottery plugin)
	{
		this.plugin = plugin;
		lConfig = plugin.getLotteryConfig();
		lGame = plugin.getLotteryGame();
	}

	@Override
	public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args)
	{

		// Can the player access the plugin?
		if (!sender.hasPermission("lottery.buy"))
		{
			lGame.sendMessage(sender, "ErrorAccess");
		}

		// If its just /lottery, and no args.
		if (args.length == 0)
		{
			commandNull(sender, args);
		}
		else if (args[0].equalsIgnoreCase("buy"))
		{
			commandBuy(sender, args);
		}
		else if (args[0].equalsIgnoreCase("winners"))
		{
			commandWinners(sender, args);
		}
		else if (args[0].equalsIgnoreCase("help"))
		{
			commandHelp(sender, args);
		}
		else if (args[0].equalsIgnoreCase("draw"))
		{
			if (sender.hasPermission("lottery.admin.draw"))
			{
				commandDraw(sender, args);
			}
			else
			{
				lGame.sendMessage(sender, "ErrorAccess");
			}
		}
		else if (args[0].equalsIgnoreCase("addtopot"))
		{
			if (sender.hasPermission("lottery.admin.addtopot"))
			{
				commandAddToPot(sender, args);
			}
			else
			{
				lGame.sendMessage(sender, "ErrorAccess");
			}
		}
		else
		{
			lGame.sendMessage(sender, "ErrorCommand");
		}

		return true;
	}

	public void commandNull(final CommandSender sender, final String[] args)
	{
		// Is this a console? If so, just tell that lottery is running and time until next draw.
		if (!(sender instanceof Player))
		{
			sender.sendMessage("Hi Console - The Lottery plugin is running");
			lGame.sendMessage(sender, "DrawIn", lGame.timeUntil(false));
			return;
		}
		final Player player = (Player)sender;

		// Send some messages:
		lGame.sendMessage(sender, "DrawIn", lGame.timeUntil(false));
		lGame.sendMessage(sender, "TicketCommand");
		lGame.sendMessage(sender, "PotAmount");
		if (lConfig.getMaxTicketsEachUser() > 1)
		{
			lGame.sendMessage(
					player, "YourTickets", lGame.playerInList(player), lConfig.getPlural("ticket", lGame.playerInList(player)));
		}
		// Number of tickets available?
		if (lConfig.getTicketsAvailable() > 0)
		{
			lGame.sendMessage(
					sender, "TicketRemaining", (lConfig.getTicketsAvailable() - lGame.ticketsSold()), lConfig.getPlural(
					"ticket", lConfig.getTicketsAvailable() - lGame.ticketsSold()));
		}
		lGame.sendMessage(sender, "CommandHelp");

		// Does lastwinner exist and != null?
		if (lConfig.getLastwinner() != null)
		{
			lGame.sendMessage(sender, "LastWinner", lConfig.getLastwinner(), Etc.formatCost(lConfig.getLastwinneramount(), lConfig));
		}
	}

	public void commandHelp(final CommandSender sender, final String[] args)
	{
		lGame.sendMessage(sender, "Help");
		// Are we dealing with admins?
		if (sender.hasPermission("lottery.admin.draw") || sender.hasPermission("lottery.admin.addtopot") || sender.hasPermission("lottery.admin.editconfig"))
		{
			lGame.sendMessage(sender, "HelpAdmin");
		}
	}

	public void commandBuy(final CommandSender sender, final String[] args)
	{
		// Is this a console? If so, just tell that lottery is running and time until next draw.
		if (!(sender instanceof Player))
		{
			lGame.sendMessage(sender, "ErrorConsole");
			return;
		}
		final Player player = (Player)sender;

		int buyTickets = 1;
		if (args.length > 1)
		{
			// How many tickets do the player want to buy?
			buyTickets = Etc.parseInt(args[1]);

			if (buyTickets < 1)
			{
				buyTickets = 1;
			}
		}

		final int allowedTickets = lConfig.getMaxTicketsEachUser() - lGame.playerInList(player);

		if (buyTickets > allowedTickets && allowedTickets > 0)
		{
			buyTickets = allowedTickets;
		}

		// Have the admin entered a max number of tickets in the lottery?
		if (lConfig.getTicketsAvailable() > 0)
		{
			// If so, can this user buy the selected amount?
			if (lGame.ticketsSold() + buyTickets > lConfig.getTicketsAvailable())
			{
				if (lGame.ticketsSold() >= lConfig.getTicketsAvailable())
				{
					lGame.sendMessage(sender, "ErrorNoAvailable");
					return;
				}
				else
				{
					buyTickets = lConfig.getTicketsAvailable() - lGame.ticketsSold();
				}
			}
		}

		int totalTicketsAfterBuy = lGame.playerInList(player) + buyTickets;
		if (lConfig.getMaxTicketsEachUser() > 0 && totalTicketsAfterBuy > lConfig.getMaxTicketsEachUser())
		{
			lGame.sendMessage(sender, "ErrorAtMax", lConfig.getMaxTicketsEachUser(), lConfig.getPlural("ticket", lConfig.getMaxTicketsEachUser()));
			return;
		}

		if (lGame.addPlayer(player, lConfig.getMaxTicketsEachUser(), buyTickets))
		{
			// You got your ticket.
			lGame.sendMessage(
					sender, "BoughtTicket", buyTickets, lConfig.getPlural("ticket", buyTickets), Etc.formatCost(lConfig.getCost() * buyTickets, lConfig));

			// Can a user buy more than one ticket? How many
			// tickets have he bought now?
			if (lConfig.getMaxTicketsEachUser() > 1)
			{
				lGame.sendMessage(
						sender, "BoughtTickets", lGame.playerInList(player), lConfig.getPlural("ticket", lGame.playerInList(player)));
			}
			if (lConfig.isBuyingExtendDeadline() && lGame.timeUntil() < lConfig.getBuyingExtendRemaining())
			{
				final long timeBonus = (long)(lConfig.getBuyingExtendBase() + (lConfig.getBuyingExtendMultiplier() * Math.sqrt(
						buyTickets)));
				lConfig.setNextexec(lConfig.getNextexec() + (timeBonus * 1000));
			}
			if (totalTicketsAfterBuy == lConfig.getMaxTicketsEachUser())
			{
				if (lGame.timeUntil() < lConfig.getBroadcastBuyingTime())
				{
					lGame.broadcastMessage(
							"BoughtAnnounceDraw", player.getDisplayName(), totalTicketsAfterBuy, lConfig.getPlural("ticket", totalTicketsAfterBuy), lGame.timeUntil(true));
				}
				else
				{
					lGame.broadcastMessage(
							"BoughtAnnounce", player.getDisplayName(), totalTicketsAfterBuy, lConfig.getPlural("ticket", totalTicketsAfterBuy));
				}
			}

		}
		else
		{
			// Something went wrong.
			lGame.sendMessage(sender,"ErrorNotAfford");
		}

	}

	public void commandDraw(final CommandSender sender, final String[] args)
	{
		// Start a timer that ends in 3 secs.
		lGame.sendMessage(sender,"DrawNow");
		plugin.startTimerSchedule(true);
	}

	public void commandWinners(final CommandSender sender, final String[] args)
	{
		// Get the winners.
		final List<String> winnerArray = lGame.getWinners();
		String[] split;
		String winListPrice;
		for (int i = 0; i < winnerArray.size(); i++)
		{
			split = winnerArray.get(i).split(":");
			winListPrice = plugin.getEconomy().format(Double.parseDouble(split[1]));
			sender.sendMessage((i + 1) + ". " + split[0] + " " + winListPrice);
		}
	}

	public void commandAddToPot(final CommandSender sender, final String[] args)
	{
		if (args.length < 2)
		{
			lGame.sendMessage(sender,"HelpPot");
			return;
		}

		final double addToPot = Etc.parseDouble(args[1]);

		if (addToPot == 0)
		{
			lGame.sendMessage(sender,"ErrorNumber");
			return;
		}
		lConfig.addExtraInPot(addToPot);
		lGame.sendMessage(sender,"AddToPot", addToPot, lConfig.getExtraInPot());
	}


    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("lottery.buy")) {
            return Collections.emptyList();
        }
        
        List<String> results = new ArrayList<String>();
        
        switch(args.length) {
            case 1:
                if (sender.hasPermission("lottery.admin.draw"))
                    if (args[0].isEmpty() || StringUtil.startsWithIgnoreCase("draw", args[0]))
                        results.add("draw");
                
                if (sender.hasPermission("lottery.admin.addtopot"))
                    if (args[0].isEmpty() || StringUtil.startsWithIgnoreCase("addtopot", args[0]))
                        results.add("addtopot");
                
                for (String s : userOptions)
                    if (args[0].isEmpty() || StringUtil.startsWithIgnoreCase(s, args[0]))
                        results.add(s);
                break;
            default:
                return results;
        }
        
        Collections.sort(results);
        
        return results;
    }
}
