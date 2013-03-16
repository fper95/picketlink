/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.picketlink.test.idm.performance;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterVariables;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.config.FeatureSet;
import org.picketlink.idm.config.IdentityConfiguration;
import org.picketlink.idm.config.JPAIdentityStoreConfiguration;
import org.picketlink.idm.internal.DefaultIdentityManager;
import org.picketlink.idm.internal.DefaultIdentityStoreInvocationContextFactory;
import org.picketlink.idm.jpa.schema.CredentialObject;
import org.picketlink.idm.jpa.schema.CredentialObjectAttribute;
import org.picketlink.idm.jpa.schema.IdentityObject;
import org.picketlink.idm.jpa.schema.IdentityObjectAttribute;
import org.picketlink.idm.jpa.schema.PartitionObject;
import org.picketlink.idm.jpa.schema.RelationshipIdentityObject;
import org.picketlink.idm.jpa.schema.RelationshipObject;
import org.picketlink.idm.jpa.schema.RelationshipObjectAttribute;
import org.picketlink.idm.model.Realm;
import org.picketlink.idm.model.SimpleUser;

/**
 * @author Pedro Silva
 * 
 */
public class JPAIdentityStoreLoadUsersJMeterTest extends AbstractJavaSamplerClient {

    private static EntityManagerFactory emf = Persistence.createEntityManagerFactory("jpa-identity-store-tests-pu");
    private static IdentityManager identityManager = null;
    private static final ThreadLocal<EntityManager> entityManager = new ThreadLocal<EntityManager>();

    static {
        identityManager = createIdentityManager();

        EntityManager currentEntityManager = emf.createEntityManager();

        currentEntityManager.getTransaction().begin();

        entityManager.set(currentEntityManager);
        
        identityManager.add(new SimpleUser("testingUser"));
        
        EntityManager entityManager2 = entityManager.get();
        entityManager2.getTransaction().commit();
        entityManager2.close();
        entityManager.remove();
    }

    @Override
    public Arguments getDefaultParameters() {
        Arguments arguments = new Arguments();

        arguments.addArgument("loginName", "Sample User");

        return arguments;
    }

    @Override
    public void setupTest(JavaSamplerContext context) {

    }

    @Override
    public void teardownTest(JavaSamplerContext context) {
    }

    @Override
    public SampleResult runTest(JavaSamplerContext context) {
        SampleResult result = new SampleResult();

        result.sampleStart();

        boolean success = false;

        String loginName = context.getParameter("loginName");

        if (loginName == null) {
            loginName = "Sample User";
        }

        JMeterVariables vars = JMeterContextService.getContext().getVariables();

        vars.put("loginName", loginName);

        try {
            EntityManager currentEntityManager = emf.createEntityManager();

            currentEntityManager.getTransaction().begin();

            entityManager.set(currentEntityManager);
            
            SimpleUser user = new SimpleUser(loginName);

            identityManager.add(user);

            success = user.getId() != null && identityManager.getUser(loginName) != null;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            result.sampleEnd();
            result.setSuccessful(success);
            EntityManager currentEntityManager = this.entityManager.get();

            currentEntityManager.getTransaction().commit();
            currentEntityManager.close();
            
            entityManager.remove();
        }

        return result;
    }

    private static IdentityManager createIdentityManager() {
        IdentityConfiguration config = new IdentityConfiguration();

        addDefaultConfiguration(config);

        IdentityManager identityManager = new DefaultIdentityManager();

        identityManager.bootstrap(config, new DefaultIdentityStoreInvocationContextFactory() {
            @Override
            public EntityManager getEntityManager() {
                return entityManager.get();
            }
        });

        return identityManager;
    }

    private static void addDefaultConfiguration(IdentityConfiguration config) {
        JPAIdentityStoreConfiguration configuration = new JPAIdentityStoreConfiguration();

        configuration.addRealm(Realm.DEFAULT_REALM);
        configuration.addRealm("Testing");

        configuration.setIdentityClass(IdentityObject.class);
        configuration.setAttributeClass(IdentityObjectAttribute.class);
        configuration.setRelationshipClass(RelationshipObject.class);
        configuration.setRelationshipIdentityClass(RelationshipIdentityObject.class);
        configuration.setRelationshipAttributeClass(RelationshipObjectAttribute.class);
        configuration.setCredentialClass(CredentialObject.class);
        configuration.setCredentialAttributeClass(CredentialObjectAttribute.class);
        configuration.setPartitionClass(PartitionObject.class);

        FeatureSet.addFeatureSupport(configuration.getFeatureSet());
        FeatureSet.addRelationshipSupport(configuration.getFeatureSet());
        configuration.getFeatureSet().setSupportsCustomRelationships(true);
        configuration.getFeatureSet().setSupportsMultiRealm(true);

        config.addStoreConfiguration(configuration);
    }

}
