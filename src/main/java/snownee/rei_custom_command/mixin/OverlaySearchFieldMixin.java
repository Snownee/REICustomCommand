package snownee.rei_custom_command.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.shedaniel.rei.impl.client.gui.widget.search.OverlaySearchField;
import net.minecraft.client.input.KeyEvent;
import snownee.rei_custom_command.REICCPlugin;

@SuppressWarnings("UnstableApiUsage")
@Mixin(value = OverlaySearchField.class, remap = false)
public abstract class OverlaySearchFieldMixin {

	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true, remap = true)
	private void reicc_keyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
		if (REICCPlugin.onPressEnterInSearch((OverlaySearchField) (Object) this, event.key())) {
			cir.setReturnValue(true);
		}
	}

}
