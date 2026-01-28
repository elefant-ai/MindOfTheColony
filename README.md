# Mind of the Colony: Giving a Voice to Your Villagers

[![Player2 AI Game Jam](https://img.shields.io/badge/Player2-AI_Game_Jam-blueviolet)](https://itch.io/jam/ai-npc-jam)
[![For MineColonies](https://img.shields.io/badge/Requires-MineColonies-blue)](https://www.minecolonies.com/)

**Mind of the Colony** is a mod that transforms your settlement's inhabitants from silent workers into living, thinking individuals. Created by **Goodbird** for the **Player2 AI Game Jam**, this project moves beyond typical gameplay mechanics to answer a simple question: *what are your colonists really thinking?*

Instead of adding another "gimmick," this mod deeply integrates with the social fabric of the renowned **MineColonies** mod, allowing you to peer into the soul of each citizen and discover what makes them happy, what troubles them, and what they truly desire.

## The Concept: The Colony as a Living Organism

In MineColonies, you build a town, manage resources, and assign jobs. But have you ever wondered what your farmer feels when the warehouse is full and there's nowhere to store the harvest? Or why your guard looks so grim after a nighttime raid?

**Mind of the Colony** gives you the chance to find out. By leveraging the power of the **Player2 API**, every citizen in your colony is given their own "inner monologue." They become fully-fledged characters with unique personalities shaped by their job, skills, background, and -- most importantly -- their current happiness level.

*   **Talk to Your Citizens:** Walk up to any colonist and start a conversation through the integrated chat window.
*   **Learn Their Thoughts:** Their responses are dynamically generated based on their current state -- job, health, mood, skills, inventory, and colony situation.
*   **Solve Their Problems:** Is a citizen unhappy? They might tell you what's bothering them -- perhaps they're short on food, their home is too far from their workplace, or they're mourning a fallen comrade.
*   **Discover Their Past:** Every citizen carries a unique background -- an origin story, a personality trait, and sometimes a dark secret or personal flaw that subtly shapes how they speak and think.
*   **Experience Colony Events:** The colony itself generates events -- raids, births, festivals, rumors -- that affect citizen happiness and give them something to talk about.

## How It Works: The Technology Behind the Soul

The magic of this mod lies in its elegant integration with MineColonies, not in creating new entities from scratch.

### 1. The Central Mind: `CitizenNpcManager`

This singleton class acts as the central nervous system for the entire colony's AI.

*   **Manages AI Instances:** It creates and tracks a `CitizenNpcBridge` for every citizen currently loaded in the world, ensuring each NPC has its own "mind."
*   **Conversation Tracking:** The manager tracks active conversations between players and citizens, routing messages through a client-server network protocol (`AIChatMessage` and `AIChatResponseMessage`).
*   **Lifecycle Management:** Citizens are automatically given their AI bridge when they load into the world and cleaned up when they unload, via Mixin hooks into MineColonies' citizen manager.

### 2. The Individual Personality: `CitizenNpcBridge`

Each citizen possesses their own `CitizenNpcBridge`, which shapes their unique personality and responses.

*   **Personalized Prompts:** A unique system prompt is generated for every single citizen, based on their **name, profession, gender, age, happiness level, and background**. Your lumberjack will not speak like your baker.
*   **Deep Contextual Awareness:** Before generating a response, the AI is fed a comprehensive snapshot of the citizen's reality:
    *   **Personal Status:** Health, saturation, sickness, sleep state, job details, skills, inventory, equipment, home location, happiness modifiers, and current requests.
    *   **Colony Status:** Colony name, population, buildings, overall happiness, whether the colony is under attack, day count, and recent colony events.
*   **Background Integration:** Each citizen's origin story, personality trait, and penalties are woven into the system prompt, creating consistent and distinctive character voices.

### 3. Colony Events: `ColonyEventManager`

The colony generates dynamic events that shape the world and give citizens context for conversation.

*   **Detected Events:** The event manager subscribes to MineColonies' event bus and captures real gameplay moments -- citizen deaths, births, hirings, building completions, job changes, and raids.
*   **Flavor Events:** Periodic randomly-generated events add atmosphere -- traveling merchants spotted nearby, bumper harvests, colony festivals, strange noises from the mines, or disputes between workers.
*   **Happiness Impact:** Events apply temporary happiness modifiers to all citizens. A raid lowers morale; a festival raises it.
*   **AI Context:** Active events are injected into citizen system prompts, so they naturally reference recent happenings in conversation.
*   **Configurable:** Event types, durations, happiness modifiers, generation intervals, and chances are all configurable.

### 4. Citizen Backgrounds: `CitizenBackground`

Each citizen is assigned a permanent background identity when they first spawn.

*   **Origins:** 12 possible backstories -- refugee farmer, disgraced noble, wandering trader, shipwreck survivor, former soldier, orphan street kid, and more.
*   **Personality Traits:** 10 distinct traits -- cheerful, grumpy, cautious, boastful, quiet, superstitious, kind-hearted, sarcastic, ambitious, or nostalgic.
*   **Penalties:** 18 possible burdens across four categories:
    *   **Criminal:** Past theft, desertion, fraud, or smuggling.
    *   **Conversation Secrets:** Hidden identity, deep regret, fears, haunted past, or forbidden knowledge.
    *   **Social:** Outcast status, unpaid debts, exile, or broken oaths.
    *   **Personal Flaws:** Laziness, frailty, clumsiness, short temper, or cowardice.
*   **Persistence:** Backgrounds are serialized to NBT and saved permanently with the citizen data.
*   **Configurable:** Origins, traits, and penalties can be customized via JSON config files.

### 5. Clean Integration with MineColonies

The mod uses **Mixins** to non-invasively inject its functionality into MineColonies without altering its core code. This ensures maximum compatibility and stability.

*   **`MixinCitizenManager`:** Hooks into citizen registration and unregistration to automatically create and destroy AI bridges as citizens load and unload.
*   **`MixinCitizenData`:** Extends citizen data serialization to persist background information and conversation history via NBT.
*   **`MixinMainWindowCitizen`:** Injects a chat tab button into the existing MineColonies citizen window, opening a dedicated chat interface.
*   **`MixinDebugWindowCitizen`:** Adds background information display to the citizen debug window for development and inspection.

## Why This is "Beyond an AI Gimmick"

*   **Deep Systemic Integration:** The AI doesn't just exist in the game; it **reacts** to its core systems -- economy, social bonds, and events. A citizen's happiness is no longer just a number; it's the reason for their mood, which they can now articulate to you.
*   **Emergent Narrative:** Colony events, citizen backgrounds, and real-time game state combine to create stories unique to your world. A former soldier turned guard who just survived a raid will speak very differently from a cheerful baker celebrating a harvest festival.
*   **A New Layer of Gameplay:** Managing a colony is no longer just about resource management. It becomes a social challenge. For your colony to thrive, you must not only build structures but also care for the well-being and happiness of its people by listening to their problems.

**Mind of the Colony** is not about adding chatbots. It's about giving a soul to an already living world.

## Acknowledgements

This project stands on the work and support of many.

### Player2
This framework was created for the **Player2 AI Game Jam** and is designed to integrate seamlessly with the Player2 API, realizing their vision for intelligent, interactive agents.
> *We are a team of researchers and engineers that are passionate about advancing the state of the art in AI. Our team members have worked at some of the world's leading tech companies and research institutions, and we are united by our shared vision of building intelligent agents that can interact with the world in a meaningful way.*

### Foundation & Inspiration
*   **MineColonies:** The mod is built over the vast and comprehensive API of MineColonies mod, an interactive building mod that allows you to create your own thriving town within Minecraft.

### Special Thanks
*   **Itsuka:** For his invaluable guidance with the Player2 API, brainstorming sessions, and rigorous testing.
