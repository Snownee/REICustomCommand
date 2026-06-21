package snownee.rei_custom_command;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import com.mojang.serialization.DataResult;

import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.favorites.FavoriteEntry;
import me.shedaniel.rei.api.client.favorites.FavoriteEntryType;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.compat.GuiGraphics;
import me.shedaniel.rei.api.client.gui.widgets.Tooltip;
import me.shedaniel.rei.api.client.gui.widgets.TooltipContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@SuppressWarnings("UnstableApiUsage")
public class CustomCommandFavoriteEntry extends FavoriteEntry {

	public static final Identifier ID = Identifier.fromNamespaceAndPath("rei_custom_command", "custom-command");
	public static final String TRANSLATION_KEY = "favorite.section.rei_custom_command";
	public static final FavoriteEntry DEFAULT = new CustomCommandFavoriteEntry(
			Component.literal("?"),
			Items.COMMAND_BLOCK.getDefaultInstance(),
			List.of());

	private final Component title;
	private final ItemStack icon;
	private final List<String> commands;

	public CustomCommandFavoriteEntry(Component title, ItemStack icon, List<String> commands) {
		this.title = title;
		this.icon = icon;
		this.commands = commands;
	}

	@Override
	public boolean isInvalid() {
		return this.equals(DEFAULT);
	}

	@Override
	public Renderer getRenderer(boolean showcase) {
		return new Renderer() {
			private final Supplier<List<Component>> tooltip = Suppliers.memoize(() -> {
				Font font = Minecraft.getInstance().font;
				if (CustomCommandFavoriteEntry.this.equals(DEFAULT)) {
					return Stream.of(I18n.get(TRANSLATION_KEY + ".tip").split("\n"))
							.flatMap(s -> font.getSplitter().splitLines(
									FormattedText.of(s),
									300,
									Style.EMPTY).stream().map(FormattedText::getString))
							.map(Component::literal)
							.map(Component.class::cast)
							.toList();
				} else {
					List<Component> tooltip = Lists.newArrayList();
					if (!title.getString().isBlank()) {
						tooltip.add(title);
					}
					for (String command : commands) {
						String cutCommand = "/" + font.plainSubstrByWidth(command, 195);
						if (cutCommand.length() < command.length()) {
							cutCommand += "...";
						}
						tooltip.add(Component.literal(cutCommand));
					}
					return tooltip;
				}
			});

			@Override
			public void render(GuiGraphics graphics, Rectangle bounds, int mouseX, int mouseY, float delta) {
				if (bounds.width < 5 || bounds.height < 5) {
					return;
				}
				if (icon.isEmpty()) {
					int color = bounds.contains(mouseX, mouseY) ? 0xFFEEEEEE : 0xFFAAAAAA;
					Font font = Minecraft.getInstance().font;
					Component component = title.getString().isBlank() ? Component.literal(commands.getFirst()) : title;
					List<FormattedCharSequence> lines = font.split(component, bounds.getWidth());
					if (lines.isEmpty()) {
						return;
					}
					graphics.pose().pushMatrix();
					graphics.pose().translate(bounds.getCenterX(), bounds.getCenterY());
					graphics.pose().scale(bounds.getWidth() / 18f, bounds.getHeight() / 18f);
					graphics.pose().translate(-font.width(lines.getFirst()) / 2f + 0.5f, -3.5f);
					graphics.drawString(font, lines.getFirst(), 0, 0, color, false);
					graphics.pose().popMatrix();
				} else {
					graphics.pose().pushMatrix();
					graphics.pose().translate(bounds.getMinX(), bounds.getMinY());
					graphics.pose().scale(bounds.getWidth() / 16f, bounds.getHeight() / 16f);
					graphics.renderItem(icon, 0, 0);
					if (!REICCPlugin.hasControlDown() && !icon.is(Items.COMMAND_BLOCK) && !icon.is(Items.REPEATING_COMMAND_BLOCK) &&
							!icon.is(
									Items.CHAIN_COMMAND_BLOCK)) {
						graphics.pose().scale(0.5f, 0.5f);
						graphics.renderItem(Items.COMMAND_BLOCK.getDefaultInstance(), 16, 16);
					}
					graphics.pose().popMatrix();
				}
			}

			@Override
			@Nullable
			public Tooltip getTooltip(TooltipContext context) {
				return Tooltip.create(context.getPoint(), tooltip.get());
			}

			@Override
			public boolean equals(Object obj) {
				if (this == obj) {
					return true;
				}
				if (obj == null || getClass() != obj.getClass()) {
					return false;
				}
				return hashCode() == obj.hashCode();
			}

			@Override
			public int hashCode() {
				return title.hashCode();
			}
		};
	}

	@Override
	public boolean doAction(MouseButtonEvent event) {
		if (event.button() != 0) {
			return false;
		}
		if (Minecraft.getInstance().player == null) {
			return false;
		}
		for (String command : commands) {
			Minecraft.getInstance().player.connection.sendCommand(command);
		}
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
		return true;
	}

	@Override
	public long hashIgnoreAmount() {
		return Objects.hash(title, icon.getItem(), icon.getComponents(), commands);
	}

	@Override
	public FavoriteEntry copy() {
		return this;
	}

	@Override
	public Identifier getType() {
		return ID;
	}

	@Override
	public boolean isSame(FavoriteEntry other) {
		if (!(other instanceof CustomCommandFavoriteEntry that)) {
			return false;
		}
		return Objects.equals(title, that.title) && ItemStack.isSameItemSameComponents(icon, that.icon) && Objects.equals(
				commands,
				that.commands);
	}

	public enum Type implements FavoriteEntryType<CustomCommandFavoriteEntry> {
		INSTANCE;

		@Override
		public DataResult<CustomCommandFavoriteEntry> read(CompoundTag object) {
			List<String> commands;
			if (object.get("command") instanceof StringTag(String value)) {
				commands = List.of(value);
			} else if (object.get("commands") instanceof ListTag listTag) {
				commands = listTag.stream().map(Tag::asString).map(Optional::orElseThrow).toList();
			} else {
				return DataResult.error(() -> "Cannot create CustomCommandFavoriteEntry!");
			}
			Component title = null;
			ItemStack icon = ItemStack.EMPTY;
			try {
				RegistryAccess.Frozen provider = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
				RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, provider);
				title = ComponentSerialization.CODEC.parse(ops, object.get("title")).getOrThrow();
				if (object.contains("item")) {
					icon = ItemStack.CODEC.parse(ops, object.get("item")).getOrThrow();
				}
			} catch (Exception ignored) {
			}
			if (title == null) {
				return DataResult.error(() -> "Cannot create CustomCommandFavoriteEntry!");
			}
			return DataResult.success(new CustomCommandFavoriteEntry(title, icon, commands));
		}

		@Override
		public DataResult<CustomCommandFavoriteEntry> fromArgs(Object... args) {
			if (args.length < 3 || !(args[0] instanceof Component title) || !(args[1] instanceof ItemStack icon)) {
				return DataResult.error(() -> "Cannot create CustomCommandFavoriteEntry!");
			}
			List<String> commands;
			try {
				commands = Stream.of(args).skip(2).map(String.class::cast).toList();
			} catch (Exception e) {
				return DataResult.error(() -> "Cannot create CustomCommandFavoriteEntry!");
			}
			return DataResult.success(new CustomCommandFavoriteEntry(title, icon, commands));
		}

		@Override
		public CompoundTag save(CustomCommandFavoriteEntry entry, CompoundTag tag) {
			RegistryAccess.Frozen provider = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
			RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, provider);
			tag.put("title", ComponentSerialization.CODEC.encodeStart(ops, entry.title).getOrThrow());
			if (!entry.icon.isEmpty()) {
				tag.put("item", ItemStack.CODEC.encodeStart(ops, entry.icon).getOrThrow());
			}
			if (entry.commands.size() == 1) {
				tag.putString("command", entry.commands.getFirst());
			} else {
				ListTag commands = new ListTag();
				for (String command : entry.commands) {
					commands.add(StringTag.valueOf(command));
				}
				tag.put("commands", commands);
			}
			return tag;
		}
	}
}
