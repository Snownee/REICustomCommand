package snownee.rei_custom_command;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;

import dev.architectury.platform.Platform;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.favorites.FavoriteEntry;
import me.shedaniel.rei.api.client.favorites.FavoriteEntryType;
import me.shedaniel.rei.api.client.gui.AbstractRenderer;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Tooltip;
import me.shedaniel.rei.api.client.gui.widgets.TooltipContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;

public class CustomCommandFavoriteEntry extends FavoriteEntry {

	public static final ResourceLocation ID = new ResourceLocation("rei_custom_command", "custom-command");
	public static final String TRANSLATION_KEY = "favorite.section.rei-custom-command";
	public static final FavoriteEntry DEFAULT = new CustomCommandFavoriteEntry(Component.literal("?"), "");

	private final Component title;
	private final String command;

	public CustomCommandFavoriteEntry(Component title, String command) {
		this.title = title;
		this.command = command;
	}

	@Override
	public boolean isInvalid() {
		return this.equals(DEFAULT);
	}

	@Override
	public Renderer getRenderer(boolean showcase) {
		return new AbstractRenderer() {
			@Override
			public void render(PoseStack matrices, Rectangle bounds, int mouseX, int mouseY, float delta) {
				int color = bounds.contains(mouseX, mouseY) ? 0xFFEEEEEE : 0xFFAAAAAA;
				if (bounds.width > 4 && bounds.height > 4) {
					Font font = Minecraft.getInstance().font;
					Component component = title.getString().isBlank() ? Component.literal(command) : title;
					List<FormattedCharSequence> lines = font.split(component, bounds.getWidth());
					if (lines.isEmpty())
						return;
					matrices.pushPose();
					matrices.translate(bounds.getCenterX(), bounds.getCenterY(), 0);
					matrices.scale(bounds.getWidth() / 18f, bounds.getHeight() / 18f, 1);
					font.draw(matrices, lines.get(0), -font.width(lines.get(0)) / 2f + 0.5f, -3.5f, color);
					matrices.popPose();
				}
			}

			@Override
			@Nullable
			public Tooltip getTooltip(TooltipContext context) {
				List<Component> tooltip;
				if (CustomCommandFavoriteEntry.this.equals(DEFAULT)) {
					tooltip = Stream.of(I18n.get(TRANSLATION_KEY + ".tip").split("\n")).map(Component::literal).map(Component.class::cast).toList();
				} else {
					Font font = Minecraft.getInstance().font;
					String cutCommand = "/" + font.plainSubstrByWidth(command, 200);
					if (title.getString().isBlank()) {
						tooltip = List.of(Component.literal(cutCommand));
					} else {
						tooltip = List.of(title, Component.literal(cutCommand));
					}
				}
				return Tooltip.create(context.getPoint(), tooltip);
			}

			@Override
			public boolean equals(Object o) {
				if (this == o)
					return true;
				if (o == null || getClass() != o.getClass())
					return false;
				return hashCode() == o.hashCode();
			}

			@Override
			public int hashCode() {
				return title.hashCode();
			}
		};
	}

	@Override
	public boolean doAction(int button) {
		if (button != 0)
			return false;
		CommandSender.sendCommand(command);
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
		return true;
	}

	@Override
	public long hashIgnoreAmount() {
		return Objects.hash(title, command);
	}

	@Override
	public FavoriteEntry copy() {
		return this;
	}

	@Override
	public ResourceLocation getType() {
		return ID;
	}

	@Override
	public boolean isSame(FavoriteEntry other) {
		if (!(other instanceof CustomCommandFavoriteEntry that))
			return false;
		return Objects.equals(title, that.title) && Objects.equals(command, that.command);
	}

	public enum Type implements FavoriteEntryType<CustomCommandFavoriteEntry> {
		INSTANCE;

		@Override
		public DataResult<CustomCommandFavoriteEntry> read(CompoundTag tag) {
			String command = tag.getString("command");
			Component title = null;
			try {
				title = Component.Serializer.fromJson(tag.getString("title"));
			} catch (Exception exception) {
			}
			if (title == null) {
				return DataResult.error("Cannot create CustomCommandFavoriteEntry!");
			}
			return DataResult.success(new CustomCommandFavoriteEntry(title, command), Lifecycle.stable());
		}

		@Override
		public DataResult<CustomCommandFavoriteEntry> fromArgs(Object... args) {
			if (args.length < 2 || !(args[0] instanceof Component title) || !(args[0] instanceof String command))
				return DataResult.error("Cannot create CustomCommandFavoriteEntry!");
			return DataResult.success(new CustomCommandFavoriteEntry(title, command), Lifecycle.stable());
		}

		@Override
		public CompoundTag save(CustomCommandFavoriteEntry entry, CompoundTag tag) {
			tag.putString("title", Component.Serializer.toJson(entry.title));
			tag.putString("command", entry.command);
			return tag;
		}
	}

	class CommandSender {
		static void sendCommand(String command) {
			try {
				Class.forName("me.shedaniel.rei.impl.client.%s.CommandSenderImpl".formatted(Platform.isForge() ? "forge" : "fabric")).getDeclaredMethod("sendCommand", String.class).invoke(null, command);
			} catch (IllegalAccessException | ClassNotFoundException | NoSuchMethodException | InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
