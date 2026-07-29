# Rich Stuff — NeoForge 1.21.1

Current development version: **0.0.13**.

Rich Stuff owns materials, alloys, gems, crystals, fuels, processing intermediates and byproducts, Juice Jugs and other fluid vessels, Slag, modular equipment, Foundry systems, tiered tanks, tiered barrels, and general utilities.

Rich Stuff requires RichCore only. Rich Ores, Rich Farming, Rich Machines, and Rich Mob Farming are optional integrations rather than hard dependencies.


## Rikumi AI integration

Rikumi's external AI controller is built directly into RichStuff. The visible native Rikumi Mita entity is paired with an invisible, Survival-constrained NeoForge fake player that can be controlled through the authenticated WebSocket protocol. The existing 27-slot Rikumi menu and fake-player inventory share one storage path.

Do not install the former standalone companion JAR. Configure the integrated actor in `config/richstuff-common.toml` under `[rikumi_ai]`, or use the documented environment variables. See [`docs/RIKUMI_AI_INTEGRATION.md`](docs/RIKUMI_AI_INTEGRATION.md) for actions, payload fields, security limits, and deployment details.

Build from the suite root with Java 21 using `run.bat`.
