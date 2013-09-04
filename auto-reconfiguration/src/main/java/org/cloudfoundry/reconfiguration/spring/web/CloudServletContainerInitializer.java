package org.cloudfoundry.reconfiguration.spring.web;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.cloudfoundry.reconfiguration.CloudAutoStagingBeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.FrameworkServlet;

/**
 * The {@link CloudServletContainerInitializer} adds a
 * {@link CloudAutoStagingBeanFactoryPostProcessor} bean factory post processor
 * to all configurable application contexts, and to their parents, associated
 * with servlets. It also adds several property beans to configure each
 * application context. See
 * <code>src/main/resources/META-INF/cloud/cloudfoundry-auto-reconfiguration-context.xml</code>
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
 * {@link ServletContainerInitializer} and is ordered after
 * <code>spring_web</code> in
 * <code>src/main/resources/META-INF/services/web-fragment.xml</code>.
 * <p>
 * This class is coded defensively to avoid negatively impacting unusual types
 * of application.
 * <p>
 * 
 * @author Glyn Normington
 */
public final class CloudServletContainerInitializer implements ServletContainerInitializer {

    private final Logger logger = Logger.getLogger(CloudServletContainerInitializer.class.getName());

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStartup(Set<Class<?>> _, ServletContext servletContext) throws ServletException {
	logger.log(Level.INFO, "Entered CloudApplicationInitializer.onStartup");
	for (ConfigurableApplicationContext applicationContext : getConfigurableApplicationContexts(servletContext)) {
	    /*
	     * Add a suitable BFPP to the application context.
	     * 
	     * Use a fresh BFPP instance each time since
	     * CloudAutoStagingBeanFactoryPostProcessor has an application
	     * context setter method.
	     */
	    BeanFactoryPostProcessor bfpp = createBeanFactoryPostProcessor();
	    logger.log(Level.INFO, "Adding bean factory post-processor '" + bfpp.getClass().getName()
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
	logger.log(Level.INFO, "Exiting CloudApplicationInitializer.onStartup");
    }

    private void addPropertiesBean(ConfigurableApplicationContext applicationContext, String beanName, String key,
	    String value) {
	logger.log(Level.INFO, "Adding properties bean '" + beanName + "' with property <'" + key + "', '" + value
		+ "'>" + "' to application context '" + applicationContext + "'");
	Properties props = new Properties();
	props.put(key, value);
	applicationContext.getBeanFactory().registerSingleton(beanName, props);
    }

    private Set<ConfigurableApplicationContext> getConfigurableApplicationContexts(ServletContext servletContext) {
	Set<ConfigurableApplicationContext> applicationContexts = new HashSet<ConfigurableApplicationContext>();
	for (String servletName : getServletNames(servletContext)) {
	    logger.log(Level.INFO, "Processing servlet " + servletName);
	    ApplicationContext applicationContext = getApplicationContextForServlet(servletContext, servletName);
	    logger.log(Level.INFO, "Application context detected " + applicationContext);
	    addApplicationContextAncestry(applicationContexts, applicationContext);
	}
	logger.log(Level.INFO, "Application contexts detected: " + applicationContexts);
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
	    logger.log(Level.INFO, "Found ConfigurableApplicationContext " + applicationContext);
	    applicationContexts.add((ConfigurableApplicationContext) applicationContext);
	} else {
	    logger.log(Level.WARNING, "Non-configurable application context encountered: " + applicationContext);
	}
    }

    private Set<String> getServletNames(ServletContext servletContext) {
	Set<String> servletNames = servletContext.getServletRegistrations().keySet();
	logger.log(Level.INFO, "Servlet names: " + servletNames);
	return servletNames;
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
