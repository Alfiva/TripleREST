/*
	Copyright 2018 ITACA-SABIEN, http://www.sabien.upv.es
	Instituto Tecnologico de Aplicaciones de Comunicacion
	Avanzadas - Grupo Tecnologias para la Salud y el
	Bienestar (SABIEN)
 */
package es.upv.sabien.triplerest.impl.rdf4j;

import java.io.ByteArrayOutputStream;
import java.io.File;

import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLWriter;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.turtle.TurtleWriterFactory;
import org.eclipse.rdf4j.sail.inferencer.fc.ForwardChainingRDFSInferencer;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;

import es.upv.sabien.triplerest.api.AbstractSPARQLTripleREST;
import es.upv.sabien.triplerest.api.Utils;

/**
 * Implementation of Triple REST API using RDF4J SPARQL interface.
 * 
 * @author alfiva
 *
 */
public class RDF4JSPARQLTripleRESTImpl extends AbstractSPARQLTripleREST {

    private static final int SELECT = 0, CONSTRUCT = 1, DESCRIBE = 2, ASK = 3,
	    UPDATE = 4, NONE = -1;

    Repository myRepository;
    RepositoryConnection con;

    public RDF4JSPARQLTripleRESTImpl() {
	super();
	String dataPath = System.getProperty(
		"es.upv.sabien.triplerest.impl.rdf4j.store",
		"data/TripleREST/store");
	File dataDir = new File(dataPath);
	String indexes = "spoc,posc,cosp";
	try {
	    myRepository = new SailRepository(
		    new ForwardChainingRDFSInferencer(
			    new NativeStore(dataDir, indexes)));
	    myRepository.initialize();
	    con = myRepository.getConnection();
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
    
    /**
     * Closes the connection to the underlying RDF4J repository.
     */
    public void close(){
	try {
	    con.close();
	    myRepository.shutDown();
	} catch (RepositoryException e) {
	    e.printStackTrace();
	}
    }

    @Override
    public String doSPARQL(String query) {
	String result = null;
	try {
	    ByteArrayOutputStream stream = new ByteArrayOutputStream();
	    switch (getQueryType(query)) {
	    case SELECT:
		SPARQLResultsXMLWriter selectWriter = new SPARQLResultsXMLWriter(stream);
		TupleQuery tquery = con.prepareTupleQuery(QueryLanguage.SPARQL, query);
		tquery.evaluate(selectWriter);
		result = stream.toString("UTF-8");
		break;
	    case ASK:
		BooleanQuery bquery = con.prepareBooleanQuery(QueryLanguage.SPARQL, query);
		result = bquery.evaluate() ? "true" : "false";
		break;
	    case CONSTRUCT:
		TurtleWriterFactory factory1 = new TurtleWriterFactory();
		RDFWriter construtWriter = factory1.getWriter(stream);
		GraphQuery cquery = con.prepareGraphQuery(QueryLanguage.SPARQL, query);
		cquery.evaluate(construtWriter);
		result = stream.toString("UTF-8");
		factory1 = null;
		break;
	    case DESCRIBE:
		TurtleWriterFactory factory2 = new TurtleWriterFactory();
		RDFWriter describeWriter = factory2.getWriter(stream);
		GraphQuery dquery = con.prepareGraphQuery(QueryLanguage.SPARQL, query);
		dquery.evaluate(describeWriter);
		result = stream.toString("UTF-8");
		factory2 = null;
		break;
	    case UPDATE:
		Update uquery = con.prepareUpdate(QueryLanguage.SPARQL, query);
		uquery.execute();
		result = "true";
		break;
	    case NONE:
		throw new MalformedQueryException(
			"A SPARQL query must contain one of SELECT, "
				+ "CONSTRUCT, DESCRIBE, ASK, or UPDATE in "
				+ "case of SPARQL Updates.");
	    default:
		throw new MalformedQueryException("Unknown SPARQL Query.");
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
	return result;
    }

    /**
     * Finds out what kind of SPARQL query the input is.
     * 
     * @param input
     *            The SPARQL query to analyze
     * @return The value of the constant indicating the type of query. One of
     *         <code>SELECT</code>,
     *         <code>CONSTRUCT</code>,
     *         <code>DESCRIBE</code>,
     *         <code>ASK</code>, <code>UPDATE</code>
     *         or <code>NONE</code>
     */
    private int getQueryType(String input) {
	int[] indexes = { input.indexOf("SELECT"), input.indexOf("CONSTRUCT"),
		input.indexOf("DESCRIBE"), input.indexOf("ASK"),
		input.indexOf("INSERT"), input.indexOf("DELETE") };
	int value = input.length();
	int index = -1;
	for (int i = 0; i < indexes.length; i++) {
	    int current = indexes[i];
	    if (current > -1 && current < value) {
		value = current;
		index = i;
	    }
	} // Finds out what SPARQL keyword goes first
	if (index > UPDATE) {
	    return UPDATE; // If DELETE treat as INSERT
	}
	return index;
    }

    @Override
    public String getRootIRI(String arg0) {
	// TODO 
	return Utils.getRootIRI(arg0);
    }

}
