package net.tangentmc.portalStick.utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.bukkit.entity.Entity;
/**
 * An interface thats capable of saving itself inside the metadata of an entity
 * Any class implementing this entity needs to have the annotation {@link Metadata}
 * @author sanjay
 *
 */
public interface MetadataSaver {
	default String getMetadataName() {
		return getMetadataName(this.getClass());
	}
	static String getMetadataName(Class<? extends MetadataSaver> clazz) {
		if (clazz.getAnnotation(Metadata.class)==null) return null;
		return clazz.getAnnotation(Metadata.class).metadataName();
	}
	static boolean isInstance(Entity en,Class<? extends MetadataSaver> clazz) {
		return en.hasMetadata(getMetadataName(clazz));
	}
	@SuppressWarnings("unchecked")
	static <T extends MetadataSaver> T get(Entity en,Class<? extends T> clazz) {
		if (!isInstance(en,clazz)) return null;
		return (T) en.getMetadata(getMetadataName(clazz)).get(0).value();
	}
	@Retention(value = RetentionPolicy.RUNTIME)
	@interface Metadata {
		String metadataName();
	}
}
