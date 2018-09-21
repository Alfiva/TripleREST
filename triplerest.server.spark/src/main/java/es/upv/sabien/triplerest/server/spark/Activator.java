/*
	Copyright 2018 ITACA-SABIEN, http://www.sabien.upv.es
	Instituto Tecnologico de Aplicaciones de Comunicacion
	Avanzadas - Grupo Tecnologias para la Salud y el
	Bienestar (SABIEN)
 */
package es.upv.sabien.triplerest.server.spark;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import es.upv.sabien.triplerest.api.TripleREST;
import spark.Request;
import spark.Spark;
import static spark.Spark.*;

import java.net.URI;

/**
 * Activator that starts and builds up the RESTlet built with Spark that handles
 * the calls to the Triple REST.
 * 
 * @author alfiva
 * 
 */
public class Activator implements BundleActivator {

    private Authenticator auth;
    private TripleREST trest;

    public void start(BundleContext context) throws Exception {
	String impl = System.getProperty("es.upv.sabien.triplerest.server.spark.impl",
		"es.upv.sabien.triplerest.impl.uaal.CHETripleRESTImpl");
	try {
	    trest = (TripleREST) Class.forName(impl).getConstructor(BundleContext.class)
		    .newInstance(context);
	} catch (NoSuchMethodException e) {
	    trest = (TripleREST) Class.forName(impl).getConstructor(new Class[] {})
		    .newInstance(new Object[] {});
	}
	auth = new Authenticator();
	// AUTH
	before((rq, rs) -> {
	    if (!auth.handleSecurity(rq, rs)) {
	        halt(401, "Authorization information missing or incorrect");
	    }
	});
	//STORE/S
	get("/store/:s", (rq, rs) -> trest.get(process(":s",rq), null, null,
		Integer.parseInt(rq.queryParams("s")),
		Integer.parseInt(rq.queryParams("p")),
		Integer.parseInt(rq.queryParams("o"))));
	put("/store/:s", (rq, rs) -> trest.put(process(":s",rq), null, null, rq.body()));
	delete("/store/:s", (rq, rs) -> trest.del(process(":s",rq), null, null));
	//STORE/S/P
	get("/store/:s/:p", (rq, rs) -> trest.get(process(":s",rq), process(":p",rq), null,
		Integer.parseInt(rq.queryParams("s")),
		Integer.parseInt(rq.queryParams("p")),
		Integer.parseInt(rq.queryParams("o"))));
	put("/store/:s/:p", (rq, rs) -> trest.put(process(":s",rq), process(":p",rq), null, rq.body()));
	post("/store/:s/:p", (rq, rs) -> trest.post(process(":s",rq), process(":p",rq), null, rq.body()));
	delete("/store/:s/:p", (rq, rs) -> trest.del(process(":s",rq), process(":p",rq), null));
	//STORE/S/P/O
	get("/store/:s/:p/:o", (rq, rs) -> trest.get(process(":s",rq), process(":p",rq), process(":o",rq),
		Integer.parseInt(rq.queryParams("s")),
		Integer.parseInt(rq.queryParams("p")),
		Integer.parseInt(rq.queryParams("o"))));
	put("/store/:s/:p/:o", (rq, rs) -> trest.put(process(":s",rq), process(":p",rq), process(":o",rq), rq.body()));
	delete("/store/:s/:p/:o", (rq, rs) -> trest.del(process(":s",rq), process(":p",rq), process(":o",rq)));
	//HEADERS
	after((rq, rs) -> {
	    rs.header("Content-Type", "text/turtle; charset=UTF-8");
	});
    }

    public void stop(BundleContext context) throws Exception {
	Spark.stop();
	try {
	    trest.getClass().getMethod("close", null).invoke(trest, null);
	} catch (Exception e) {
	    // TODO warn
	}
    }

    
    /**
     * Check if the path segment is one of the following, and process it
     * appropriately to be accepted by REST interface:
     * 
     * null > wildcard > IRI > Literal > prefixed:IRI > prefixed:type:Literal >
     * int > float > long > boolean > Label
     * 
     * @param x
     *            Path segment
     * @param rq
     *            Request
     * @return
     */
    private static String process(String x, Request rq){
	String param = rq.params(x);
	// null
	if (param==null) return null; //TODO error
	// wildcard
	if (param.equals("*")) return null;
	// IRI
	try {
	    URI uri=new URI(param);
	    String scheme=uri.getScheme();
	    String prefix=rq.queryParams(scheme);
	    if (prefix!=null){ // pref:whatever?pref=namespace.owl
		return prefix+"#"+param.substring(scheme.length()+1);
	    }else{ // pref is actually the scheme of a "valid" IRI -> urn:whatever
		return param;
	    }
	}catch(Exception e){
	 // Not an IRI, continue
	}
	// Literal
	    if (param.contains("^^")) return param;
	    // prefixed:IRI
	    if (param.contains(":")){
		if(param.split(":").length==1){
		    String[] split=param.split(":",2);
		    return rq.queryParams(split[0])+"#"+split[1];
		}else if(param.split(":").length==2){
		    // prefixed:type:Literal
		    String[] split=param.split(":",3);
		    return "\""+split[2]+"\"^^<"+rq.queryParams(split[0])!=null?rq.queryParams(split[0]):split[0]+"#"+split[1]+">";
		}else{
		    // TODO Error
		    return param;
		}
	    }else{
		// Int
		try{
		    return "\""+Integer.parseInt(param)+"\"^^<http://www.w3.org/2001/XMLSchema#int>";
		}catch(NumberFormatException e1){
		    // Float
		    try{
			return "\""+Float.parseFloat(param)+"\"^^<http://www.w3.org/2001/XMLSchema#float>";
		    }catch(NumberFormatException e2){
			// Long
			try{
			    return "\""+Long.parseLong(param)+"\"^^<http://www.w3.org/2001/XMLSchema#long>";
			}catch(NumberFormatException e3){
			    // Boolean
			    if (param.equals("true")) return "\"true\"^^<http://www.w3.org/2001/XMLSchema#boolean>";
			    if (param.equals("false")) return "\"false\"^^<http://www.w3.org/2001/XMLSchema#boolean>";
			    // Label
			    return param;
			}
		    }
		}
	    }
    }
}
