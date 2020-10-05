/*
	Copyright 2018 ITACA-SABIEN, http://www.sabien.upv.es
	Instituto Tecnologico de Aplicaciones de Comunicacion
	Avanzadas - Grupo Tecnologias para la Salud y el
	Bienestar (SABIEN)

	See the NOTICE file distributed with this work for additional
	information regarding copyright ownership

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

	  http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
 */
package es.upv.sabien.triplerest.server.servlet;

import javax.servlet.ServletException;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

import es.upv.sabien.triplerest.api.TripleREST;

/**
 * Activator that starts the servlet using OSGi http services.
 * 
 * @author alfiva
 *
 */
public class Activator implements BundleActivator {
    private static final String URL = "/store";
    public static BundleContext osgiContext = null;
    private HttpListener httpListener;
    private ServiceReference[] httpRefs;
    private Authenticator auth;
    private RemoteServlet remoteServlet;
    protected static TripleREST trest;

    public void start(BundleContext context) throws Exception {
	// Get uAAL context
	osgiContext = context;
	
	// Initialize APIs
	String impl = System.getProperty("es.upv.sabien.triplerest.server.servlet.impl",
		"es.upv.sabien.triplerest.impl.uaal.CHETripleRESTImpl");
	try {
	    trest = (TripleREST) Class.forName(impl).getConstructor(BundleContext.class)
		    .newInstance(context);
	} catch (NoSuchMethodException e) {
	    trest = (TripleREST) Class.forName(impl).getConstructor(new Class[] {})
		    .newInstance(new Object[] {});
	}

	// Initialize the servlet
	remoteServlet = new RemoteServlet();// TODO intantiate

	// Find the HTTP service
	auth = new Authenticator();
	httpListener = new HttpListener();
	String filter = "(objectclass=" + HttpService.class.getName() + ")";
	osgiContext.addServiceListener(httpListener, filter);
	httpRefs = osgiContext
		.getServiceReferences((String) null, filter);
	// TODO If there are more than 1 HttpService it gets registered in all!
	for (int i = 0; httpRefs != null && i < httpRefs.length; i++) {
	    httpListener.serviceChanged(new ServiceEvent(
		    ServiceEvent.REGISTERED, httpRefs[i]));
	}
    }

    public void stop(BundleContext arg0) throws Exception {
	for (int i = 0; httpRefs != null && i < httpRefs.length; i++) {
	    httpListener.serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, httpRefs[i]));
	}
	remoteServlet=null;
	auth=null;
	try {
	    trest.getClass().getMethod("close", null).invoke(trest, null);
	} catch (Exception e) {
	    // TODO warn
	}
    }

    /**
     * Listener for reacting to the presence of OSGi HTTP services
     * 
     * @author alfiva
     * 
     */
    private class HttpListener implements ServiceListener {
	public void serviceChanged(ServiceEvent event) {
	    switch (event.getType()) {
	    case ServiceEvent.REGISTERED:
	    case ServiceEvent.MODIFIED:
		register((HttpService) osgiContext.getService(event
			.getServiceReference()));
		break;
	    case ServiceEvent.UNREGISTERING:
		unregister((HttpService) osgiContext.getService(event
			.getServiceReference()));
		break;
	    default:
		break;
	    }
	}
    }

    /**
     * Registers the Servlet into the OSGi HTTP service.
     * 
     * @param http
     *            The referenced OSGi HTTP service
     * @return true if it managed to register
     */
    public boolean register(HttpService http) {
	try {
	    http.registerServlet(URL, remoteServlet, null, auth);
	} catch (ServletException e) {
//	    logE("register", "Exception while registering Servlet." + e);
	    return false;
	} catch (NamespaceException e) {
//	    logE("register", "Servlet Namespace exception; URL is already in use." + e);
	    return false;
	}
	return true;
    }

    /**
     * Unregisters the Servlet from the OSGi HTTP service.
     * 
     * @param http
     *            The referenced OSGi HTTP service
     * @return true if it managed to unregister
     */
    public boolean unregister(HttpService http) {
	try {
	    http.unregister(URL);
	} catch (IllegalArgumentException e) {
//	    logE("unregister",
//		    "Servlet cannot be unregistered: illegal argument.");
	    return false;
	}
//	logI("unregister", "Servlet stopped.");
	return true;
    }
    
}
