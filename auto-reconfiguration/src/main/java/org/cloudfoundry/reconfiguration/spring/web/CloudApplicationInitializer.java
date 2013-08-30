package org.cloudfoundry.reconfiguration.spring.web;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.cloudfoundry.reconfiguration.CloudAutoStagingBeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.FrameworkServlet;

/**
 * The {@link CloudApplicationInitializer} adds a
 * {@link CloudAutoStagingBeanFactoryPostProcessor} bean factory post processor
 * to all configurable application contexts, and to their parents, associated
 * with servlets. It also adds several property beans to configure each
 * application context. See
 * <code>src/main/resources/META-=INF/cloud/cloudfoundry-auto-reconfiguration-context.xml</code>
 * for corresponding bean definitions.
 * <p>
 * This class requires that, when the {@link #onStartup} method is called, all
 * application contexts which will be created have already been created, but
 * that none of them has yet been refreshed. Thus the BFPP will be added to all
 * application contexts associated with servlets, and their parents including
 * the root context, and will take effect when those application contexts are
 * refreshed.
 * <p>
 * To satisfy this requirement, this class implements
 * {@link WebApplicationInitializer} and positions itself as late as possible in
 * the ordering of {@link WebApplicationInitializer}s.
 * <p>
 * This class is coded defensively to avoid negatively impacting unusual types
 * of application.
 * <p>
 * 
 * @author Glyn Normington
 */
@Order(Ordered.LOWEST_PRECEDENCE)
final class CloudApplicationInitializer implements WebApplicationInitializer {

    private final Logger logger = Logger.getLogger(CloudApplicationInitializer.class.getName());

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
	for (ConfigurableApplicationContext applicationContext : getConfigurableApplicationContexts(servletContext)) {
	    /*
	     * Add a suitable BFPP to the application context.
	     * 
	     * Use a fresh BFPP instance each time since
	     * CloudAutoStagingBeanFactoryPostProcessor has an application
	     * context setter method.
	     */
	    BeanFactoryPostProcessor bfpp = createBeanFactoryPostProcessor();
	    logger.log(Level.FINE, "Adding bean factory post-processor '" + bfpp.getClass().getName()
		    + "' to application context '" + applicationContext + "'");
	    applicationContext.addBeanFactoryPostProcessor(bfpp);

	    addPropertiesBean(applicationContext, "__appCloudJpaPostgreSQLReplacementProperties", "hibernate.dialect",
		    "org.hibernate.dialect.PostgreSQLDialect");
	    addPropertiesBean(applicationContext, "__appCloudJpaMySQLReplacementProperties", "hibernate.dialect",
		    "org.hibernate.dialect.MySQLDialect");
	    addPropertiesBean(applicationContext, "__appCloudHibernatePostgreSQLReplacementProperties",
		    "hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
	    addPropertiesBean(applicationContext, "__appCloudHibernateMySQLReplacementProperties", "hibernate.dialect",
		    "org.hibernate.dialect.MySQLDialect");
	}
    }

    private void addPropertiesBean(ConfigurableApplicationContext applicationContext, String beanName, String key,
	    String value) {
	logger.log(Level.FINE, "Adding properties bean '" + beanName + "' with property <'" + key + "', '" + value
		+ "'>" + "' to application context '" + applicationContext + "'");
	Properties props = new Properties();
	props.put(key, value);
	applicationContext.getBeanFactory().registerSingleton(beanName, props);
    }

    private Set<ConfigurableApplicationContext> getConfigurableApplicationContexts(ServletContext servletContext) {
	Set<ConfigurableApplicationContext> applicationContexts = new HashSet<ConfigurableApplicationContext>();
	for (String servletName : getServletNames(servletContext)) {
	    ApplicationContext applicationContext = getApplicationContextForServlet(servletContext, servletName);
	    addApplicationContextAncestry(applicationContexts, applicationContext);
	}
	return applicationContexts;
    }

    private void addApplicationContextAncestry(Set<ConfigurableApplicationContext> applicationContexts,
	    ApplicationContext applicationContext) {
	while (applicationContext != null) {
	    addApplicationContext(applicationContexts, applicationContext);
	    applicationContext = applicationContext.getParent();
	}
    }

    private void addApplicationContext(Set<ConfigurableApplicationContext> applicationContexts,
	    ApplicationContext applicationContext) {
	if (applicationContext instanceof ConfigurableApplicationContext) {
	    applicationContexts.add((ConfigurableApplicationContext) applicationContext);
	} else {
	    logger.log(Level.WARNING, "Non-configurable application context encountered: " + applicationContext);
	}
    }

    private Set<String> getServletNames(ServletContext servletContext) {
	return servletContext.getServletRegistrations().keySet();
    }

    private WebApplicationContext getApplicationContextForServlet(ServletContext servletContext, String servletName) {
	try {
	    return WebApplicationContextUtils.getWebApplicationContext(servletContext,
		    webApplicationContextServletContextAttributeNameFromServletName(servletName));
	} catch (RuntimeException re) {
	    logger.log(Level.WARNING, "Failed to get application context for servlet: " + servletName, re);
	    return null;
	}
    }

    private String webApplicationContextServletContextAttributeNameFromServletName(String servletName) {
	return FrameworkServlet.SERVLET_CONTEXT_PREFIX + servletName;
    }

    private BeanFactoryPostProcessor createBeanFactoryPostProcessor() {
	return new CloudAutoStagingBeanFactoryPostProcessor();
    }

}
