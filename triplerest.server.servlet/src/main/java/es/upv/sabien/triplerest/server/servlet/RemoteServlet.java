/*
	Copyright 2018 ITACA-SABIEN, http://www.sabien.upv.es
	Instituto Tecnologico de Aplicaciones de Comunicacion
	Avanzadas - Grupo Tecnologias para la Salud y el
	Bienestar (SABIEN)
 */
package es.upv.sabien.triplerest.server.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Hashtable;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import es.upv.sabien.triplerest.api.TripleRESTException;
import es.upv.sabien.triplerest.api.Utils;

/**
 * An HTTP Servlet that handles the calls to the Triple REST.
 * 
 * @author alfiva
 * 
 */
public class RemoteServlet extends javax.servlet.http.HttpServlet {

    private static final long serialVersionUID = -1931914654539856412L;
    private static final String WILDCARD = "*";

    public RemoteServlet() {

    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
	    throws ServletException, IOException {
	resp.setContentType("text/turtle");
	resp.setCharacterEncoding("UTF-8");
	Boolean result = null;
	String input = getInputString(req.getReader());
	PrintWriter os = null;
	try {
	    String[] parts = splitURL(req);
	    result = Activator.trest.put(parts[0], parts[1], parts[2], input);
	    resp.setStatus(HttpServletResponse.SC_ACCEPTED);
	    resp.setStatus(HttpServletResponse.SC_OK);
	    os = resp.getWriter();
	    if (result) {
		switch (Utils.coordinates(parts[0], parts[1], parts[2])) {
		case Utils.SXX:
		    os.print(Activator.trest.get(Utils.getRootIRI(input), null, null));
		    break;
		case Utils.SPX:
		    os.print(Activator.trest.get(parts[0], parts[1], null));
		    break;
		case Utils.SPO:
		    os.print(Activator.trest.get(parts[0], parts[1], Utils.getRootIRI(input)));
		    break;
		default:
		    os.print("Invalid triple request");
		    return;
		}
	    }
	} catch (TripleRESTException e) {
	    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	} finally {
	    if (os != null) {
		os.flush();
		os.close();
	    }
	}
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
	    throws ServletException, IOException {
	resp.setContentType("text/turtle");
	resp.setCharacterEncoding("UTF-8");
	Boolean result = null;
	PrintWriter os = null;
	try {
	    String[] parts = splitURL(req);
	    result = Activator.trest.del(parts[0], parts[1], parts[2]);
	    resp.setStatus(HttpServletResponse.SC_ACCEPTED);
	    resp.setStatus(HttpServletResponse.SC_OK);
	    os = resp.getWriter();
	    if (result) {
		switch (Utils.coordinates(parts[0], parts[1], parts[2])) {
		case Utils.SXX:
		    os.print(Activator.trest.get(null, null, null));
		    break;
		case Utils.SPX:
		    os.print(Activator.trest.get(parts[0], null, null));
		    break;
		case Utils.SPO:
		    os.print(Activator.trest.get(parts[0], parts[1], null));
		    break;
		default:
		    os.print("Invalid triple request");
		    return;
		}
	    }
	} catch (TripleRESTException e) {
	    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	} finally {
	    if (os != null) {
		os.flush();
		os.close();
	    }
	}
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
	    throws ServletException, IOException {
	resp.setContentType("text/turtle");
	resp.setCharacterEncoding("UTF-8");
	String result = null;
	Hashtable<String, String> extras = processExtras(req);
	PrintWriter os = null;
	try {
	    String[] parts = splitURL(req);
	    result = Activator.trest.get(parts[0], parts[1], parts[2],
		    Integer.parseInt(extras.get("s")),
		    Integer.parseInt(extras.get("p")),
		    Integer.parseInt(extras.get("o")));
	    resp.setStatus(HttpServletResponse.SC_ACCEPTED);
	    resp.setStatus(HttpServletResponse.SC_OK);
	    os = resp.getWriter();
	    if (result != null) {
		os.print(result);
	    }
	} catch (TripleRESTException e) {
	    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	} finally {
	    if (os != null) {
		os.flush();
		os.close();
	    }
	}
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
	    throws ServletException, IOException {
	resp.setContentType("text/turtle");
	resp.setCharacterEncoding("UTF-8");
	Boolean result = null;
	String input = getInputString(req.getReader());
	PrintWriter os = null;
	try {
	    String[] parts = splitURL(req);
	    result = Activator.trest.post(parts[0], parts[1], parts[2], input);
	    resp.setStatus(HttpServletResponse.SC_ACCEPTED);
	    resp.setStatus(HttpServletResponse.SC_OK);
	    os = resp.getWriter();
	    if (result) {
		switch (Utils.coordinates(parts[0], parts[1], parts[2])) {
		case Utils.SPX:
		    os.print(Activator.trest.get(parts[0], parts[1], Utils.getRootIRI(input)));
		    break;
		default:
		    os.print("Invalid triple request");
		    return;
		}
	    }
	} catch (TripleRESTException e) {
	    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	} finally {
	    if (os != null) {
		os.flush();
		os.close();
	    }
	}
    }
    
    private Hashtable<String, String> processExtras(HttpServletRequest req){
	Hashtable<String, String> result=new Hashtable<String, String>();
	String query=req.getQueryString();
	if(query!=null){
	    String[] extras=query.split("&");
	    for(String extra:extras){
		String[] parts=extra.split("=");
		if(parts.length==2){
		    result.put(parts[0], parts[1]);
		}
	    }
	}
	if(!result.containsKey("s")){
	    result.put("s", "0");
	}
	if(!result.containsKey("p")){
	    result.put("p", "0");
	}
	if(!result.containsKey("o")){
	    result.put("o", "0");
	}
	return result;
    }
    
    public static String getInputString(BufferedReader br) throws IOException{
	StringBuilder builder=new StringBuilder();
	String line=br.readLine();
	while (line!=null){
	    builder.append(line);
	    line=br.readLine();
	}
	br.close();
	return builder.toString();
    }
    
    /**
     * Given a REST path encoded in the form /<subject>/<predicate>/<object> it
     * slices it and returns an array with [subject, predicate, object]. If any
     * part is a wildcard, the value in the array will be null.
     * 
     * @param url
     *            The encoded path. E.g: /<http://foo.bar>/a/"sam/ple"^^<http://foo.bar>
     * @param rq 
     * @return The sliced array. E.g: [ <http://foo.bar> , a , "sam/ple"^^<http://foo.bar> ]
     * @throws TripleRESTException 
     */
    public static String[] splitURL(HttpServletRequest rq) throws TripleRESTException {// TODO \/(?![^<]*>)(?![^"]*"\^\^)
	String[] result = new String[] { null, null, null };
	String url=rq.getRequestURI().substring(rq.getServletPath().length());
	if (url.startsWith("/")) url = url.substring(1); // Remove initial /
	String[] parts = url.split("/");
	if (parts.length == 1
		&& (parts[0].trim().isEmpty() || parts[0].trim().equals("/"))) { // Empty URL or /
	    return result;
	}
	if (parts.length > 0) {
	    if (parts[0] != null && !parts[0].isEmpty()
		    && !parts[0].equals(WILDCARD)) {
		result[0] = process(parts[0], rq);
	    }
	}
	if (parts.length > 1) {
	    if (parts[1] != null && !parts[1].isEmpty()
		    && !parts[1].equals(WILDCARD)) {
		result[1] = process(parts[1],rq);
	    }
	}
	if (parts.length > 2) {
	    if (parts[2] != null && !parts[2].isEmpty()
		    && !parts[2].equals(WILDCARD)) {
		result[2] = process(parts[2],rq);
	    }
	}
	return result;
    }
    
    /**
     * Check if the path segment is one of the following, and process it
     * appropriately to be accepted by REST interface:
     * 
     * null > wildcard > IRI > Literal > prefixed:IRI > prefixed:type:Literal
     * 
     * @param x
     *            Path segment
     * @param rq
     *            HTTP Request
     * @return
     * @throws TripleRESTException
     *             If the processed segment will not be accepted by TripleREST
     */
    private static String process(String s, HttpServletRequest rq)
	    throws TripleRESTException {
	String x;
	try {
	    x = URLDecoder.decode(s, "UTF-8");
	} catch (UnsupportedEncodingException e4) { // This should not happen
	    x = s;
	}
	// null
	if (x == null)
	    throw new TripleRESTException("Cannot decode segment from URL: " + s);
	// wildcard
	if (x.equals("*"))
	    return null;
	// IRI
	try {
	    URI uri = new URI(x);
	    String scheme = uri.getScheme();
	    if (scheme != null) { // something:something   (absolute IRI)
		String prefix = rq.getParameter(scheme);
		if (prefix != null) { // pref:something?pref=namespace.owl
		    return prefix + "#" + x.substring(scheme.length() + 1);
		} else { // pref:something > pref should be scheme (urn:) or known prefix (rdf:)
		    return x;
		}
	    } else { // something   (relative IRI)    TODO process plain literals
		throw new TripleRESTException("Relative IRIs are not allowed in segments: " + s);
	    }
	} catch (Exception e) {
	    // Not an IRI, check if Literal
	    if (x.contains("\"^^<")) {
		return x;
	    } else {
		throw new TripleRESTException("Segment is not an IRI nor a Literal: " + s);
	    }
	}
    }
}
