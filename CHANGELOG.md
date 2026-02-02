# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Citizen background and personality system ([#1](https://github.com/elefant-ai/MindOfTheColony/pull/1))
  - 12 unique backstories for citizens (refugee farmer, disgraced noble, wandering trader, etc.)
  - 10 distinct personality traits (cheerful, grumpy, cautious, boastful, etc.)
  - 18 possible penalties across criminal, conversation secrets, social, and personal flaw categories
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
  - Persistent NPC memory across server restarts via UUID-based resume
  - NPC IDs stored in NBT compound tags for state persistence
  - Fixed game session ID for identity consistency
  - Conversation history continuity across reloads
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
- Automated release workflow ([#10](https://github.com/elefant-ai/MindOfTheColony/pull/10))
  - GitHub Actions for automated publishing to Modrinth and CurseForge
  - Builds and uploads on GitHub release publication
  - Uses CHANGELOG.md for release notes
- Configurable disease system ([#5](https://github.com/elefant-ai/MindOfTheColony/pull/5))
  - Job-specific disease susceptibility modifiers (miners/quarriers 2.0x, healers 0.05x, etc.)
  - Configurable contact spread rates per job type
  - `DiseaseConfig.java` with TOML configuration
  - `MixinCitizenDiseaseHandler.java` for custom disease spread logic
- Development tools
  - VSCode settings for project
  - Git attributes configuration

### Changed
- Refactored AI bridge architecture
  - Renamed `CitizenAIManager` to `CitizenNpcManager` for clarity
  - Replaced `CitizenAIBridge` with `CitizenNpcBridge`
  - Improved conversation management with client-server network protocol
  - Enhanced system prompt generation with contextual awareness
- Improved citizen status reporting ([#3](https://github.com/elefant-ai/MindOfTheColony/pull/3))
  - Added Level 0 → 1 build phrasing in citizen AI prompts
  - Enhanced job/request status strings with work order and requester details
  - Added null safety guards for citizen status
- Updated dependency management
  - Moved MineColonies and related libraries from local `lib/` to Maven dependencies
  - Cleaned up build.gradle configuration
- Enhanced .gitignore to exclude build artifacts and IDE files
- Replaced hardcoded healer disease immunity with configurable low-susceptibility modifier ([#5](https://github.com/elefant-ai/MindOfTheColony/pull/5))

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
