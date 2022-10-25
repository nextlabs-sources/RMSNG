package com.nextlabs.router.hibernate;

import java.io.Serializable;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.SequenceGenerator;

/*
 * Example of usage:
 *
 * @GeneratedValue(generator="seq-gen")
 * @GenericGenerator(name="seq-gen", strategy = "com.ginkodrop.hibernate.SeqGenerator", parameters = {@Parameter(name="sequence", value="seqName")})
 *
 */
public class SeqGenerator extends SequenceGenerator {

    public SeqGenerator() {
        super();
    }

    @Override
    public Serializable generate(SessionImplementor session, Object obj) {
        Number id = (Number)session.getEntityPersister(null, obj).getClassMetadata().getIdentifier(obj, session);
        if (id != null && id.longValue() > 0) {
            return id;
        }
        return super.generate(session, obj);
    }
}
