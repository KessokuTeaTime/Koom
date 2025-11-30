/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2025 FabricMC
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

package band.kessoku.koom.extension;

import javax.inject.Inject;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;

import net.fabricmc.loom.LoomGradleExtension;

public abstract class SimpleMappingsExtension {
	@Inject
	protected abstract Project getProject();

	/**
	 * Yarn mappings patch only support (Neo)Forge, because (Neo)Forge patches broken with yarn mappings.
	 * Support 1.20.5 and above NeoForge version, but MinecraftForge version only support 1.20.3 and above.
	 *
	 * @param patchVersion yarn mappings patch version
	 * @return yarn-mappings-patch dependency
	 */
	public Dependency yarnPatch(String patchVersion) {
		Project project = this.getProject();
		LoomGradleExtension loomExtension = LoomGradleExtension.get(project);

		if (loomExtension.isNeoForge()) {
			return project.getDependencies().create("dev.architectury:yarn-mappings-patch-neoforge:" + patchVersion);
		} else if (loomExtension.isForge()) {
			return project.getDependencies().create("dev.architectury:yarn-mappings-patch-forge:" + patchVersion);
		} else {
			throw new RuntimeException("Yarn mappings patch only support (Neo)Forge");
		}
	}
}
