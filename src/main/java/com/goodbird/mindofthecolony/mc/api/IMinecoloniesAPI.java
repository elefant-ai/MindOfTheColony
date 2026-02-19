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

public interface IMinecoloniesAPI
{

    static IMinecoloniesAPI getInstance()
    {
        return MinecoloniesAPIProxy.getInstance();
    }

    IColonyManager getColonyManager();

    ICitizenDataManager getCitizenDataManager();

    IMobAIRegistry getMobAIRegistry();

    IPathNavigateRegistry getPathNavigateRegistry();

    IBuildingDataManager getBuildingDataManager();

    Registry<BuildingEntry> getBuildingRegistry();

    Registry<BuildingExtensionEntry> getBuildingExtensionRegistry();

    IJobDataManager getJobDataManager();

    Registry<JobEntry> getJobRegistry();

    Registry<InteractionResponseHandlerEntry> getInteractionResponseHandlerRegistry();

    IGuardTypeDataManager getGuardTypeDataManager();

    Registry<GuardType> getGuardTypeRegistry();

    IModelTypeRegistry getModelTypeRegistry();

    Configurations<ClientConfiguration, ServerConfiguration, CommonConfiguration> getConfig();

    IFurnaceRecipes getFurnaceRecipes();

    IInteractionResponseHandlerDataManager getInteractionResponseHandlerDataManager();

    IGlobalResearchTree getGlobalResearchTree();

    Registry<ModResearchRequirements.ResearchRequirementEntry> getResearchRequirementRegistry();

    Registry<ModResearchEffects.ResearchEffectEntry> getResearchEffectRegistry();

    Registry<ColonyEventTypeRegistryEntry> getColonyEventRegistry();

    Registry<ColonyEventDescriptionTypeRegistryEntry> getColonyEventDescriptionRegistry();

    Registry<RecipeTypeEntry> getRecipeTypeRegistry();

    Registry<CraftingType> getCraftingTypeRegistry();

    Registry<QuestRegistries.RewardEntry> getQuestRewardRegistry();

    Registry<QuestRegistries.ObjectiveEntry> getQuestObjectiveRegistry();

    Registry<QuestRegistries.TriggerEntry> getQuestTriggerRegistry();

    Registry<QuestRegistries.DialogueAnswerEntry> getQuestDialogueAnswerRegistry();

    Registry<HappinessRegistry.HappinessFactorTypeEntry> getHappinessTypeRegistry();

    Registry<HappinessRegistry.HappinessFunctionEntry> getHappinessFunctionRegistry();

    void onRegistryNewRegistry(NewRegistryEvent event);

    Registry<EquipmentTypeEntry> getEquipmentTypeRegistry();

    EventBus getEventBus();
}
