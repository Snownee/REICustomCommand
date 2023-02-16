package snownee.rei_custom_command;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.shedaniel.rei.api.client.config.ConfigObject;
import me.shedaniel.rei.api.client.favorites.FavoriteEntryType.Registry;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.impl.client.gui.widget.search.OverlaySearchField;
import net.minecraft.network.chat.Component;

public class REICCPlugin implements REIClientPlugin {

	private static final Pattern PATTERN = Pattern.compile("^([^/\\\"][^/]*|\\\".+\\\")?/(.+)$", Pattern.MULTILINE);
	private static final Pattern FORMATTING_PATTERN = Pattern.compile("\\$([0-9a-f])");

	@Override
	public void registerFavorites(Registry registry) {
		registry.register(CustomCommandFavoriteEntry.ID, CustomCommandFavoriteEntry.Type.INSTANCE);
		registry.getOrCrateSection(Component.translatable(CustomCommandFavoriteEntry.TRANSLATION_KEY)).add(CustomCommandFavoriteEntry.DEFAULT);
	}

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
		Component title = null;
		if (titleStr != null && titleStr.startsWith("{")) {
			title = Component.Serializer.fromJson(titleStr);
		} else if (titleStr != null) {
			title = Component.literal(FORMATTING_PATTERN.matcher(titleStr).replaceAll("§$1"));
		} else {
			title = Component.empty();
		}
		CustomCommandFavoriteEntry entry = new CustomCommandFavoriteEntry(title, command);
		ConfigObject.getInstance().getFavoriteEntries().add(entry);
		searchField.setText("");
		return true;
	}

}
