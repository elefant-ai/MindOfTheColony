package com.goodbird.mindofthecolony.mc.core.client.gui.townhall;

import com.ldtteam.blockui.controls.ButtonImage;
import com.ldtteam.blockui.controls.Image;
import com.ldtteam.blockui.controls.Text;
import com.goodbird.mindofthecolony.mc.api.util.constant.Constants;
import com.goodbird.mindofthecolony.mc.core.MineColonies;
import com.goodbird.mindofthecolony.mc.core.client.gui.AbstractWindowSkeleton;
import com.goodbird.mindofthecolony.mc.core.network.messages.server.GetColonyInfoMessage;
import com.goodbird.mindofthecolony.mc.core.network.messages.server.PickupBlockMessage;
import com.goodbird.mindofthecolony.mc.core.network.messages.server.colony.ColonyAbandonOwnMessage;
import com.goodbird.mindofthecolony.mc.core.network.messages.server.colony.ColonyDeleteOwnMessage;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

import static com.goodbird.mindofthecolony.mc.api.util.constant.WindowConstants.*;

/**
 * UI to delete or abandon an old colony.
 */
public class WindowTownHallDeleteAbandonColony extends AbstractWindowSkeleton
{
    /**
     * String constants.
     */
    private static final String DELETE_PROCEED = "com.goodbird.mindofthecolony.mc.core.gui.colony.delete.proceed";
    private static final String ABANDON_PROCEED = "com.goodbird.mindofthecolony.mc.core.gui.colony.abandon.proceed";
    private static final String DELETE_WARNING =    "com.goodbird.mindofthecolony.mc.core.gui.colony.delete.warning";
    private static final String ABANDON_WARNING =    "com.goodbird.mindofthecolony.mc.core.gui.colony.abandon.warning";

    /**
     * Townhall position
     */
    private final BlockPos pos;

    public WindowTownHallDeleteAbandonColony(final BlockPos pos, final String oldColonyName, final BlockPos oldColonyPos)
    {
        super(new ResourceLocation(Constants.MOD_ID, "gui/townhall/windowdeleteabandoncolony.xml"));
        this.pos = pos;
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));

        registerButton(BUTTON_CANCEL, this::close);
        registerButton(BUTTON_PICKUP_BUILDING, this::pickup);
        registerButton(BUTTON_DELETE, this::deleteColony);
        registerButton(BUTTON_ABANDON, this::abandonColony);
        registerButton("cancelaction", this::cancel);

        registerButton(BUTTON_CONFIRM_DELETE, this::confirmDeleteColony);
        registerButton(BUTTON_CONFIRM_ABANDON, this::confirmAbandonColony);

        if (MineColonies.getConfig().getServer().allowInfiniteColonies.get())
        {
            this.findPaneOfTypeByID("abandon", ButtonImage.class).show();
            this.findPaneOfTypeByID("warningtext", Text.class).setText(Component.translatable(ABANDON_WARNING, oldColonyPos.getX(), oldColonyPos.getY(), oldColonyPos.getZ(), Component.literal(oldColonyName).withStyle(ChatFormatting.DARK_RED)));
        }
        else
        {
            this.findPaneOfTypeByID("abandon", ButtonImage.class).hide();
            this.findPaneOfTypeByID("warningtext", Text.class).setText(Component.translatable(DELETE_WARNING, oldColonyPos.getX(), oldColonyPos.getY(), oldColonyPos.getZ(), Component.literal(oldColonyName).withStyle(ChatFormatting.DARK_RED)));
        }
    }

    private void confirmAbandonColony()
    {
        new ColonyAbandonOwnMessage().sendToServer();
        new GetColonyInfoMessage(pos).sendToServer();
        close();
    }

    private void confirmDeleteColony()
    {
        new ColonyDeleteOwnMessage().sendToServer();
        new GetColonyInfoMessage(pos).sendToServer();
        close();
    }

    private void abandonColony()
    {
        this.findPaneOfTypeByID("dialog", Image.class).show();
        final Text confirmText = this.findPaneOfTypeByID("confirmtext", Text.class);
        confirmText.show();
        confirmText.setText(Component.translatable(ABANDON_PROCEED));
        this.findPaneOfTypeByID("confirmdelete", ButtonImage.class).hide();
        this.findPaneOfTypeByID("confirmabandon", ButtonImage.class).show();
        this.findPaneOfTypeByID("cancelaction", ButtonImage.class).show();
    }

    private void deleteColony()
    {
        this.findPaneOfTypeByID("dialog", Image.class).show();
        final Text confirmText = this.findPaneOfTypeByID("confirmtext", Text.class);
        confirmText.show();
        confirmText.setText(Component.translatable(DELETE_PROCEED));
        this.findPaneOfTypeByID("confirmdelete", ButtonImage.class).show();
        this.findPaneOfTypeByID("confirmabandon", ButtonImage.class).hide();
        this.findPaneOfTypeByID("cancelaction", ButtonImage.class).show();
    }

    private void cancel()
    {
        this.findPaneOfTypeByID("dialog", Image.class).hide();
        this.findPaneOfTypeByID("confirmtext", Text.class).hide();
        this.findPaneOfTypeByID("confirmdelete", ButtonImage.class).hide();
        this.findPaneOfTypeByID("confirmabandon", ButtonImage.class).hide();
        this.findPaneOfTypeByID("cancelaction", ButtonImage.class).hide();
    }

    /**
     * When the pickup building button was clicked.
     */
    private void pickup()
    {
        new PickupBlockMessage(pos).sendToServer();
        close();
    }
}
