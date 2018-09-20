/*
	Copyright 2018 ITACA-SABIEN, http://www.sabien.upv.es
	Instituto Tecnologico de Aplicaciones de Comunicacion
	Avanzadas - Grupo Tecnologias para la Salud y el
	Bienestar (SABIEN)
 */
package es.upv.sabien.triplerest.impl.uaal;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.osgi.framework.BundleContext;
import org.universAAL.middleware.rdf.Resource;
import org.universAAL.middleware.rdf.TypeMapper;
import org.universAAL.middleware.service.CallStatus;
import org.universAAL.middleware.service.ServiceResponse;
import org.universAAL.middleware.service.owl.Service;
import org.universAAL.utilities.api.service.Arg;
import org.universAAL.utilities.api.service.Path;
import org.universAAL.utilities.api.service.low.Request;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import es.upv.sabien.triplerest.api.Construct;
import es.upv.sabien.triplerest.api.TripleRESTException;
import es.upv.sabien.triplerest.api.Utils;

public class MWCHETripleRESTImpl extends CHETripleRESTImpl {

    private static final String SERVICEURI = "http://ontology.universAAL.org/Metaverse.owl#auxService";
    private static final String SERVICEOUTPUT = "http://ontology.universAAL.org/Metaverse.owl#auxOut";

    public MWCHETripleRESTImpl(BundleContext context) {
	super(context);
    }
    
    @Override
    public String get(String s, String p, String o, int sdepth, int pdepth, int odepth) throws TripleRESTException {
	int type=Utils.coordinates(s, p, o);
	if (type != Utils.SXX && type !=Utils.SPX) {
	    return super.get(s, p, o, sdepth, pdepth, odepth);
//	    throw new TripleRESTException("Operation not allowed in this triple, yet");
	}
	// Find services that control this type of R.
	// Array of { Service URI, Property Controls, Type of controlled obj }
	String[][] foundSrvPrpTyp = findServices(s);
	if (foundSrvPrpTyp == null || foundSrvPrpTyp.length == 0) {
	    return super.get(s, p, o, sdepth, pdepth, odepth);
//	    throw new TripleRESTException(
//		    "No members found that provide this service");
	}
	Resource instance = null;

	// Build request and call each of the found services. Add all combined results
	for (int i = 0; i < foundSrvPrpTyp.length; i++) {
	    Service srv = (Service) Resource.getResource(foundSrvPrpTyp[i][0],
		    SERVICEURI);
	    Resource r = Resource.getResource(foundSrvPrpTyp[i][2], s);
	    if (r == null) {
		// !!! Service resolution may have found that restriction on the
		// controlled object is over an "abstract" type, which cannot be
		// instantiated, so... Ask CHE about it
		if (instance == null) {
		    Construct q = new Construct();
		    q.construct("<" + s + ">", null, null);
		    q.where("<" + s + ">", null, null);
		    String result = doSPARQL(q.build());
		    instance = (Resource) parser.deserialize(result);// Should only be 1
		    if (instance == null) {
			return super.get(s, p, o, sdepth, pdepth, odepth);
//			throw new TripleRESTException(
//				"The subject resource does not exist yet. "
//					+ "Currenlty, it is not possible to use a non-existent subject in these calls");
		    }
		}
		r = instance;
	    }
	    Request req = new Request(srv);
	    req.put(Path.at(foundSrvPrpTyp[i][1]), Arg.in(r));
	    if (p != null) {
		req.put(Path.at(foundSrvPrpTyp[i][1]).to(
			p), Arg.out(SERVICEOUTPUT));
	    } else {
		req.put(Path.at(foundSrvPrpTyp[i][1]), Arg.out(SERVICEOUTPUT));
	    }
	    ServiceResponse resp = caller.call(req);
	    if (resp.getCallStatus() == CallStatus.succeeded) {
		try {
		    List<Object> results = resp.getOutput(SERVICEOUTPUT);
		    for (Object result : results) {
			if (TypeMapper.isLiteral(result) && s != null
				&& p != null) {
			    // TODO can s & p be null here? TODO So far only
			    // works with s/p/o, so just 1 result, so...
			    /*
			     * allresults.add(s+" "+p+" \""+result+"\"^^<"+
			     * TypeMapper.getDatatypeURI(result)+"> .");
			     */
			    return "<" + s + "> <" + p + "> \"" + result + "\"^^<"
				    + TypeMapper.getDatatypeURI(result) + "> .";
			} else {
			    /*
			     * allresults.add(Activator.parser.serialize(result))
			     * ;
			     */
			    return parser.serialize(result);
			}
		    }
		} catch (Exception e) {
		    return super.get(s, p, o, sdepth, pdepth, odepth);
//		    throw new TripleRESTException("Services call: Result corrupt! ",
//			    e);
		}
	    }
	}
	/* return allresults.toArray(new String[]{}); */
	return super.get(s, p, o, sdepth, pdepth, odepth);
//	return "";
    }

    @Override
    public boolean post(String s, String p, String o, String input) throws TripleRESTException {
	int type=Utils.coordinates(s, p, o);
	if (type != Utils.SPX) {
	    return super.post(s, p, o, input);
//	    throw new TripleRESTException("Operation not allowed in this triple");
	}
	Object obj;
	if (input != null && !input.isEmpty()) {
	    obj = parser.deserialize(input);
	} else {
	    return super.post(s, p, o, input);
//	    throw new TripleRESTException("Operation needs input");
	}
	// Find services that control this type of R.
	// Array of { Service URI, Property Controls, Type of controlled obj }
	String[][] foundSrvPrpTyp = findServices(s);
	if (foundSrvPrpTyp == null || foundSrvPrpTyp.length == 0) {
	    return super.post(s, p, o, input);
//	    throw new TripleRESTException(
//		    "No members found that provide this service");
	}
	Resource instance = null;
	// Build request and call each of the found services
	boolean succeeded = false;
	for (int i = 0; i < foundSrvPrpTyp.length; i++) {
	    Service srv = (Service) Resource.getResource(foundSrvPrpTyp[i][0],
		    SERVICEURI);
	    Resource r = Resource.getResource(foundSrvPrpTyp[i][2], s);
	    if (r == null) {
		// !!! Service resolution may have found that restriction on the
		// controlled object is over an "abstract" type, which cannot be
		// instantiated, so... Ask CHE about it
		if (instance == null) {
		    Construct q = new Construct();
		    q.construct("<" + s + ">", null, null);
		    q.where("<" + s + ">", null, null);
		    String result = doSPARQL(q.build());
		    instance = (Resource) parser.deserialize(result);// Should only be 1
		    if (instance == null) {
			return super.post(s, p, o, input);
//			throw new TripleRESTException(
//				"The subject resource does not exist yet. "
//					+ "Currenlty, it is not possible to use a non-existent subject in these calls");
		    }
		}
		r = instance;
	    }
	    Request req = new Request(srv);
	    req.put(Path.at(foundSrvPrpTyp[i][1]), Arg.in(r));
	    if (p != null) {
		req.put(Path.at(foundSrvPrpTyp[i][1]).to(
			p), Arg.add(obj));
	    } else {
		req.put(Path.at(foundSrvPrpTyp[i][1]), Arg.add(obj));
	    }
	    ServiceResponse resp = caller.call(req);
	    if (resp.getCallStatus() == CallStatus.succeeded) {
		if (!succeeded)
		    succeeded = true;
	    }
	}
	return succeeded;
    }

    @Override
    public boolean put(String s, String p, String o, String input) throws TripleRESTException {
	int type=Utils.coordinates(s, p, o);
	if ( type!=Utils.SXX && type!=Utils.SPX && type!=Utils.SPO) {
	    return super.put(s, p, o, input);
//	    throw new TripleRESTException("Operation not allowed in this triple");
	}
	Object res;
	if (input != null && !input.isEmpty()) {
	    res = parser.deserialize(input);
	} else {
	    return super.put(s, p, o, input);
//	    throw new TripleRESTException("Operation needs input");
	}
	// Find services that control this type of R.
	// Array of { Service URI, Property Controls, Type of controlled obj }
	String[][] foundSrvPrpTyp = findServices(s);
	if (foundSrvPrpTyp == null || foundSrvPrpTyp.length == 0) {
	    return super.put(s, p, o, input);
//	    throw new TripleRESTException(
//		    "No members found that provide this service");
	}
	Resource instance = null;
	boolean succeeded = false;
	// Build request and call each of the found services
	for (int i = 0; i < foundSrvPrpTyp.length; i++) {
	    Service srv = (Service) Resource.getResource(foundSrvPrpTyp[i][0],
		    SERVICEURI);
	    Resource r = Resource.getResource(foundSrvPrpTyp[i][2], s);
	    if (r == null) {
		// !!! Service resolution may have found that restriction on the
		// controlled object is over an "abstract" type, which cannot be
		// instantiated, so... Ask CHE about it
		if (instance == null) {
		    Construct q = new Construct();
		    q.construct("<" + s + ">", null, null);
		    q.where("<" + s + ">", null, null);
		    String result = doSPARQL(q.build());
		    instance = (Resource) parser.deserialize(result);// Should only be 1
		    if (instance == null) {
			return super.put(s, p, o, input);
//			throw new TripleRESTException(
//				"The subject resource does not exist yet. "
//					+ "Currenlty, it is not possible to use a non-existent subject in these calls");
		    }
		}
		r = instance;
	    }
	    Request req = new Request(srv);
	    req.put(Path.at(foundSrvPrpTyp[i][1]), Arg.in(r));
	    if (p != null) {
		req.put(Path.at(foundSrvPrpTyp[i][1]).to(
			p), Arg.change(res));
	    } else {
		req.put(Path.at(foundSrvPrpTyp[i][1]), Arg.change(res));
	    }
	    if (o != null) {
		// TODO Restrict services with value of o in p exactly this one.
		// But MW does not allow that yet
	    }
	    ServiceResponse resp = caller.call(req);
	    if (resp.getCallStatus() == CallStatus.succeeded) {
		if (!succeeded)
		    succeeded = true;
	    }
	}
	return succeeded;
    }

    @Override
    public boolean del(String s, String p, String o) throws TripleRESTException {
	int type=Utils.coordinates(s, p, o);
	if (type!=Utils.SXX){
	    return super.del(s, p, o);
//	    throw new TripleRESTException("Operation not allowed in this triple, yet");
	}
	// Find services that control this type of R.
	// Array of { Service URI, Property Controls, Type of controlled obj }
	String[][] foundSrvPrpTyp = findServices(s);
	if (foundSrvPrpTyp == null || foundSrvPrpTyp.length == 0) {
	    return super.del(s, p, o);
//	    throw new TripleRESTException(
//		    "No members found that provide this service");
	}
	Resource instance = null;
	boolean succeeded = false;
	// Build request and call each of the found services
	for (int i = 0; i < foundSrvPrpTyp.length; i++) {
	    Service srv = (Service) Resource.getResource(foundSrvPrpTyp[i][0],
		    SERVICEURI);
	    Resource r = Resource.getResource(foundSrvPrpTyp[i][2], s);
	    if (r == null) {
		// !!! Service resolution may have found that restriction on the
		// controlled object is over an "abstract" type, which cannot be
		// instantiated, so... Ask CHE about it
		if (instance == null) {
		    Construct q = new Construct();
		    q.construct("<" + s + ">", null, null);
		    q.where("<" + s + ">", null, null);
		    String result = doSPARQL(q.build());
		    instance = (Resource) parser.deserialize(result);// Should only be 1
		    if (instance == null) {
			return super.del(s, p, o);
//			throw new TripleRESTException(
//				"The subject resource does not exist yet. "
//					+ "Currenlty, it is not possible to use a non-existent subject in these calls");
		    }
		}
		r = instance;
	    }
	    Request req = new Request(srv);
	    req.put(Path.at(foundSrvPrpTyp[i][1]), Arg.remove(r));
	    ServiceResponse resp = caller.call(req);
	    if (resp.getCallStatus() == CallStatus.succeeded) {
		if (!succeeded)
		    succeeded = true;
	    }
	}
	return succeeded;
    }

    private String[][] findServices(String controlledResourceURI) throws TripleRESTException {
	String query = "SELECT ?s ?p ?x WHERE { "
		+ "?p a <http://www.w3.org/2002/07/owl#ObjectProperty>."
		+ "?p <http://www.w3.org/2000/01/rdf-schema#domain> ?s ."
		+ "?s <http://www.w3.org/2000/01/rdf-schema#subClassOf> <http://www.daml.org/services/owl-s/1.1/Service.owl#Service> ."
		+ "?s <http://www.w3.org/2000/01/rdf-schema#subClassOf> ?r ."
		+ "?r a <http://www.w3.org/2002/07/owl#Restriction> . "
		+ "?r <http://www.w3.org/2002/07/owl#allValuesFrom> ?x ."
		+ "<" + controlledResourceURI + "> a ?x . }";
	String result = doSPARQL(query);
	if (result != null) {
	    String corrected = result;
	    if (result.trim().startsWith("[")) {
		corrected = corrected.replaceFirst("\\[", "");
	    }
	    if (result.endsWith("]")) {
		corrected = corrected.substring(0, corrected.length() - 1);
	    }

	    InputStream is;
	    String service = null, property = null, type = null;
	    try {
		is = new ByteArrayInputStream(corrected.getBytes("UTF-8"));

		DocumentBuilderFactory dbFactory = DocumentBuilderFactory
			.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(is);
		NodeList nodes = doc.getElementsByTagName("result");
		String[][] outputlist = new String[nodes.getLength()][3];
		// TODO Looks like you cant simply loop like this with
		// documents, there are null nodes in between
		for (int i = 0; i < nodes.getLength(); i++) {
		    Node node = nodes.item(i);
		    NodeList bindings = node.getChildNodes();
		    for (int j = 0; j < bindings.getLength(); j++) {
			Node binding = bindings.item(j);
			try {
			    if (binding.getAttributes().getNamedItem("name")
				    .getNodeValue().equals("s")) {
				service = binding.getTextContent().trim();
			    } else if (binding.getAttributes()
				    .getNamedItem("name").getNodeValue()
				    .equals("p")) {
				property = binding.getTextContent().trim();
			    } else if (binding.getAttributes()
				    .getNamedItem("name").getNodeValue()
				    .equals("x")) {
				type = binding.getTextContent().trim();
			    }
			} catch (NullPointerException e) {
			    // System.out.println("For some reason sometimes a binding is null"+binding.getAttributes());
			} catch (Exception e) {
			    throw new TripleRESTException(
				    "Cannot parse the found services", e);
			}
		    }
		    outputlist[i] = new String[] { service, property, type };
		}
		return outputlist;
	    } catch (Exception e) {
		throw new TripleRESTException("Cannot parse the found services", e);
	    }
	}
	return null;
    }

}
