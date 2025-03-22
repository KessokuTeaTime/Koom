package band.kessoku.koom;

import net.fabricmc.loom.util.ModPlatform;

public enum Platform {
	FABRIC,
	NEO,
	COMMON;

	public String id() {
		return switch (this) {
			case FABRIC -> "fabric";
			case NEO -> "neoforge";
			case COMMON -> "common";
		};
	}

	public ModPlatform convert() {
		return switch (this) {
			case FABRIC -> ModPlatform.FABRIC;
			case NEO -> ModPlatform.NEOFORGE;
			case COMMON -> null;
		};
	}
}
