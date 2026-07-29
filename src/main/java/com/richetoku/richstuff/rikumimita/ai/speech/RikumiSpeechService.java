package com.richetoku.richstuff.rikumimita.ai.speech;

import com.google.gson.JsonObject;
import com.richetoku.richstuff.RichStuff;
import com.richetoku.richstuff.rikumimita.RikumiMitaEntity;
import com.richetoku.richstuff.rikumimita.ai.RikumiAiLifecycle;
import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/** Chat plus RichStuff voice playback for pregenerated lines and agent-provided sound events. */
public final class RikumiSpeechService {
    private RikumiSpeechService() {}

    public static JsonObject speak(String text, String preset, String soundEventId, float volume, float pitch) {
        RikumiMitaEntity avatar = RikumiAiLifecycle.avatar()
                .orElseThrow(() -> new IllegalStateException("Rikumi Mita is not loaded"));
        String clean = text == null ? "" : text.strip();
        if (!clean.isBlank()) avatar.sendDialogueToOwner(clean);
        SoundEvent sound = resolveSound(preset, soundEventId);
        if (avatar.isVoiceEnabled()) {
            avatar.playSound(sound, clamp(volume, 0.05F, 4.0F), clamp(pitch, 0.25F, 2.0F));
        }
        ResourceLocation soundId = BuiltInRegistries.SOUND_EVENT.getKey(sound);
        JsonObject result = new JsonObject();
        result.addProperty("text", clean);
        result.addProperty("voice_enabled", avatar.isVoiceEnabled());
        result.addProperty("preset", normalizePreset(preset));
        result.addProperty("sound_event", soundId == null ? "richstuff:rikumi_idle" : soundId.toString());
        result.addProperty("dynamic_tts_mode", "registered_sound_event");
        result.addProperty("dynamic_audio_contract",
                "The connected agent may register generated OGG audio in a resource pack and pass its sound-event id.");
        return result;
    }

    public static void playPreset(RikumiMitaEntity avatar, String preset) {
        if (avatar != null && avatar.isVoiceEnabled()) avatar.playSound(resolveSound(preset, ""), 0.8F, 1.0F);
    }

    private static SoundEvent resolveSound(String preset, String soundEventId) {
        if (soundEventId != null && !soundEventId.isBlank()) {
            String raw = soundEventId.strip();
            ResourceLocation id = ResourceLocation.tryParse(raw.indexOf(':') < 0 ? "richstuff:" + raw : raw);
            if (id != null && BuiltInRegistries.SOUND_EVENT.containsKey(id)) {
                return BuiltInRegistries.SOUND_EVENT.get(id);
            }
        }
        return switch (normalizePreset(preset)) {
            case "happy", "success", "greeting" -> RichStuff.RIKUMI_GREETING.get();
            case "warning", "blocked", "failure" -> RichStuff.RIKUMI_WARNING.get();
            case "thinking", "working" -> RichStuff.RIKUMI_WORKING.get();
            case "crafting" -> RichStuff.RIKUMI_CRAFTING.get();
            case "found", "pickup" -> RichStuff.RIKUMI_FOUND.get();
            default -> RichStuff.RIKUMI_IDLE.get();
        };
    }

    private static String normalizePreset(String preset) {
        return preset == null || preset.isBlank() ? "neutral" : preset.strip().toLowerCase(Locale.ROOT);
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
