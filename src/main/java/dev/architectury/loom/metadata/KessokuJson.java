package dev.architectury.loom.metadata;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.fabricmc.loom.LoomGradlePlugin;
import net.fabricmc.loom.configuration.ifaceinject.InterfaceInjectionProcessor;
import net.fabricmc.loom.util.Constants;
import net.fabricmc.loom.util.ModPlatform;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.util.CheckSignatureAdapter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class KessokuJson implements JsonBackedModMetadataFile, SingleIdModMetadataFile {
	public static final String FILE_NAME = "kessoku.json";

	private final JsonObject json;

	public KessokuJson(JsonObject json) {
		this.json = Objects.requireNonNull(json, "json");
	}

	public static KessokuJson of(byte[] utf8) {
		return of(new String(utf8, StandardCharsets.UTF_8));
	}

	public static KessokuJson of(String text) {
		return of(LoomGradlePlugin.GSON.fromJson(text, JsonObject.class));
	}

	public static KessokuJson of(Path path) throws IOException {
		return of(Files.readString(path, StandardCharsets.UTF_8));
	}

	public static KessokuJson of(File file) throws IOException {
		return of(file.toPath());
	}

	public static KessokuJson of(JsonObject json) {
		return new KessokuJson(json);
	}

	@Override
	public JsonObject getJson() {
		return json;
	}

	@Override
	public @Nullable String getId() {
		return null;
	}

	@Override
	public Set<String> getAccessWideners() {
		return Set.of();
	}

	@Override
	public Set<String> getAccessTransformers(ModPlatform platform) {
		return Set.of();
	}

	@Override
	public List<InterfaceInjectionProcessor.InjectedInterface> getInjectedInterfaces(@Nullable String modId) {
		if (modId == null) {
			throw new IllegalArgumentException("getInjectedInterfaces: mod ID has to be provided for kessoku.json");
		}

		return getInjectedInterfaces(json, modId);
	}

	static List<InterfaceInjectionProcessor.InjectedInterface> getInjectedInterfaces(JsonObject json, String modId) {
		Objects.requireNonNull(modId, "mod ID");

		if (json.has(Constants.CustomModJsonKeys.INJECTED_INTERFACE)) {
			JsonObject addedIfaces = json.getAsJsonObject(Constants.CustomModJsonKeys.INJECTED_INTERFACE);

			final List<InterfaceInjectionProcessor.InjectedInterface> result = new ArrayList<>();

			for (String className : addedIfaces.keySet()) {
				final JsonArray ifacesInfo = addedIfaces.getAsJsonArray(className);

				for (JsonElement ifaceElement : ifacesInfo) {
					String ifaceInfo = ifaceElement.getAsString();

					String name = ifaceInfo;
					String generics = null;

					if (ifaceInfo.contains("<") && ifaceInfo.contains(">")) {
						name = ifaceInfo.substring(0, ifaceInfo.indexOf("<"));
						generics = ifaceInfo.substring(ifaceInfo.indexOf("<"));

						// First Generics Check, if there are generics, are them correctly written?
						SignatureReader reader = new SignatureReader("Ljava/lang/Object" + generics + ";");
						CheckSignatureAdapter checker = new CheckSignatureAdapter(CheckSignatureAdapter.CLASS_SIGNATURE, null);
						reader.accept(checker);
					}

					result.add(new InterfaceInjectionProcessor.InjectedInterface(modId, className, name, generics));
				}
			}

			return result;
		}

		return Collections.emptyList();
	}

	@Override
	public String getFileName() {
		return FILE_NAME;
	}

	@Override
	public List<String> getMixinConfigs() {
		return List.of();
	}
}
