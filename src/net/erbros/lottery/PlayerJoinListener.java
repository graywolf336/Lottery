package net.erbros.lottery;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;


public class PlayerJoinListener implements Listener
{

	final private LotteryGame lGame;

	public PlayerJoinListener(final Lottery plugin)
	{
		lGame = plugin.getLotteryGame();
		plugin.getLotteryConfig();
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerJoin(final PlayerJoinEvent event)
	{
		// Send the player some info about time until lottery draw?
		lGame.sendMessage(event.getPlayer(),"Welcome");
	}
}
