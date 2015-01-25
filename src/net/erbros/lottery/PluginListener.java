package net.erbros.lottery;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import net.erbros.lottery.register.payment.Methods;


public class PluginListener implements Listener
{

	final private Lottery plugin;
	final private Methods Methods;

	public PluginListener(final Lottery plugin)
	{
		this.plugin = plugin;
		this.Methods = new Methods();
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPluginDisable(final PluginDisableEvent event)
	{
		if (this.Methods != null && net.erbros.lottery.register.payment.Methods.hasMethod())
		{
			final boolean check = net.erbros.lottery.register.payment.Methods.checkDisabled(event.getPlugin());

			if (check)
			{
				this.plugin.setMethod(null);
				System.out.println("[Lottery] Payment method was disabled. No longer accepting payments.");
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPluginEnable(final PluginEnableEvent event)
	{
		if (!net.erbros.lottery.register.payment.Methods.hasMethod())
		{
			if (net.erbros.lottery.register.payment.Methods.setMethod(Bukkit.getPluginManager()))
			{
				this.plugin.setMethod(net.erbros.lottery.register.payment.Methods.getMethod());
				System.out.println(
						"[Lottery] Payment method found (" + this.plugin.getMethod().getName() + " version: " + this.plugin.getMethod().getVersion() + ")");
			}
		}
	}
}