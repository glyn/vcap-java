package org.cloudfoundry.reconfiguration.spring.web;

import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * Cloud Foundry {@link ServletContainerInitializer}
 * 
 * <p>
 * The {@link CloudServletContainerInitializer} modifies the servlet context
 * analogously to the way the Cloud Foundry Java Buildpack modifies web.xml.
 * This supports Servlet 3 applications which do not necessarily have a web.xml.
 * </p>
 * <p>
 * The modifications are as follows:
 * <ul>
 * <li>TBD</li>
 * </ul>
 * </p>
 * 
 * @author Glyn Normington
 */
public final class CloudServletContainerInitializer implements ServletContainerInitializer {

    private static final String CONTEXT_LOCATION_DEFAULT = "/WEB-INF/applicationContext.xml";
    private static final String CONTEXT_CONFIG_LOCATION = "contextConfigLocation";
    private static final String CONTEXT_CLASS = "contextClass";
    private static final String CONTEXT_CLASS_ANNOTATION = "org.springframework.web.context.support.AnnotationConfigWebApplicationContext";
    private static final String CONTEXT_LOCATION_ADDITIONAL_ANNOTATION = "org.cloudfoundry.reconfiguration.spring.web.CloudAppAnnotationConfigAutoReconfig";
    private static final String CONTEXT_LOCATION_ADDITIONAL_XML = "classpath:META-INF/cloud/cloudfoundry-auto-reconfiguration-context.xml";

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStartup(Set<Class<?>> _, ServletContext servletContext) throws ServletException {
	augmentRootContext(servletContext);
	augmentServletContexts(servletContext);
    }

    private void augmentRootContext(ServletContext servletContext) {
	/*
	 * The equivalent web.xml modifications are guarded by a test for the
	 * root context having a listener whose class name contains
	 * "ContextLoaderListener". This test is not implementable using the
	 * ServletContext API.
	 */
	augmentContextConfigLocations(servletContext);
    }

    private void augmentContextConfigLocations(ServletContext servletContext) {
	String contextConfigLocationValue = servletContext.getInitParameter(CONTEXT_CONFIG_LOCATION);
	if (contextConfigLocationValue == null) {
	    contextConfigLocationValue = CONTEXT_LOCATION_DEFAULT;
	}
	String[] contextConfigLocations = contextConfigLocationValue.split("[,;\\s]+");
	String additionalContextConfigLocation = getAdditionalContextConfigLocation(servletContext);
	int n = contextConfigLocations.length;
	String[] augmentedContextConfigLocations = new String[n + 1];
	System.arraycopy(contextConfigLocations, 0, augmentedContextConfigLocations, 0, n);
	augmentedContextConfigLocations[n] = additionalContextConfigLocation;
	String augmentedContextConfigLocationValue = ...;
	// TODO: join augmentedContextConfigLocations with commas to form augmentedContextConfigLocationValue
	servletContext.setInitParameter(CONTEXT_CONFIG_LOCATION, augmentedContextConfigLocationValue);
    }

    private String getAdditionalContextConfigLocation(ServletContext servletContext) {
	String contextClassValue = servletContext.getInitParameter(CONTEXT_CLASS);
	return CONTEXT_CLASS_ANNOTATION.equals(contextClassValue) ? CONTEXT_LOCATION_ADDITIONAL_ANNOTATION
		: CONTEXT_LOCATION_ADDITIONAL_XML;
    }

    private void augmentServletContexts(ServletContext servletContext) {
	// TODO Auto-generated method stub

    }

}
