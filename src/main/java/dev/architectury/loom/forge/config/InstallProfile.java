package dev.architectury.loom.forge.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Map;

public record InstallProfile(
		String profile,
		String version,
		String minecraft,
		Map<String, Data> data
) {

	public static final Codec<InstallProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.fieldOf("profile").forGetter(InstallProfile::profile),
			Codec.STRING.fieldOf("version").forGetter(InstallProfile::version),
			Codec.STRING.fieldOf("minecraft").forGetter(InstallProfile::minecraft),
			Data.MAP_CODEC.fieldOf("data").forGetter(InstallProfile::data)
	).apply(instance, InstallProfile::new));

	public record Data(String client, String server) {
		public static final Codec<Data> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.STRING.fieldOf("client").forGetter(Data::client),
				Codec.STRING.fieldOf("server").forGetter(Data::server)
		).apply(instance, Data::new));

		public static final Codec<Map<String, Data>> MAP_CODEC = Codec.unboundedMap(Codec.STRING, Data.CODEC);
	}
}
