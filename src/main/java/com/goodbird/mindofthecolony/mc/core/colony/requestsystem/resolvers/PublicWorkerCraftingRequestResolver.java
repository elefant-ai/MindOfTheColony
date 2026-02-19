package com.goodbird.mindofthecolony.mc.core.colony.requestsystem.resolvers;

import com.goodbird.mindofthecolony.mc.api.colony.buildings.IBuilding;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.views.IBuildingView;
import com.goodbird.mindofthecolony.mc.api.colony.jobs.registry.JobEntry;
import com.goodbird.mindofthecolony.mc.api.colony.requestsystem.location.ILocation;
import com.goodbird.mindofthecolony.mc.api.colony.requestsystem.manager.IRequestManager;
import com.goodbird.mindofthecolony.mc.api.colony.requestsystem.request.IRequest;
import com.goodbird.mindofthecolony.mc.api.colony.requestsystem.requestable.IDeliverable;
import com.goodbird.mindofthecolony.mc.api.colony.requestsystem.requestable.IRequestable;
import com.goodbird.mindofthecolony.mc.api.colony.requestsystem.requestable.crafting.PublicCrafting;
import com.goodbird.mindofthecolony.mc.api.colony.requestsystem.requester.IRequester;
import com.goodbird.mindofthecolony.mc.api.colony.requestsystem.token.IToken;
import com.goodbird.mindofthecolony.mc.api.crafting.IRecipeStorage;
import com.goodbird.mindofthecolony.mc.core.colony.buildings.AbstractBuilding;
import com.goodbird.mindofthecolony.mc.core.colony.buildings.modules.WorkerBuildingModule;
import com.goodbird.mindofthecolony.mc.core.colony.buildings.moduleviews.WorkerBuildingModuleView;
import com.goodbird.mindofthecolony.mc.core.colony.requestsystem.resolvers.core.AbstractCraftingRequestResolver;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.goodbird.mindofthecolony.mc.api.util.constant.RSConstants.CONST_CRAFTING_RESOLVER_PRIORITY;

/**
 * A crafting resolver which takes care of 3x3 crafts which are crafted by a crafter worker.
 */
public class PublicWorkerCraftingRequestResolver extends AbstractCraftingRequestResolver
{
    /**
     * Initializing constructor.
     *
     * @param location the location of the resolver.
     * @param token    its id.
     */
    public PublicWorkerCraftingRequestResolver(@NotNull final ILocation location, @NotNull final IToken<?> token, final JobEntry entry)
    {
        super(location, token, entry, true);
    }

    @Nullable
    @Override
    public List<IRequest<?>> getFollowupRequestForCompletion(@NotNull final IRequestManager manager, @NotNull final IRequest<? extends IDeliverable> completedRequest)
    {
        //Noop. The production resolver already took care of that.
        return null;
    }

    @Override
    public void onAssignedRequestBeingCancelled(@NotNull final IRequestManager manager, @NotNull final IRequest<? extends IDeliverable> request)
    {
    }

    @Override
    public void onAssignedRequestCancelled(@NotNull final IRequestManager manager, @NotNull final IRequest<? extends IDeliverable> request)
    {

    }

    @Override
    public void onRequestedRequestComplete(@NotNull final IRequestManager manager, @NotNull final IRequest<?> request)
    {
        /*
         * Nothing to be done.
         */
    }

    @Override
    public void onRequestedRequestCancelled(@NotNull final IRequestManager manager, @NotNull final IRequest<?> request)
    {
        //NOOP
    }

    @NotNull
    @Override
    public MutableComponent getRequesterDisplayName(@NotNull final IRequestManager manager, @NotNull final IRequest<?> request)
    {
        final IRequester requester = manager.getColony().getRequesterBuildingForPosition(getLocation().getInDimensionLocation());
        if (requester instanceof IBuildingView)
        {
            final WorkerBuildingModuleView moduleView = ((IBuildingView) requester).getModuleViewMatching(WorkerBuildingModuleView.class, m -> m.getJobEntry() == getJobEntry());
            if (moduleView != null)
            {
                return Component.translatableEscape(moduleView.getJobEntry().getTranslationKey());
            }
        }
        if (requester instanceof IBuilding)
        {
            final WorkerBuildingModule module = ((IBuilding) requester).getModuleMatching(WorkerBuildingModule.class, m -> m.getJobEntry() == getJobEntry());
            return Component.translatableEscape(module.getJobEntry().getTranslationKey());
        }
        return super.getRequesterDisplayName(manager, request);
    }

    @Override
    public int getPriority()
    {
        return CONST_CRAFTING_RESOLVER_PRIORITY;
    }

    @Override
    public boolean canBuildingCraftRecipe(@NotNull final AbstractBuilding building, final IRecipeStorage recipeStorage)
    {
        return recipeStorage != null;
    }

    @Override
    protected IRequestable createNewRequestableForStack(final ItemStack stack, final int count, final int minCount, final IToken<?> recipeStorage)
    {
        return new PublicCrafting(stack, count, minCount, recipeStorage);
    }
}
