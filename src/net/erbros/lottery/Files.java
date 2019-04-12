package net.erbros.lottery;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class Files {
	private Plugin plugin;
	private FileConfiguration tickets;
	private File ticketsFile;
	
	public Files(JavaPlugin plugin) {
		this.plugin = plugin;
		if (!plugin.getDataFolder().exists())
			plugin.getDataFolder().mkdir();
		
		this.setupTicketsFile();
	}
	
	public FileConfiguration getTickets() {
		return tickets;
	}
	
	public void saveTickets() {
		try {
            this.tickets.save(this.ticketsFile);
        } catch (final IOException e) {
            e.printStackTrace();
            plugin.getLogger().severe("The tickets file (tickets.yml) could NOT be saved.");
        }
	}
	
	public void reloadTickets() {
		this.tickets = YamlConfiguration.loadConfiguration(this.ticketsFile);
	}

	private void setupTicketsFile() {
		this.ticketsFile = new File(plugin.getDataFolder(), "tickets.yml");

		if (!this.ticketsFile.exists())
			copyFileFromJar("tickets.yml");
		
		this.reloadTickets();
	}	   
	
   // Copies a file from the JAR (including comments) and puts it into the plugins folder.
    private void copyFileFromJar(String fileName) {
        if (!fileName.endsWith(".yml")) {
            this.plugin.getLogger().severe("Invalid file to copy from jar: " + fileName);
            return;
        }

        File file = new File(plugin.getDataFolder() + File.separator + fileName);
        YamlConfiguration f = YamlConfiguration.loadConfiguration(new InputStreamReader(plugin.getResource(fileName)));

        try {
            f.save(file);
        } catch (IOException e) {
            e.printStackTrace();
            this.plugin.getLogger().severe("Failed to copy the file \"" + fileName + "\" from the plugin file.");
        }
    }
}
