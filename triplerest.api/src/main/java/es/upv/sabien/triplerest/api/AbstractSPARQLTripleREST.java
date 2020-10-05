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
package es.upv.sabien.triplerest.api;

/**
 * Abstract implementation of Triple REST interface that converts its method
 * calls into a SPARQL query to be executed by any custom class that extends
 * this one. That class only has to implement the doSPARQL method. This is
 * intended for making easy implementations that interface with a SPARQL
 * endpoint.
 * 
 * @author alfiva
 *
 */
public abstract class AbstractSPARQLTripleREST implements TripleREST {

    public String get(String s, String p, String o) throws TripleRESTException {
	return get(s, p, o, 1, 1, 1);
    }

    public String get(String s, String p, String o, int sdepth, int pdepth,
	    int odepth) throws TripleRESTException {

	// Check valid operation
	if (Utils.coordinates(s, p, o) == Utils.XXX) {
	    throw new TripleRESTException(
		    "Operation not allowed in this triple. "
			    + "GET must at least have a valid /subject.");
	}
	
	// Check valid params
	if(s!=null && Utils.isIRI(s)) s="<"+s+">";
	if(p!=null && Utils.isIRI(p)) p="<"+p+">";
	if(o!=null && Utils.isIRI(o)) o="<"+o+">";

	// Build query
	Construct q = new Construct();
	q.construct(s, p, o);
	q.where(s, p, o);

	// This is for extra information, but not like CBD
	String sxs = (s == null) ? "?s" : s;
	String pxs = "?sp";
	String oxs = "?so";
	for (int ss = 0; ss < sdepth; ss++) {
	    pxs = pxs + "x";
	    oxs = oxs + "x";
	    q.construct(sxs, pxs, oxs);
	    q.where(sxs, pxs, oxs);
	    sxs = pxs;
	}

	String sxp = (p == null) ? "?p" : p;
	String pxp = "?pp";
	String oxp = "?po";
	for (int ps = 0; ps < pdepth; ps++) {
	    pxp = pxp + "x";
	    oxp = oxp + "x";
	    q.construct(sxp, pxp, oxp);
	    q.where(sxp, pxp, oxp);
	    sxp = pxp;
	}

	String sxo = (o == null) ? "?o" : o;
	String pxo = "?op";
	String oxo = "?oo";
	for (int os = 0; os < odepth; os++) {
	    pxo = pxo + "x";
	    oxo = oxo + "x";
	    q.construct(sxo, pxo, oxo);
	    q.where(sxo, pxo, oxo);
	    sxo = pxo;
	}

	// Execute query
	System.out.println("BUILT QUERY IS: \n" + q.build());
	return doSPARQL(q.build());
    }

    public boolean post(String s, String p, String o, String input)
	    throws TripleRESTException {
	String query, root;
	String[] prefixes_triples;

	// Check valid operation
	if (Utils.coordinates(s, p, o) != Utils.SPX) {
	    throw new TripleRESTException(
		    "Operation not allowed in this triple. "
			    + "POST can only handle /subject/predicate");
	}
	
	// Check valid params
	if(s!=null && Utils.isIRI(s)) s="<"+s+">";
	if(p!=null && Utils.isIRI(p)) p="<"+p+">";
	if(o!=null && Utils.isIRI(o)) o="<"+o+">"; // Actually not needed

	// Process input
	if (input != null && !input.isEmpty()) {
	    if (input.trim().endsWith(".")){ // Resource described with triples
		prefixes_triples = Utils.splitPrefixes(input);
		root = "<"+getRootIRI(input)+">";
	    }else if(Utils.isIRI(input)){ // IRI
		root="<"+input+">";
		prefixes_triples = new String[]{"",""};
	    }else if(input.contains("^^")){ // Literal 
		root=input;
		prefixes_triples = new String[]{"",""};
	    }else{
		throw new TripleRESTException("Input is not a valid resource");
	    }
	} else {
	    throw new TripleRESTException("Input is empty or not defined");
	}

	// Build query
	query = prefixes_triples[0] + "INSERT DATA { " + s + " " + p + " "
		+ root + " . " + prefixes_triples[1] + " }";

	// Execute query
	System.out.println("BUILT QUERY IS: \n" + query);
	String result = doSPARQL(query);
	return (result != null && result.contains("true"));
    }

    public boolean put(String s, String p, String o, String input)
	    throws TripleRESTException {
	String query, root;
	String[] prefixes_triples;

	// Process input
	if (input != null && !input.isEmpty()) {
	    if (input.trim().endsWith(".")){ // Resource described with triples
		prefixes_triples = Utils.splitPrefixes(input);
		root = "<"+getRootIRI(input)+">";
	    }else if(Utils.isIRI(input)){ // IRI
		root="<"+input+">";
		prefixes_triples = new String[]{"",""};
	    }else if(input.contains("^^")){ // Literal 
		root=input;
		prefixes_triples = new String[]{"",""};
	    }else{
		throw new TripleRESTException("Input is not a valid resource");
	    }
	} else {
	    throw new TripleRESTException("Input is empty or not defined");
	}
	
	// Check valid params
	if(s!=null && Utils.isIRI(s)) s="<"+s+">";
	if(p!=null && Utils.isIRI(p)) p="<"+p+">";
	if(o!=null && Utils.isIRI(o)) o="<"+o+">";

	// Check valid operation / Build query
	switch (Utils.coordinates(s, p, o)) {
	case Utils.SXX:
	    if (!Utils.isIRI(root))
		throw new TripleRESTException(
			"Cannot change resource into literal");
	    query = prefixes_triples[0] + " DELETE { " + s + " ?p ?o . ?ss ?pp "
		    + s + " } " + " INSERT { ?ss ?pp " + root + " . "
		    + prefixes_triples[1] + " } " + " WHERE { " + s
		    + " ?p ?o . OPTIONAL { ?ss ?pp " + s + " } }";
	    break;
	case Utils.SPX:
	    query = prefixes_triples[0] + " DELETE { " + s + " " + p + " ?o } "
		    + " INSERT { " + s + " " + p + " " + root + " . "
		    + prefixes_triples[1] + " } " + " WHERE { " + s + " " + p
		    + " ?o }";
	    break;
	case Utils.SPO:
	    query = "DELETE DATA { " + s + " " + p + " " + o + " } ; "
		    + prefixes_triples[0] + " INSERT DATA { " + s + " " + p
		    + " " + root + " . " + prefixes_triples[1] + " }";
	    break;
	default:
	    throw new TripleRESTException(
		    "Operation not allowed in this triple. "
			    + "PUT can only handle /subject, /subject/predicate "
			    + "or /subject/predicate/object");
	}

	// Execute query
	System.out.println("BUILT QUERY IS: \n" + query);
	String result = doSPARQL(query);
	return (result != null && result.contains("true"));
    }

    public boolean del(String s, String p, String o)
	    throws TripleRESTException {
	String query;
	
	// Check valid params
	if(s!=null && Utils.isIRI(s)) s="<"+s+">";
	if(p!=null && Utils.isIRI(p)) p="<"+p+">";
	if(o!=null && Utils.isIRI(o)) o="<"+o+">";

	// Check valid operation / Build query
	switch (Utils.coordinates(s, p, o)) {
	case Utils.SXX:
	    query = "DELETE { " + s + " ?p ?o . ?ss ?pp " + s + " } WHERE { "
		    + s + " ?p ?o . OPTIONAL { ?ss ?pp " + s + " } }";
	    break;
	case Utils.SPX:
	    query = "DELETE { " + s + " " + p + " ?o } WHERE { " + s + " " + p
		    + " ?o }";
	    break;
	case Utils.SPO:
	    query = "DELETE DATA { " + s + " " + p + " " + o + " }";
	    break;
	default:
	    throw new TripleRESTException(
		    "Operation not allowed in this triple. "
			    + "DELETE can only handle /subject, "
			    + "/subject/predicate or /subject/predicate/object");
	}

	// Execute query
	System.out.println("BUILT QUERY IS: \n" + query);
	String result = doSPARQL(query);
	return (result != null && result.contains("true"));
    }

    /**
     * Method to be implemented by subclasses. This receives a SPARQL query that
     * represents a Triple REST API call. Implementors only need to execute the
     * SPARQL on their data store / endpoint, and then return the appropriate
     * result:
     * <p/>
     * For CONSTRUCT queries -> A Turtle serialization of the resulting graph,
     * or an empty string if it produced nothing.
     * <p/>
     * For INSERT or DELETE queries -> The string "true" if it was executed
     * properly. Anything else otherwise.
     * 
     * @param query
     *            The SPARQL query to be executed. It will be CONSTRUCT, INSERT,
     *            DELETE or a combination of the last two.
     * @return The resulting graph for CONSTRUCT. "true" or anything else for
     *         the other queries.
     */
    public abstract String doSPARQL(String query);

    /**
     * Method to be implemented by subclasses. You must return the IRI of the
     * root resource you are given serialized in the input parameter. This input
     * parameter represents the body of a Triple REST POST or PUT request. As
     * such, it represents a resource to be added, and therefore, among all the
     * triples in its graph, there is a "root" IRI, the IRI of that resource.
     * That is what this method must return.
     * <p/>
     * Because it may be necessary to use a RDF engine to parse the resource and
     * get the root IRI, this method has been extracted and made abstract, so
     * that implementations can use their own engine parser to do that (instead
     * of including an entire parser implementation in this API). Alternatively,
     * you can use the method getRootIRI() from
     * es.upv.sabien.triplerest.api.Utils for a quick implementation, but is not
     * 100% reliable.
     * 
     * @param input A serialized resource
     * @return The IRI of that resource (without {@literal <} {@literal >})
     */
    public abstract String getRootIRI(String input);
}
