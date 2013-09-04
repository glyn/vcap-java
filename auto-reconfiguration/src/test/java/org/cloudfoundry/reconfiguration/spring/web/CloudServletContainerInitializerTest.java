package org.cloudfoundry.reconfiguration.spring.web;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.cloudfoundry.reconfiguration.CloudAutoStagingBeanFactoryPostProcessor;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.servlet.FrameworkServlet;

public class CloudServletContainerInitializerTest {

    private static final String TEST_SERVLET_NAME = "testServlet";

    private static final String TEST_WEB_APPLICATION_CONTEXT_SERVLET_CONTEXT_ATTRIBUTE_NAME = FrameworkServlet.SERVLET_CONTEXT_PREFIX
	    + TEST_SERVLET_NAME;

    private ConfigurableWebApplicationContext webAC;

    private ConfigurableApplicationContext rootAC;

    private ConfigurableListableBeanFactory webACBeanFactory;
    
    private ConfigurableListableBeanFactory rootACBeanFactory;

    @Test
    public void testOnStartup() throws ServletException {
	ServletContext servletContext = mock(ServletContext.class);
	stubServletRegistrations(servletContext);
	stubApplicationContexts(servletContext);
	CloudServletContainerInitializer cloudApplicationInitializer = new CloudServletContainerInitializer();
	
	cloudApplicationInitializer.onStartup(null, servletContext);
	
	verifyBeanFactoryPostProcessorAdded(webAC);
	verifyBeanFactoryPostProcessorAdded(rootAC);
	verifyPropertyBeansRegistered(webACBeanFactory);
	verifyPropertyBeansRegistered(rootACBeanFactory);
    }

    private void verifyBeanFactoryPostProcessorAdded(ConfigurableApplicationContext ac) {
	verify(ac).addBeanFactoryPostProcessor(any(CloudAutoStagingBeanFactoryPostProcessor.class));
    }

    private void verifyPropertyBeansRegistered(ConfigurableListableBeanFactory bf) {
	verify(bf).registerSingleton(eq("__appCloudJpaPostgreSQLReplacementProperties"), any(Properties.class));
	verify(bf).registerSingleton(eq("__appCloudJpaMySQLReplacementProperties"), any(Properties.class));
	verify(bf).registerSingleton(eq("__appCloudHibernatePostgreSQLReplacementProperties"), any(Properties.class));
	verify(bf).registerSingleton(eq("__appCloudHibernateMySQLReplacementProperties"), any(Properties.class));
    }

    private void stubApplicationContexts(ServletContext servletContext) {
	webAC = mock(ConfigurableWebApplicationContext.class);
	rootAC = mock(ConfigurableApplicationContext.class);
	when(rootAC.getParent()).thenReturn(null);
	when(webAC.getParent()).thenReturn(rootAC);
	when(servletContext.getAttribute(TEST_WEB_APPLICATION_CONTEXT_SERVLET_CONTEXT_ATTRIBUTE_NAME)).thenReturn(webAC);
	
	webACBeanFactory = mock(ConfigurableListableBeanFactory.class);
	when(webAC.getBeanFactory()).thenReturn(webACBeanFactory);
	
	rootACBeanFactory = mock(ConfigurableListableBeanFactory.class);
	when(rootAC.getBeanFactory()).thenReturn(rootACBeanFactory);
    }

    private void stubServletRegistrations(ServletContext servletContext) {
	final Map<String, ServletRegistration> servletRegistrations = new HashMap<String, ServletRegistration>();
	servletRegistrations.put(TEST_SERVLET_NAME, null);
	Answer<Map<String, ? extends ServletRegistration>> answer = new Answer<Map<String, ? extends ServletRegistration>>() {

	    @Override
	    public Map<String, ? extends ServletRegistration> answer(InvocationOnMock invocation) throws Throwable {
		return servletRegistrations;
	    }
	};
	when(servletContext.getServletRegistrations()).thenAnswer(answer);
    }

}
