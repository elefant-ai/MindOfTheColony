# Mind of the Colony: Giving a Voice to Your Villagers

[![Player2 AI Game Jam](https://img.shields.io/badge/Player2-AI_Game_Jam-blueviolet)](https://itch.io/jam/ai-npc-jam)
[![For MineColonies](https://img.shields.io/badge/Requires-MineColonies-blue)](https://www.minecolonies.com/)

**Mind of the Colony** is a mod that transforms your settlement's inhabitants from silent workers into living, thinking individuals. Created by **Goodbird** for the **Player2 AI Game Jam**, this project moves beyond typical gameplay mechanics to answer a simple question: *what are your colonists really thinking?*

Instead of adding another "gimmick," this mod deeply integrates with the social fabric of the renowned **MineColonies** mod, allowing you to peer into the soul of each citizen and discover what makes them happy, what troubles them, and what they truly desire.

## The Concept: The Colony as a Living Organism

In MineColonies, you build a town, manage resources, and assign jobs. But have you ever wondered what your farmer feels when the warehouse is full and there's nowhere to store the harvest? Or why your guard looks so grim after a nighttime raid?

**Mind of the Colony** gives you the chance to find out. By leveraging the power of the **Player2 API**, every citizen in your colony is given their own "inner monologue." They become fully-fledged characters with unique personalities shaped by their job, skills, family, and — most importantly — their current happiness level.

*   **Talk to Your Citizens:** Walk up to any colonist and start a conversation in chat.
*   **Learn Their Thoughts:** Their responses won't be random lines of text but dynamically generated replies based on their current state.
*   **Solve Their Problems:** Is a citizen unhappy? They might tell you what's bothering them — perhaps they're short on food, their home is too far from their workplace, or they're mourning a fallen comrade.
*   **Listen to Their Conversations:** Citizens don't just talk to you; they talk to each other, creating the lively, authentic atmosphere of a real settlement.

## How It Works: The Technology Behind the Soul

The magic of this mod lies in its elegant integration with MineColonies, not in creating new entities from scratch.

### 1. The Central Mind: `CitizenAIManager`

This singleton class acts as the central nervous system for the entire colony's AI.

*   **Manages AI Instances:** It creates and tracks a `CitizenAIBridge` for every citizen currently loaded in the world, ensuring each NPC has its own "mind."
*   **Contextual Chat:** `CitizenAIManager` intelligently routes player chat only to nearby citizens, creating a realistic sense of local communication.
*   **Social Simulation:** Citizens can "overhear" each other. When one colonist speaks, others nearby receive that message as context, allowing them to react to conversations happening around them.
*   **Conversation Queuing:** To avoid the chaos of everyone talking at once, the manager uses a queuing system (`responseQueue`). This creates a more natural and orderly flow of dialogue, turning a cacophony into a community.

### 2. The Individual Personality: `CitizenAIBridge`

Each citizen possesses their own `CitizenAIBridge`, which shapes their unique personality and responses.

*   **Personalized Prompts:** A unique system prompt is generated for the LLM for every single citizen, based on their **name, profession, happiness level, and relationships**. Your lumberjack will not speak like your baker.
*   **Deep Contextual Awareness:** Before generating a response, the AI is fed a comprehensive snapshot of the citizen's reality:
    *   **Personal Status:** "I am a baker, and I'm a bit upset because my tools are about to break. My home is at `[coordinates]`, and my wife is `[name]`."
    *   **Colony Status:** "Our colony has `[X]` citizens, it's currently nighttime, and we were recently attacked."
*   **Persistent Memory:** Each citizen's `ConversationHistory` is serialized and saved with the colony's data. They will remember your past conversations, even after a server restart.

### 3. Clean Integration with MineColonies

The mod uses **Forge Mixins** to non-invasively inject its functionality into MineColonies without altering its core code. This ensures maximum compatibility and stability.

## Why This is "Beyond an AI Gimmick"

*   **Deep Systemic Integration:** The AI doesn't just exist in the game; it **reacts** to its core systems—economy, social bonds, and events. A citizen's happiness is no longer just a number; it's the reason for their mood, which they can now articulate to you.
*   **Emergent Narrative:** The mod creates a foundation for stories written by you and your citizens. The successes and failures of your colony are reflected in their dialogues, creating a unique and personal history for your settlement.
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
