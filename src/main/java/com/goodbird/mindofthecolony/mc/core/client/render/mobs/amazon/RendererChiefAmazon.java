package com.goodbird.mindofthecolony.mc.core.client.render.mobs.amazon;

import com.goodbird.mindofthecolony.mc.api.entity.mobs.AbstractEntityMinecoloniesMonster;
import com.goodbird.mindofthecolony.mc.core.client.model.raiders.ModelAmazonChief;
import com.goodbird.mindofthecolony.mc.core.event.ClientRegistryHandler;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * Renderer used for Chief amazons.
 */
public class RendererChiefAmazon extends AbstractRendererAmazon<AbstractEntityMinecoloniesMonster, ModelAmazonChief>
{
    /**
     * Texture of the entity.
     */
    private static final ResourceLocation TEXTURE = new ResourceLocation("minecolonies", "textures/entity/raiders/amazon_chief.png");

    /**
     * Constructor method for renderer
     *
     * @param context the renderManager
     */
    public RendererChiefAmazon(final EntityRendererProvider.Context context)
    {
        super(context, new ModelAmazonChief(context.bakeLayer(ClientRegistryHandler.AMAZON_CHIEF)), 0.5F);
    }

    @NotNull
    @Override
    public ResourceLocation getTextureLocation(final AbstractEntityMinecoloniesMonster entity)
    {
        return TEXTURE;
    }
}
