# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Unified trait system with gameplay modifiers
  - Traits can have both positive and negative effects on disease rate, happiness, work speed, and food consumption
  - Supported modifiers: `diseaseRate`, `contactDiseaseRate`, `happinessBase`, `happinessDecayRate`, `workSpeed`, `foodConsumption`
  - Weighted random trait selection (2-4 traits per citizen)
  - Configurable via `traits.json` and `origins.json`
- Visitor background support
  - Visitors now receive AI-generated backgrounds when spawned via `MixinVisitorManager`
  - Background data persists through recruitment (NBT transfer)
  - Missing backgrounds checked and generated on server startup
  - Debug window displays visitor backgrounds
- Admin commands (`/motc`) - requires operator level 2
  - `/motc language [lang]` - Get or set NPC conversation language
  - `/motc regenbackground <colonyId> <citizenId>` - Regenerate a single citizen's background
  - `/motc regenall <colonyId>` - Regenerate all citizens' backgrounds in a colony
  - `/motc regenvisitors <colonyId>` - Regenerate all visitors' backgrounds in a colony
- Disease modifier system
  - Job-based disease modifiers via `disease_config.json`
  - Trait-based disease modifiers that stack multiplicatively with job modifiers
  - Contact disease rate modifiers for disease spread
- Citizen background and personality system ([#1](https://github.com/elefant-ai/MindOfTheColony/pull/1))
  - AI-generated backstories for citizens
  - Background data persistence via NBT serialization
  - Configurable backgrounds via JSON config files
  - Background display in citizen debug window
- Colony events system ([#2](https://github.com/elefant-ai/MindOfTheColony/pull/2))
  - Real-time event detection from MineColonies (citizen deaths, births, hires, building completions, job changes, raids)
  - Randomly generated flavor events (traveling merchants, bumper harvests, festivals, strange noises, worker disputes)
  - Happiness modifiers based on events
  - Event context injection into citizen AI prompts
  - Configurable event types, durations, happiness modifiers, and generation intervals
- NPC conversation history tracking ([#4](https://github.com/elefant-ai/MindOfTheColony/pull/4))
  - API history support for NPCs
- Chat window UI improvements ([#3](https://github.com/elefant-ai/MindOfTheColony/pull/3))
  - Enter-to-send functionality in chat interface
  - Auto-scroll feature in chat history
  - Chat tab button in MineColonies citizen window
  - Dedicated chat interface for citizen conversations
- Comprehensive Mixin integrations
  - `MixinCitizenManager` for automatic AI bridge lifecycle management
  - `MixinCitizenData` for background and conversation history persistence
  - `MixinMainWindowCitizen` for chat UI integration
  - `MixinDebugWindowCitizen` for background information display
- Network messaging system
  - `AIChatMessage` and `AIChatResponseMessage` for chat communication
  - `BackgroundRequestMessage` and `BackgroundResponseMessage` for background data sync
  - `ModNetworking` for network protocol management
- Configuration system
  - `ModSettings` for mod-wide configuration
  - `BackgroundConfigLoader` for loading custom background definitions
  - `BackgroundConfigData` for background data structures
- Client-side functionality
  - `ClientChatHandler` for handling chat on client
  - `ClientBackgroundCache` for caching citizen backgrounds
  - `ChatWindowCitizen` for chat UI rendering
- NeoForge 1.21.1 support
  - Migration from Forge to NeoForge
  - Updated mod metadata to `neoforge.mods.toml`
  - Gradle wrapper integration
- Build automation
  - GitHub Actions workflow for automated builds
- Development tools
  - VSCode settings for project
  - Git attributes configuration

### Changed
- Refactored AI bridge architecture
  - Renamed `CitizenAIManager` to `CitizenNpcManager` for clarity
  - Replaced `CitizenAIBridge` with `CitizenNpcBridge`
  - Improved conversation management with client-server network protocol
  - Enhanced system prompt generation with contextual awareness
- `BackgroundRequestMessage` now checks both citizen and visitor managers
- `MixinCitizenData` updated for NeoForge's `HolderLookup.Provider` in NBT serialization
- Improved citizen status reporting ([#3](https://github.com/elefant-ai/MindOfTheColony/pull/3))
  - Added Level 0 → 1 build phrasing in citizen AI prompts
  - Enhanced job/request status strings with work order and requester details
  - Added null safety guards for citizen status
- Updated dependency management
  - Moved MineColonies and related libraries from local `lib/` to Maven dependencies
  - Cleaned up build.gradle configuration
- Enhanced .gitignore to exclude build artifacts and IDE files

### Removed
- Local library JARs from repository
  - blockui-1.20.1-1.0.193.jar
  - domum_ornamentum-1.20.1-1.0.290-snapshot-universal.jar
  - minecolonies-1.20.1-1.1.989-snapshot.jar
  - multipiston-1.20-1.2.43-RELEASE.jar
  - structurize-1.20.1-1.0.777-snapshot.jar
- Obsolete Player2 API service classes
  - `HTTPUtils.java`
  - `Player2APIService.java`
- Old Forge metadata (`META-INF/mods.toml`)
- `pack.mcmeta` file
- `ConversationHistory.java` (functionality integrated into new bridge)
- `ChatEventHandler.java` (replaced by `NpcEventHandler.java`)

[Unreleased]: https://github.com/elefant-ai/MindOfTheColony/compare/master...dev/mc-1.21.1
