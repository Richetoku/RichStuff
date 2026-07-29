# Integrated Rikumi AI Controller

The Rikumi AI controller is compiled as part of **RichStuff**. Do not place a separate Rikumi AI Companion JAR in `mods`; the standalone project, wrapper, build cache, test source sets, metadata, and duplicate entry point were intentionally removed.

## Runtime model

- `richstuff:rikumi_mita` remains the visible, configurable companion entity.
- An invisible NeoForge `FakePlayer` is the server-authoritative action executor used by an external AI model.
- While AI control is active, the visible entity mirrors the fake player's position and rotation.
- Rikumi's existing 27-slot GUI inventory fronts slots 0–26 of the fake-player inventory. Items acquired or used by the AI therefore appear in the same RichStuff menu.
- The fake player is forced back into Survival mode every server tick. Creative building, flight, and invulnerability are disabled.
- The actor can operate locally without a network endpoint. External commands are enabled only when both an endpoint and token are configured.

## Configuration

RichStuff creates these values under `[rikumi_ai]` in `config/richstuff-common.toml`:

```toml
enabled = true
autoSpawn = true
mirrorVisibleAvatar = true
allowUnsafeTeleport = false
accountUuid = ""
accountName = "RikumiMita"
companionApiUrl = ""
companionApiToken = ""
```

A blank UUID uses a deterministic RichStuff UUID. Environment variables take precedence over the matching config values:

- `RIKUMI_ACCOUNT_UUID`
- `RIKUMI_ACCOUNT_NAME`
- `COMPANION_API_URL`
- `COMPANION_API_TOKEN`

The URL may use `ws://`, `wss://`, `http://`, or `https://`; HTTP schemes are upgraded to their WebSocket equivalents. The token is URL-encoded and appended as the `token` query parameter. Prefer `wss://` and the environment variable for production use. Token-bearing URLs are sanitized before logging.

## Inbound protocol

External actions use a `rikumi.protocol_envelope.v1` envelope with type `actor_control.v1`. The action is read from `payload.action`. Envelope field names are snake case on the wire.

```json
{
  "schema": "rikumi.protocol_envelope.v1",
  "type": "actor_control.v1",
  "msg_id": "1c4ad964-5b42-49a8-a8e4-b4ec66ae6361",
  "corr_id": "6013b489-3e76-41bf-9238-af25806ae5ec",
  "seq": 0,
  "ack": null,
  "ack_required": true,
  "idempotency_key": "move-0001",
  "payload": {"action": "move_relative", "dx": 0.5, "dy": 0.0, "dz": 0.0},
  "deadline_ms": 5000,
  "capabilities": [],
  "ts": "2026-07-27T12:00:00Z"
}
```

Supported actions:

| Action | Important payload fields | Behavior |
|---|---|---|
| `spawn` | optional `x`, `y`, `z` | Creates or repositions the fake player at the visible avatar or world spawn. |
| `despawn` | none | Detaches the shared inventory and suppresses automatic respawn until an explicit `spawn` command or the next server start. |
| `snapshot` | none | Returns health, hunger, armor, air, XP, position, rotation, dimension, and game time. |
| `move_relative` | `dx`, `dy`, `dz` | Collision-aware relative movement, clamped to 1.25 blocks per request. |
| `look` | `yaw`, `pitch` | Changes view rotation; pitch is clamped to ±90 degrees. |
| `select_slot` | `slot` 0–8 | Selects a hotbar slot. |
| `swing` | optional `hand` | Swings `main_hand` or `off_hand`. |
| `use_item` | optional `hand` | Runs normal server-side item use. |
| `use_item_on` | `x`, `y`, `z`, optional `face`, `hand` | Uses the held item on a reachable block face. |
| `break_block` | `x`, `y`, `z` | Breaks a reachable block through Survival `ServerPlayerGameMode`; required tools are enforced. |
| `attack` | `entity_id` or `entity_uuid` | Attacks a live reachable entity and swings the main hand. |
| `interact_entity` | `entity_id` or `entity_uuid`, optional `hand` | Performs normal player interaction on a reachable entity. |
| `chat` / `say` | `text` | Broadcasts filtered in-character dialogue. Operational telemetry, coordinates, UUIDs, raw envelopes, and stack traces are rejected. |
| `teleport` | `x`, `y`, `z` | Direct same-dimension repositioning; rejected unless `allowUnsafeTeleport=true`. |

Inbound `chat_message.v1` envelopes are also accepted and routed through the same chat filter. Every action returns an `action_result.v1` envelope using the original correlation ID.

No external payload is executed as a Minecraft command or slash command.

## Build and test

From the suite root:

```bat
run.bat -FullRebuild -RegenerateCompat
```

The integrated sources compile under `:richstuff:compileJava` in the same Gradle 8.8 / Java 21 invocation as the other six source modules.
