package com.nextlabs.router.hibernate;

import java.io.Serializable;
import java.util.UUID;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.IdentifierGenerator;

/*
 * Example of usage:
 *
 * @GeneratedValue(generator="id-gen")
 * @GenericGenerator(name="id-gen", strategy = "com.nextlabs.router.hibernate.IdGenerator")
 *
 */
public class IdGenerator implements IdentifierGenerator {

    public IdGenerator() {
    }

    @Override
    public Serializable generate(SessionImplementor session, Object obj) {
        Serializable id = session.getEntityPersister(null, obj).getClassMetadata().getIdentifier(obj, session);
        if (id != null) {
            return id;
        }
        return UUID.randomUUID().toString();
    }
}
