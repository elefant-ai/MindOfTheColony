package com.goodbird.mindofthecolony.mc.api;

import com.ldtteam.common.config.Configurations;
import com.goodbird.mindofthecolony.mc.api.client.render.modeltype.registry.IModelTypeRegistry;
import com.goodbird.mindofthecolony.mc.api.colony.ICitizenDataManager;
import com.goodbird.mindofthecolony.mc.api.colony.IColonyManager;
import com.goodbird.mindofthecolony.mc.api.colony.buildingextensions.registry.BuildingExtensionRegistries.BuildingExtensionEntry;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.registry.BuildingEntry;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.registry.IBuildingDataManager;
import com.goodbird.mindofthecolony.mc.api.colony.colonyEvents.registry.ColonyEventDescriptionTypeRegistryEntry;
import com.goodbird.mindofthecolony.mc.api.colony.colonyEvents.registry.ColonyEventTypeRegistryEntry;
import com.goodbird.mindofthecolony.mc.api.colony.guardtype.GuardType;
import com.goodbird.mindofthecolony.mc.api.colony.guardtype.registry.IGuardTypeDataManager;
import com.goodbird.mindofthecolony.mc.api.colony.interactionhandling.registry.IInteractionResponseHandlerDataManager;
import com.goodbird.mindofthecolony.mc.api.colony.interactionhandling.registry.InteractionResponseHandlerEntry;
import com.goodbird.mindofthecolony.mc.api.colony.jobs.registry.IJobDataManager;
import com.goodbird.mindofthecolony.mc.api.colony.jobs.registry.JobEntry;
import com.goodbird.mindofthecolony.mc.api.compatibility.IFurnaceRecipes;
import com.goodbird.mindofthecolony.mc.api.configuration.ClientConfiguration;
import com.goodbird.mindofthecolony.mc.api.configuration.CommonConfiguration;
import com.goodbird.mindofthecolony.mc.api.configuration.ServerConfiguration;
import com.goodbird.mindofthecolony.mc.api.crafting.registry.CraftingType;
import com.goodbird.mindofthecolony.mc.api.crafting.registry.RecipeTypeEntry;
import com.goodbird.mindofthecolony.mc.api.entity.mobs.registry.IMobAIRegistry;
import com.goodbird.mindofthecolony.mc.api.entity.citizen.happiness.HappinessRegistry;
import com.goodbird.mindofthecolony.mc.api.entity.pathfinding.registry.IPathNavigateRegistry;
import com.goodbird.mindofthecolony.mc.api.equipment.registry.EquipmentTypeEntry;
import com.goodbird.mindofthecolony.mc.api.eventbus.EventBus;
import com.goodbird.mindofthecolony.mc.api.quests.registries.QuestRegistries;
import com.goodbird.mindofthecolony.mc.api.research.IGlobalResearchTree;
import com.goodbird.mindofthecolony.mc.api.research.ModResearchEffects;
import com.goodbird.mindofthecolony.mc.api.research.ModResearchRequirements;
import net.minecraft.core.Registry;
import net.neoforged.neoforge.registries.NewRegistryEvent;

public final class MinecoloniesAPIProxy implements IMinecoloniesAPI
{
    private static final MinecoloniesAPIProxy ourInstance = new MinecoloniesAPIProxy();

    private IMinecoloniesAPI apiInstance;

    public static MinecoloniesAPIProxy getInstance()
    {
        return ourInstance;
    }

    private MinecoloniesAPIProxy()
    {
    }

    public void setApiInstance(final IMinecoloniesAPI apiInstance)
    {
        this.apiInstance = apiInstance;
    }

    @Override
    public IColonyManager getColonyManager()
    {
        return apiInstance.getColonyManager();
    }

    @Override
    public ICitizenDataManager getCitizenDataManager()
    {
        return apiInstance.getCitizenDataManager();
    }

    @Override
    public IMobAIRegistry getMobAIRegistry()
    {
        return apiInstance.getMobAIRegistry();
    }

    @Override
    public IPathNavigateRegistry getPathNavigateRegistry()
    {
        return apiInstance.getPathNavigateRegistry();
    }

    @Override
    public IBuildingDataManager getBuildingDataManager()
    {
        return apiInstance.getBuildingDataManager();
    }

    @Override
    public Registry<BuildingEntry> getBuildingRegistry()
    {
        return apiInstance.getBuildingRegistry();
    }

    @Override
    public Registry<BuildingExtensionEntry> getBuildingExtensionRegistry()
    {
        return apiInstance.getBuildingExtensionRegistry();
    }

    @Override
    public IJobDataManager getJobDataManager()
    {
        return apiInstance.getJobDataManager();
    }

    @Override
    public Registry<JobEntry> getJobRegistry()
    {
        return apiInstance.getJobRegistry();
    }

    @Override
    public Registry<InteractionResponseHandlerEntry> getInteractionResponseHandlerRegistry()
    {
        return apiInstance.getInteractionResponseHandlerRegistry();
    }

    @Override
    public IGuardTypeDataManager getGuardTypeDataManager()
    {
        return apiInstance.getGuardTypeDataManager();
    }

    @Override
    public Registry<GuardType> getGuardTypeRegistry()
    {
        return apiInstance.getGuardTypeRegistry();
    }

    @Override
    public IModelTypeRegistry getModelTypeRegistry()
    {
        return apiInstance.getModelTypeRegistry();
    }

    @Override
    public Configurations<ClientConfiguration, ServerConfiguration, CommonConfiguration> getConfig()
    {
        return apiInstance.getConfig();
    }

    @Override
    public IFurnaceRecipes getFurnaceRecipes()
    {
        return apiInstance.getFurnaceRecipes();
    }

    @Override
    public IInteractionResponseHandlerDataManager getInteractionResponseHandlerDataManager()
    {
        return apiInstance.getInteractionResponseHandlerDataManager();
    }

    @Override
    public IGlobalResearchTree getGlobalResearchTree()
    {
        return apiInstance.getGlobalResearchTree();
    }

    @Override
    public Registry<ModResearchRequirements.ResearchRequirementEntry> getResearchRequirementRegistry() {return apiInstance.getResearchRequirementRegistry();}

    @Override
    public Registry<ModResearchEffects.ResearchEffectEntry> getResearchEffectRegistry() {return apiInstance.getResearchEffectRegistry();}

    @Override
    public Registry<ColonyEventTypeRegistryEntry> getColonyEventRegistry()
    {
        return apiInstance.getColonyEventRegistry();
    }

    @Override
    public Registry<ColonyEventDescriptionTypeRegistryEntry> getColonyEventDescriptionRegistry()
    {
        return apiInstance.getColonyEventDescriptionRegistry();
    }

    @Override
    public Registry<RecipeTypeEntry> getRecipeTypeRegistry()
    {
        return apiInstance.getRecipeTypeRegistry();
    }

    @Override
    public Registry<CraftingType> getCraftingTypeRegistry()
    {
        return apiInstance.getCraftingTypeRegistry();
    }

    @Override
    public Registry<QuestRegistries.RewardEntry> getQuestRewardRegistry()
    {
        return apiInstance.getQuestRewardRegistry();
    }

    @Override
    public Registry<QuestRegistries.ObjectiveEntry> getQuestObjectiveRegistry()
    {
        return apiInstance.getQuestObjectiveRegistry();
    }

    @Override
    public Registry<QuestRegistries.TriggerEntry> getQuestTriggerRegistry()
    {
        return apiInstance.getQuestTriggerRegistry();
    }

    @Override
    public Registry<QuestRegistries.DialogueAnswerEntry> getQuestDialogueAnswerRegistry()
    {
        return apiInstance.getQuestDialogueAnswerRegistry();
    }

    @Override
    public Registry<HappinessRegistry.HappinessFactorTypeEntry> getHappinessTypeRegistry()
    {
        return apiInstance.getHappinessTypeRegistry();
    }

    @Override
    public Registry<HappinessRegistry.HappinessFunctionEntry> getHappinessFunctionRegistry()
    {
        return apiInstance.getHappinessFunctionRegistry();
    }

    @Override
    public void onRegistryNewRegistry(final NewRegistryEvent event)
    {
        apiInstance.onRegistryNewRegistry(event);
    }

    @Override
    public Registry<EquipmentTypeEntry> getEquipmentTypeRegistry()
    {
        return apiInstance.getEquipmentTypeRegistry();
    }

    @Override
    public EventBus getEventBus()
    {
        return apiInstance.getEventBus();
    }
}
