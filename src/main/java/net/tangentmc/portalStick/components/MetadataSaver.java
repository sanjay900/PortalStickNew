package net.tangentmc.portalStick.components;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.UUID;

import org.bukkit.entity.Entity;
/**
 * An interface thats capable of saving itself inside the metadata of an entity
 * Any class implementing this entity needs to have the annotation {@link Metadata}
 * @author sanjay
 *
 */
public interface MetadataSaver {
	public default String getMetadataName() {
		return getMetadataName(this.getClass());
	}
	public static String getMetadataName(Class<? extends MetadataSaver> clazz) {
		if (clazz.getAnnotation(Metadata.class)==null) return null;
		return clazz.getAnnotation(Metadata.class).metadataName();
	}
	public static boolean isInstance(Entity en,Class<? extends MetadataSaver> clazz) {
		return en.hasMetadata(getMetadataName(clazz));
	}
	@SuppressWarnings("unchecked")
	public static <T extends MetadataSaver> T get(Entity en,Class<? extends T> clazz) {
		if (!isInstance(en,clazz)) return null;
		return (T) en.getMetadata(getMetadataName(clazz)).get(0).value();
	}
	@Retention(value = RetentionPolicy.RUNTIME)
	public static @interface Metadata {
		public String metadataName();
	}
}
