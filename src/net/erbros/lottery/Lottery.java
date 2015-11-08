package net.erbros.lottery;

import java.util.Random;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;


public class Lottery extends JavaPlugin
{
	private Economy economy = null;
	private Server server = null;
	private LotteryConfig lConfig;
	private LotteryGame lGame;
	private Files files;
	private BukkitTask drawTask;
	private boolean add;
	private long interval;
	private Random random;

	@Override
	public void onDisable()
	{
		// Disable all running timers.
		Bukkit.getServer().getScheduler().cancelTasks(this);
	}

	@Override
	public void onEnable()
	{

        if (!setupEconomy())
        {
            this.getLogger().severe("Economy API not found! Disabling Lottery...");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
		files = new Files(this);
		FileConfiguration config;
		lConfig = new LotteryConfig(this);
		lGame = new LotteryGame(this);
		// Lets find some configs
		config = getConfig();
		config.options().copyDefaults(true);
		saveConfig();
		lConfig.loadConfig();


		server = getServer();


		getCommand("lottery").setExecutor(new MainCommandExecutor(this));

		// Is the date we are going to draw the lottery set? If not, we should
		// do it.
		if (getNextexec() == 0)
		{
			// Set first time to be config hours later? Millisecs, * 1000.
			setNextexec(System.currentTimeMillis() + extendTime());
		}

		// Start the timer for the first time.
		startTimerSchedule(false);

		interval = lConfig.getBroadcastInterval() * 60 * 20l;
		random = new Random();
		runBroadcastTask();
	}

	private void runBroadcastTask()
	{
		// Make the broadcast not be exactly every x minutes.
		// Let's give it some variation to make the players not see a rigid interval.
		int extra = ((lConfig.getBroadcastInterval() * 20) * random.nextInt(20) + 1);
		new BukkitRunnable()
		{
			@Override
			public void run()
			{
				lGame.broadcastMessage("DrawIn", lGame.timeUntil(false));
				add = !add;
				runBroadcastTask();
			}
		}.runTaskLater(this, add ? interval + extra : interval - extra);
	}

	public Files getFiles()
	{
		return files;
	}

	public Server getBukkitServer()
	{
		return server;
	}

	public LotteryConfig getLotteryConfig()
	{
		return lConfig;
	}

	public LotteryGame getLotteryGame()
	{
		return lGame;
	}

	protected long getNextexec()
	{
		return lConfig.getNextexec();
	}

	protected void setNextexec(final long aNextexec)
	{
		lConfig.setNextexec(aNextexec);
	}

	public boolean isLotteryDue()
	{
		return getNextexec() > 0 && System.currentTimeMillis() + 1000 >= getNextexec();
	}

	public void startTimerSchedule(final boolean drawAtOnce)
	{
		long extendtime;
		// Cancel the draw task.
		if (drawTask != null)
		{
			drawTask.cancel();

			extendtime = extendTime();
		}
		else
		{
			// Get time until lottery drawing.
			extendtime = getNextexec() - System.currentTimeMillis();
		}
		// What if the admin changed the config to a shorter time? lets check,
		// and if
		// that is the case, lets use the new time.
		if (System.currentTimeMillis() + extendTime() < getNextexec())
		{
			setNextexec(System.currentTimeMillis() + extendTime());
		}

		// If the time is passed (perhaps the server was offline?), draw lottery
		// at once.
		if (extendtime <= 0)
		{
			extendtime = 1000;
		}

		// Is the drawAtOnce boolean set to true? In that case, do drawing in a
		// few secs.
		if (drawAtOnce)
		{
			extendtime = 100;
			setNextexec(System.currentTimeMillis() + 100);
		}

		// Delay in server ticks. 20 ticks = 1 second.
		extendtime = extendtime / 1000 * 20;
		runDrawTimer(extendtime);
	}

	public void lotteryDraw()
	{
		if (getNextexec() > 0 && System.currentTimeMillis() + 1000 >= getNextexec())
		{
			// Get the winner, if any, and clear to file in preparation for the next round.
			lGame.getWinner();
			setNextexec(System.currentTimeMillis() + extendTime());
		}
		// Call a new timer.
		startTimerSchedule(false);
	}

	public void extendLotteryDraw()
	{
		// Cancel any the draw task.
		if (drawTask != null)
		{
			drawTask.cancel();
		}

		long extendtime;

		// How much time left? Below 0?
		if (getNextexec() < System.currentTimeMillis())
		{
			extendtime = 3000;
		}
		else
		{
			extendtime = getNextexec() - System.currentTimeMillis();
		}
		// Delay in server ticks. 20 ticks = 1 second.
		extendtime = extendtime / 1000 * 20;
		runDrawTimer(extendtime);
	}

	private void runDrawTimer(final long extendtime)
	{
		// Is this very long until? On servers with lag and long between
		// restarts there might be a very long time between when server
		// should have drawn winner and when it will draw. Perhaps help the
		// server a bit by only scheduling for half the length at a time?
		// But only if its more than 5 seconds left.
		if (extendtime < 5 * 20)
		{
			drawTask = server.getScheduler().runTaskLaterAsynchronously(this, new LotteryDraw(this, true), extendtime);
		}
		else
		{
			final long newtime = extendtime / 10;
			drawTask = server.getScheduler().runTaskLaterAsynchronously(this, new LotteryDraw(this, false), newtime);
		}
	}

	public long extendTime()
	{
		final double exacttime = lConfig.getHours() * 60 * 60 * 1000;
		final long extendTime = (long)exacttime;
		return extendTime;
	}

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(Economy.class);
        if (economyProvider != null)
        {
            economy = economyProvider.getProvider();
        }
        return economy != null;
    }

	public Economy getEconomy()
	{
		return economy;
	}
}
