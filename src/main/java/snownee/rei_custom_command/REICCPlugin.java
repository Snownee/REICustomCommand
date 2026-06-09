package snownee.rei_custom_command;

import java.util.List;
import java.util.regex.Pattern;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.StringReader;

import me.shedaniel.rei.RoughlyEnoughItemsCore;
import me.shedaniel.rei.api.client.config.ConfigObject;
import me.shedaniel.rei.api.client.favorites.FavoriteEntryType.Registry;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.impl.client.gui.widget.search.OverlaySearchField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("UnstableApiUsage")
public class REICCPlugin implements REIClientPlugin {

	private static final Pattern FORMATTING_PATTERN = Pattern.compile("(?i)\\$([0-9A-FK-OR])");

	public static boolean onPressEnterInSearch(OverlaySearchField searchField, int keyCode) {
		if (keyCode != InputConstants.KEY_RETURN && keyCode != InputConstants.KEY_NUMPADENTER) {
			return false;
		}
		if (!searchField.isVisible() || !searchField.isFocused() || !ConfigObject.getInstance().isFavoritesEnabled()) {
			return false;
		}
		ParsedResult result = parse(searchField.getText());
		if (result == null) {
			return false;
		}
		if (result.commands.size() > 10) {
			MutableComponent component = Component.translatable("rei_custom_command.too-many_commands");
			SystemToast.addOrUpdate(Minecraft.getInstance().getToasts(), SystemToast.SystemToastId.PACK_LOAD_FAILURE, component, null);
			return true;
		}
		String titleStr = result.title();
		ItemStack icon = ItemStack.EMPTY;
		Component title;
		if (titleStr.isEmpty()) {
			title = Component.empty();
		} else if (titleStr.startsWith("{")) {
			try {
				RegistryAccess.Frozen provider = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
				JsonObject jsonObject = GsonHelper.parse(titleStr, true);
				if (jsonObject.has("item")) {
					ItemParser parser = new ItemParser(provider);
					ItemParser.ItemResult itemResult = parser.parse(new StringReader(jsonObject.get("item").getAsString()));
					icon = new ItemStack(itemResult.item(), 1, itemResult.components());
				}
				title = Component.Serializer.fromJson(jsonObject, provider);
				if (title == null) {
					title = Component.empty();
				}
			} catch (Exception e) {
				if (icon.isEmpty()) {
					Minecraft mc = Minecraft.getInstance();
					SystemToast.SystemToastId ids = SystemToast.SystemToastId.UNSECURE_SERVER_WARNING; // make the toast persists longer
					SystemToast toast = SystemToast.multiline(
							mc,
							ids,
							Component.translatable("rei_custom_command.sth-wrong"),
							Component.literal(e.getLocalizedMessage()));
					mc.getToasts().addToast(toast);
					RoughlyEnoughItemsCore.LOGGER.error("Failed to parse custom command favorite entry title as JSON", e);
					return false;
				}
				title = Component.empty();
			}
		} else {
			title = Component.literal(FORMATTING_PATTERN.matcher(titleStr).replaceAll("§$1"));
		}
		CustomCommandFavoriteEntry entry = new CustomCommandFavoriteEntry(title, icon, result.commands());
		ConfigObject.getInstance().getFavoriteEntries().add(entry);
		if (!Screen.hasControlDown()) {
			searchField.setText("");
		}
		return true;
	}

	@Override
	public void registerFavorites(Registry registry) {
		registry.register(CustomCommandFavoriteEntry.ID, CustomCommandFavoriteEntry.Type.INSTANCE);
		registry.getOrCrateSection(Component.translatable(CustomCommandFavoriteEntry.TRANSLATION_KEY))
				.add(CustomCommandFavoriteEntry.DEFAULT);
	}

	public static ParsedResult parse(String input) {
		List<String> instructions = Lists.newArrayList();
		StringBuilder currentInstruction = new StringBuilder();
		boolean inDoubleQuotes = false;
		boolean escapeNext = false;

		for (char c : input.toCharArray()) {
			if (escapeNext) {
				currentInstruction.append(c);
				escapeNext = false;
				continue;
			}

			if (c == '\\') {
				escapeNext = true;
				continue;
			}

			if (c == '"') {
				inDoubleQuotes = !inDoubleQuotes;
				currentInstruction.append(c);
				continue;
			}

			if (c == '/' && !inDoubleQuotes) {
				instructions.add(currentInstruction.toString().trim());
				currentInstruction = new StringBuilder();
				continue;
			}

			currentInstruction.append(c);
		}

		if (escapeNext) {
			currentInstruction.append('\\');
		}

		if (!currentInstruction.isEmpty()) {
			instructions.add(currentInstruction.toString().trim());
		}

		if (instructions.size() < 2) {
			return null;
		}

		for (int i = 1; i < instructions.size(); i++) {
			if (instructions.get(i).isEmpty()) {
				return null;
			}
		}

		return new ParsedResult(instructions.getFirst(), instructions.subList(1, instructions.size()));
	}

	public record ParsedResult(String title, List<String> commands) {}
}
