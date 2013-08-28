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
public class CloudServletContainerInitializer implements
		ServletContainerInitializer {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onStartup(Set<Class<?>> _, ServletContext servletContext)
			throws ServletException {
		// TODO Auto-generated method stub

	}

}
