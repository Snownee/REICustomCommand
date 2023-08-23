package snownee.rei_custom_command;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;

import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.favorites.FavoriteEntry;
import me.shedaniel.rei.api.client.favorites.FavoriteEntryType;
import me.shedaniel.rei.api.client.gui.AbstractRenderer;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Tooltip;
import me.shedaniel.rei.api.client.gui.widgets.TooltipContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class CustomCommandFavoriteEntry extends FavoriteEntry {

	public static final ResourceLocation ID = new ResourceLocation("rei_custom_command", "custom-command");
	public static final String TRANSLATION_KEY = "favorite.section.rei-custom-command";
	public static final FavoriteEntry DEFAULT = new CustomCommandFavoriteEntry(Component.literal("?"), Items.COMMAND_BLOCK.getDefaultInstance(), "");

	private final Component title;
	private final ItemStack icon;
	private final String command;

	public CustomCommandFavoriteEntry(Component title, ItemStack icon, String command) {
		this.title = title;
		this.icon = icon;
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
				if (bounds.width < 5 || bounds.height < 5)
					return;
				if (icon.isEmpty()) {
					int color = bounds.contains(mouseX, mouseY) ? 0xFFEEEEEE : 0xFFAAAAAA;
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
				} else {
					matrices.pushPose();
	                matrices.mulPoseMatrix(RenderSystem.getModelViewMatrix());
					matrices.translate(bounds.getMinX(), bounds.getMinY(), 0);
					matrices.scale(bounds.getWidth() / 16f, bounds.getHeight() / 16f, 1);
					PoseStack modelViewStack = RenderSystem.getModelViewStack();
	                modelViewStack.pushPose();
	                modelViewStack.last().pose().load(matrices.last().pose());
	                RenderSystem.applyModelViewMatrix();
					ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();
					renderer.renderAndDecorateItem(icon, 0, 0);
					if (!Screen.hasControlDown() && !icon.is(Items.COMMAND_BLOCK) && !icon.is(Items.REPEATING_COMMAND_BLOCK) && !icon.is(Items.CHAIN_COMMAND_BLOCK)) {
						modelViewStack.scale(0.5f, 0.5f, 1);
						renderer.renderAndDecorateItem(Items.COMMAND_BLOCK.getDefaultInstance(), 16, 16, 0, 30);
					}
					matrices.popPose();
	                modelViewStack.popPose();
	                RenderSystem.applyModelViewMatrix();
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
		Minecraft.getInstance().player.commandSigned(command, null);
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
			ItemStack icon = ItemStack.EMPTY;
			try {
				title = Component.Serializer.fromJson(tag.getString("title"));
				if (tag.contains("item")) {
					icon = ItemStack.of(tag.getCompound("item"));
				}
			} catch (Exception ignored) {
			}
			if (title == null) {
				return DataResult.error("Cannot create CustomCommandFavoriteEntry!");
			}
			return DataResult.success(new CustomCommandFavoriteEntry(title, icon, command), Lifecycle.stable());
		}

		@Override
		public DataResult<CustomCommandFavoriteEntry> fromArgs(Object... args) {
			if (args.length < 2 || !(args[0] instanceof Component title) || !(args[1] instanceof String command))
				return DataResult.error("Cannot create CustomCommandFavoriteEntry!");
			return DataResult.success(new CustomCommandFavoriteEntry(title, ItemStack.EMPTY, command), Lifecycle.stable());
		}

		@Override
		public CompoundTag save(CustomCommandFavoriteEntry entry, CompoundTag tag) {
			tag.putString("title", Component.Serializer.toJson(entry.title));
			tag.putString("command", entry.command);
			if (!entry.icon.isEmpty()) {
				tag.put("item", entry.icon.save(new CompoundTag()));
			}
			return tag;
		}
	}
}
