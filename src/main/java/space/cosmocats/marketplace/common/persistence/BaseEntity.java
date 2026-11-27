package space.cosmocats.marketplace.common.persistence;

import lombok.*;
import java.util.UUID;
import jakarta.persistence.*;
import org.hibernate.proxy.HibernateProxy;

@Getter
@MappedSuperclass
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseEntity {
    @Id
    private UUID id;

    protected BaseEntity(UUID id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BaseEntity entity) || persistentClass(this) != persistentClass(entity)) {
            return false;
        }
        UUID identifier = identifier(this);
        return identifier != null && identifier.equals(identifier(entity));
    }

    @Override
    public int hashCode() {
        return persistentClass(this).hashCode();
    }

    private static UUID identifier(BaseEntity entity) {
        return (entity instanceof HibernateProxy proxy)
                ? (UUID) proxy.getHibernateLazyInitializer().getInternalIdentifier()
                : entity.id;
    }

    private static Class<?> persistentClass(BaseEntity entity) {
        return (entity instanceof HibernateProxy proxy)
                ? proxy.getHibernateLazyInitializer().getPersistentClass()
                : entity.getClass();
    }
}
