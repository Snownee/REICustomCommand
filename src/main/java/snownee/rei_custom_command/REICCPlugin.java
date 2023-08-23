package snownee.rei_custom_command;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.shedaniel.rei.api.client.config.ConfigObject;
import me.shedaniel.rei.api.client.favorites.FavoriteEntryType.Registry;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.forge.REIPluginClient;
import me.shedaniel.rei.impl.client.gui.widget.search.OverlaySearchField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

@REIPluginClient
public class REICCPlugin implements REIClientPlugin {

	private static final Pattern PATTERN = Pattern.compile("^([^/\\\"][^/]*|\\\".+\\\")?/(.+)$", Pattern.MULTILINE);
	private static final Pattern FORMATTING_PATTERN = Pattern.compile("(?i)\\$([0-9A-FK-OR])");

	public static boolean onPressEnterInSearch(OverlaySearchField searchField, int keyCode) {
		if (keyCode != 257 && keyCode != 335) {
			return false;
		}
		if (!searchField.isVisible() || !searchField.isFocused() || !ConfigObject.getInstance().isFavoritesEnabled()) {
			return false;
		}
		Matcher matcher = PATTERN.matcher(searchField.getText());
		if (!matcher.find()) {
			return false;
		}
		String titleStr = matcher.group(1);
		String command = matcher.group(2);
		Component title;
		if (titleStr != null) {
			if (titleStr.startsWith("{")) {
				try {
					title = Component.Serializer.fromJson(titleStr);
				} catch (Exception e) {
					Minecraft mc = Minecraft.getInstance();
					SystemToast.SystemToastIds ids = SystemToast.SystemToastIds.UNSECURE_SERVER_WARNING; // make the toast persists longer
					SystemToast toast = SystemToast.multiline(mc, ids, Component.translatable("rei-custom-command.sth-wrong"), Component.literal(e.getLocalizedMessage()));
					mc.getToasts().addToast(toast);
					return false;
				}
			} else {
				title = Component.literal(FORMATTING_PATTERN.matcher(titleStr).replaceAll("ยง$1"));
			}
		} else {
			title = Component.empty();
		}
		CustomCommandFavoriteEntry entry = new CustomCommandFavoriteEntry(title, command);
		ConfigObject.getInstance().getFavoriteEntries().add(entry);
		searchField.setText("");
		return true;
	}

	@Override
	public void registerFavorites(Registry registry) {
		registry.register(CustomCommandFavoriteEntry.ID, CustomCommandFavoriteEntry.Type.INSTANCE);
		registry.getOrCrateSection(Component.translatable(CustomCommandFavoriteEntry.TRANSLATION_KEY)).add(CustomCommandFavoriteEntry.DEFAULT);
	}

}
