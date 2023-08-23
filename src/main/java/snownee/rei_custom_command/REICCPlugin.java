package snownee.rei_custom_command;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;

import me.shedaniel.rei.api.client.config.ConfigObject;
import me.shedaniel.rei.api.client.favorites.FavoriteEntryType.Registry;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.impl.client.gui.widget.search.OverlaySearchField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;

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
		ItemStack icon = ItemStack.EMPTY;
		Component title;
		if (titleStr != null) {
			if (titleStr.startsWith("{")) {
				try {
					JsonObject jsonObject = GsonHelper.parse(titleStr, true);
					if (jsonObject.has("item")) {
						JsonElement element = jsonObject.get("item");
						if (element.isJsonPrimitive()) {
							icon = net.minecraft.core.Registry.ITEM.get(new ResourceLocation(element.getAsString())).getDefaultInstance();
						} else {
							JsonObject itemObject = element.getAsJsonObject();
							if (!itemObject.has("Count")) {
								itemObject.addProperty("Count", 1);
							}
							icon = ItemStack.CODEC.parse(JsonOps.INSTANCE, element).getOrThrow(false, e -> {
							});
						}
					}
					title = Component.Serializer.fromJson(jsonObject);
				} catch (Exception e) {
					if (icon.isEmpty()) {
						Minecraft mc = Minecraft.getInstance();
						SystemToast.SystemToastIds ids = SystemToast.SystemToastIds.UNSECURE_SERVER_WARNING; // make the toast persists longer
						SystemToast toast = SystemToast.multiline(mc, ids, Component.translatable("rei-custom-command.sth-wrong"), Component.literal(e.getLocalizedMessage()));
						mc.getToasts().addToast(toast);
						return false;
					}
					title = Component.empty();
				}
			} else {
				title = Component.literal(FORMATTING_PATTERN.matcher(titleStr).replaceAll("ยง$1"));
			}
		} else {
			title = Component.empty();
		}
		CustomCommandFavoriteEntry entry = new CustomCommandFavoriteEntry(title, icon, command);
		ConfigObject.getInstance().getFavoriteEntries().add(entry);
		if (!Screen.hasControlDown())
			searchField.setText("");
		return true;
	}

	@Override
	public void registerFavorites(Registry registry) {
		registry.register(CustomCommandFavoriteEntry.ID, CustomCommandFavoriteEntry.Type.INSTANCE);
		registry.getOrCrateSection(Component.translatable(CustomCommandFavoriteEntry.TRANSLATION_KEY)).add(CustomCommandFavoriteEntry.DEFAULT);
	}

}
