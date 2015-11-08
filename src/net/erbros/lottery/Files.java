package net.erbros.lottery;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.logging.Level;

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
		
		setupFile(ticketsFile, tickets, "tickets", "tickets.yml");
	}
	
	public FileConfiguration getTickets() {
		return tickets;
	}
	
	public void saveTickets() {
		save(ticketsFile, tickets, "tickets.yml");
	}
	
	public void reloadTickets() {
		tickets = YamlConfiguration.loadConfiguration(ticketsFile);
	}
	
	// Copies a file from the JAR (including comments) and puts it into the plugins folder.
	public void copyFileFromJar(String fileName) {
		File file = new File(plugin.getDataFolder() + File.separator + fileName);
		if (!file.exists())
			file.getParentFile().mkdirs();
		InputStream fis = plugin.getResource(fileName);
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		try {
			byte[] buf = new byte[1024];
			int i = 0;
			while ((i = fis.read(buf)) != -1) {
				fos.write(buf, 0, i);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	// Sets up a file, including saving and copying from local file.
	private void setupFile(File file, FileConfiguration fileTickets, String variableName, String fileName) {
		file = new File(plugin.getDataFolder(), fileName);
		if (!file.exists())
			copyFileFromJar(fileName);
		fileTickets = YamlConfiguration.loadConfiguration(file);
		try {
			Field field1 = getClass().getDeclaredField(variableName);
			Field field2 = getClass().getDeclaredField(variableName.concat("File"));
			field1.setAccessible(true);
			field2.setAccessible(true);
			field1.set(this, fileTickets);
			field2.set(this, file);
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}	    
	
	// Saves a file.
	private void save(File file, FileConfiguration fileTickets, String fileName) {
		try {
			fileTickets.save(file);
		} catch (final IOException e) {
			plugin.getLogger().log(Level.WARNING, "The file " + fileName + " couldn't be saved.");
		}
	}
}
