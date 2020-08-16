package org.oddlama.imex.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.StringBuilder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.oddlama.imex.annotation.ConfigVersion;
import org.oddlama.imex.annotation.ConfigLong;
import org.oddlama.imex.annotation.ConfigString;

public abstract class Module extends JavaPlugin {
	private Core core;
	protected Logger log;

	public ConfigManager config_manager = new ConfigManager();
	public LangManager lang_manager = new LangManager();

	@ConfigVersion(desc = "DO NOT CHANGE! The version of this config file. Used to determine if the config needs to be updated.")
	public long version;

	@ConfigString(def = "inherit", desc = "DO NOT CHANGE! The version of this config file. Used to determine if the config needs to be updated.")
	public String lang;

	@Override
	public void onEnable() {
		// Get core plugin reference, important for inherited configuration
		if (this.getName().equals("imex-core")) {
			core = (Core)this;
		} else {
			core = (Core)getServer().getPluginManager().getPlugin("imex-core");
		}

		log = getLogger();
		if (!reload_configuration()) {
			// Force stop server, we encountered an invalid config file version
			log.severe("Invalid plugin configuration. Shutting down.");
			getServer().shutdown();
		}
	}

	public boolean reload_configuration() {
		// Get data directory
		var data_folder = getDataFolder();
		if (!data_folder.exists()) {
			 data_folder.mkdirs();
		}

		// Generate new file if not existing
		var file = new File(data_folder, "config.yml");
		if (!file.exists()) {
			var builder = new StringBuilder();
			config_manager.generate_yaml(builder);
			var contents = builder.toString();

			// Save contents to file
			try {
				Files.write(file.toPath(), contents.getBytes(StandardCharsets.UTF_8));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// Load config file
		var yaml = YamlConfiguration.loadConfiguration(file);

		// Check config file version
		var version = yaml.getLong("version", 0);
		if (!verify_config_version(file, version)) {
			return false;
		}

		// Reload automatic variables
		if (!config_manager.reload(log, yaml)) {
			return false;
		}

		// Reload localization
		if (!reload_localization()) {
			return false;
		}

		return true;
	}

	private List<String> getResourceFiles(String path) throws IOException {
		var filenames = new ArrayList<String>();

		try (
		    var in = getResourceAsStream(path);
		    var br = new BufferedReader(new InputStreamReader(in))) {
			String resource;
			while ((resource = br.readLine()) != null) {
				filenames.add(resource);
			}
		}

		return filenames;
	}

	private InputStream getResourceAsStream(String resource) {
		final var in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
		return in == null ? getClass().getResourceAsStream(resource) : in;
	}

	public boolean reload_localization() {
		// Get data directory
		var data_folder = getDataFolder();
		if (!data_folder.exists()) {
			 data_folder.mkdirs();
		}

		// Copy all embedded lang files, if version is greater.
		try {
			var f = getResourceFiles("/");
			f.stream().forEach(System.out::println);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Get configured language code
		var lang_code = lang;
		if ("inherit".equals(lang_code)) {
			lang_code = core.lang;

			// Fallback to en in case 'inherit' is used in imex-core.
			if ("inherit".equals(lang_code)) {
				lang_code = "en";
			}
		}

		// Generate new file if not existing
		var file = new File(data_folder, "lang-" + lang_code + ".yml");
		if (!file.exists()) {
			log.severe("");
			return false;
		}

		// Load config file
		var yaml = YamlConfiguration.loadConfiguration(file);

		// Check version
		var version = yaml.getLong("version", 0);
		if (!verify_lang_version(file, version)) {
			return false;
		}

		// Reload automatic variables
		if (!lang_manager.reload(log, yaml)) {
			return false;
		}

		return true;
	}

	private boolean verify_config_version(File file, long version) {
		if (version != config_manager.expected_version) {
			log.severe(file.getName() + ": expected version " + config_manager.expected_version + ", but got " + version);

			if (version == 0) {
				log.severe("Something went wrong while generating or loading the configuration.");
				log.severe("If you are sure your configuration is correct and this isn't a file");
				log.severe("system permission issue, please report this to https://github.com/oddlama/imex/issues");
			} else if (version < config_manager.expected_version) {
				log.severe("This config is for an older version of " + getName() + ".");
				log.severe("Please backup the file and delete it afterwards. It will");
				log.severe("then be regenerated the next time the server is started.");
			} else {
				log.severe("This config is for a future version of " + getName() + ".");
				log.severe("Please use the correct file for this version, or delete it and");
				log.severe("it will be regenerated next time the server is started.");
			}

			return false;
		}

		return true;
	}

	private boolean verify_lang_version(File file, long version) {
		if (version != lang_manager.expected_version) {
			log.severe(file.getName() + ": expected version " + lang_manager.expected_version + ", but got " + version);

			if (version == 0) {
				log.severe("Something went wrong while generating or loading the configuration.");
				log.severe("If you are sure your configuration is correct and this isn't a file");
				log.severe("system permission issue, please report this to https://github.com/oddlama/imex/issues");
			} else if (version < lang_manager.expected_version) {
				log.severe("This language file is for an older version of " + getName() + ".");
				log.severe("Please update your file or use an officially supported language file.");
			} else {
				log.severe("This language file is for a future version of " + getName() + ".");
				log.severe("Please use the correct file for this version, or use an officially");
				log.severe("supported language file.");
			}

			return false;
		}

		return true;
	}

	public void register_listener(Listener listener) {
		getServer().getPluginManager().registerEvents(listener, this);
	}

	public void schedule_task(Runnable task, long delay_ticks) {
		getServer().getScheduler().runTaskLater(this, task, delay_ticks);
	}

	public void schedule_next_tick(Runnable task) {
		getServer().getScheduler().runTask(this, task);
	}
}
