/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2020-2023 FabricMC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package dev.architectury.loom.forge.dependency;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.architectury.loom.forge.config.InstallProfile;
import org.gradle.api.Project;
import org.jetbrains.annotations.Nullable;

import net.fabricmc.loom.configuration.DependencyInfo;
import net.fabricmc.loom.util.Constants;
import net.fabricmc.loom.util.FileSystemUtil;
import net.fabricmc.loom.util.ZipUtils;

public class PatchProvider extends DependencyProvider {
	private final Path projectCacheFolder;
	private File installerJar;
	private JsonObject json;
	private InstallProfile installProfile;
	private @Nullable Path clientPatches;
	private @Nullable Path serverPatches;

	public PatchProvider(Project project) {
		super(project);
		this.projectCacheFolder = ForgeProvider.getForgeCache(project);
	}

	@Override
	public void provide(DependencyInfo dependency) throws Exception {
		init();

		installerJar = new File(getExtension().getForgeProvider().getGlobalCache(), "forge-installer.jar");
		Path installProfileJson = getExtension().getForgeProvider().getGlobalCache().toPath().resolve("forge-installProfile.json");

		if (Files.notExists(installProfileJson) || refreshDeps()) {
			File resolved = dependency.resolveFile().orElseThrow(() -> new RuntimeException("Could not resolve Forge installer"));
			Files.copy(resolved.toPath(), installerJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
			Files.write(installProfileJson, ZipUtils.unpack(resolved.toPath(), "install_profile.json"));
		}

		try (Reader reader = Files.newBufferedReader(installProfileJson)) {
			json = new Gson().fromJson(reader, JsonObject.class);
			installProfile = InstallProfile.CODEC.parse(JsonOps.INSTANCE, json)
					.getOrThrow(false, msg -> getProject().getLogger().error("Couldn't read installer install profile, {}", msg));
		}
	}

	public Path extractClientPatches() {
		if (clientPatches == null) {
			clientPatches = projectCacheFolder.resolve("patches-client.lzma");
			String binpatchName = installProfile.data().get("BINPATCH").client().replaceFirst("^/data/", "");
			extractPatches(clientPatches, binpatchName);
		}

		return clientPatches;
	}

	public Path extractServerPatches() {
		if (serverPatches == null) {
			serverPatches = projectCacheFolder.resolve("patches-server.lzma");
			String binpatchName = installProfile.data().get("BINPATCH").server().replaceFirst("^/data/", "");
			extractPatches(serverPatches, binpatchName);
		}

		return serverPatches;
	}

	private void extractPatches(Path targetPath, String name) {
		if (Files.exists(targetPath) && !refreshDeps()) {
			// No need to extract
			return;
		}

		try (FileSystemUtil.Delegate fs = FileSystemUtil.getJarFileSystem(installerJar, false)) {
			Files.copy(fs.getPath("data", name), targetPath, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void init() {
		try {
			Files.createDirectories(projectCacheFolder);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public String getTargetConfig() {
		return Constants.Configurations.FORGE_INSTALLER;
	}

	public JsonObject getJson() {
		return json;
	}

	public InstallProfile getInstallProfile() {
		return installProfile;
	}
}
