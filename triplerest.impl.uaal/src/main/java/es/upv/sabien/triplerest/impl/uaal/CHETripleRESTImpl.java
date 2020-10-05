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
package es.upv.sabien.triplerest.impl.uaal;

import java.util.List;

import org.osgi.framework.BundleContext;
import org.universAAL.middleware.container.ModuleContext;
import org.universAAL.middleware.container.osgi.OSGiContainer;
import org.universAAL.middleware.owl.MergedRestriction;
import org.universAAL.middleware.rdf.PropertyPath;
import org.universAAL.middleware.rdf.Resource;
import org.universAAL.middleware.serialization.MessageContentSerializer;
import org.universAAL.middleware.serialization.MessageContentSerializerEx;
import org.universAAL.middleware.service.CallStatus;
import org.universAAL.middleware.service.DefaultServiceCaller;
import org.universAAL.middleware.service.ServiceCaller;
import org.universAAL.middleware.service.ServiceRequest;
import org.universAAL.middleware.service.ServiceResponse;
import org.universAAL.middleware.service.owls.process.ProcessOutput;
import org.universAAL.ontology.che.ContextHistoryService;

import es.upv.sabien.triplerest.api.AbstractSPARQLTripleREST;
import es.upv.sabien.triplerest.api.TripleRESTException;

/**
 * Implementation of Triple REST API using universAAL CHe SPARQL interface.
 * 
 * @author alfiva
 *
 */
public class CHETripleRESTImpl extends AbstractSPARQLTripleREST {
    protected ServiceCaller caller;
    protected MessageContentSerializer parser;
    private ModuleContext uaalContext;

    /**
     * This constructor needs the OSGI bundle context to access the universAAL
     * context and initialize its elements. Remember to .close() this instance
     * when the context goes down.
     * <p/>
     * Alternatively, you can instead use the empty constructor and set the
     * caller and parser manually with their setters, but do this before start
     * using the instance.
     * 
     * @param context
     *            The OSGI bundle context.
     */
    public CHETripleRESTImpl(BundleContext context){
	uaalContext = OSGiContainer.THE_CONTAINER
		.registerModule(new Object[] { context });
	parser = (MessageContentSerializerEx) context
		.getService(context.getServiceReference(
			MessageContentSerializerEx.class.getName()));
	caller = new DefaultServiceCaller(uaalContext);
    }
    
    /**
     * Sets the service caller in case an updated one is needed.
     * 
     * @param caller
     *            The new caller
     */
    public void setCaller(ServiceCaller caller) {
	this.caller = caller;
    }

    /**
     * Sets the Turtle parser in case an updated one is needed.
     * 
     * @param parser
     *            The new parser
     */
    public void setParser(MessageContentSerializer parser) {
	this.parser = parser;
    }
    
    /**
     * Call this when the uAAL context is closing, to properly free resources.
     */
    public void close() {
	caller.close();
    }

    public String get(String s, String p, String o) throws TripleRESTException {
	return get(s, p, o, 0, 0, 0);
    }

    public String doSPARQL(String query) {
	String output="http://ontology.upv.es/HistoryClient.owl#resultString";
	ServiceRequest getQuery = new ServiceRequest(new ContextHistoryService(
		null), null);
	MergedRestriction r = MergedRestriction.getFixedValueRestriction(
		ContextHistoryService.PROP_PROCESSES, query);

	getQuery.getRequestedService().addInstanceLevelRestriction(r,
		new String[] { ContextHistoryService.PROP_PROCESSES });
	getQuery.addSimpleOutputBinding(
		new ProcessOutput(output), new PropertyPath(null,
			true,
			new String[] { ContextHistoryService.PROP_RETURNS })
			.getThePath());
	ServiceResponse response = caller.call(getQuery);
	if (response.getCallStatus() == CallStatus.succeeded) {
	    try {
		List<Object> results = response.getOutput(output);
		// Uncomment this line if you want to show the raw results. Do
		// this for CONSTRUCT, ASK or DESCRIBE
		System.out.println("Result of SPARQL query was:\n" + results);
		// 1 CHE -> 1 result -> 1 String
		if (results != null && !results.isEmpty()
			&& results.get(0) != null) {
		    return (String) results.get(0);
		}
	    } catch (Exception e) {
		System.out.println("History Client: Result corrupt! " + e);
		return null;
	    }
	} else {
	    System.out.println("History Client - status of doSparqlQuery(): "
		    + response.getCallStatus());
	}
	System.out.println("History Client: Result empty or null");
	return null;
    }
    
    public String getRootIRI(String input) {
	Object res = parser.deserialize(input);
	if (res instanceof Resource) {
	    return ((Resource) res).getURI();
	} else if (res instanceof Object) {
	    return input; // A Literal. Use it directly
	} else {
	    return "";
	}
    }

}
