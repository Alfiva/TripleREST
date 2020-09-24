/*
	Copyright 2018 ITACA-SABIEN, http://www.tsb.upv.es
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
package es.upv.sabien.triplerest.impl.rdf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.inferencer.fc.SchemaCachingRDFSInferencer;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;

import es.upv.sabien.triplerest.api.TripleREST;
import es.upv.sabien.triplerest.api.TripleRESTException;
import es.upv.sabien.triplerest.api.Utils;

/**
 * Implementation of Triple REST API using RDF4J Java APIs.
 * 
 * @author alfiva
 *
 */
public class RDF4JTripleRESTImpl implements TripleREST {

    Repository myRepository;
    RepositoryConnection con;

    public RDF4JTripleRESTImpl() {
	super();
	String dataPath = System.getProperty(
		"es.upv.sabien.triplerest.impl.rdf4j.store",
		"data/TripleREST/store");
	File dataDir = new File(dataPath);
	String indexes = "spoc,posc,cosp";
	try {
	    myRepository = new SailRepository(
		    new SchemaCachingRDFSInferencer(
			    new NativeStore(dataDir, indexes)));
	    myRepository.init();
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
    
    public String get(String s, String p, String o)
	    throws TripleRESTException {
	return get(s,p,o,0,0,0);
    }

    public String get(String s, String p, String o, int sdepth, int pdepth, int odepth) 
	    throws TripleRESTException {
	if (Utils.coordinates(s, p, o) == Utils.XXX) {
	    throw new TripleRESTException(
		    "Operation not allowed in this triple. "
			    + "GET must at least have a valid /subject.");
	}
	ByteArrayOutputStream stream = new ByteArrayOutputStream();
	Resource subj=(s!=null)?con.getValueFactory().createIRI(s):null;
	IRI pred=(p!=null)?con.getValueFactory().createIRI(p):null;
	try {
	    IRI obj=(o!=null)?con.getValueFactory().createIRI(o):null;
	    Rio.write(Iterations.asList(con.getStatements(subj, pred, obj)), stream, RDFFormat.TURTLE);
	} catch (IllegalArgumentException e) {
	    // Not a IRI, try literal
	    Literal obj=processLiteral(o);
	    Rio.write(Iterations.asList(con.getStatements(subj, pred, obj)), stream, RDFFormat.TURTLE);
	}
	try {
	    return stream.toString("UTF-8");
	} catch (UnsupportedEncodingException e) {
	    return null; // Should not happen
	}
    }
    
    public boolean del(String s, String p, String o) throws TripleRESTException {
	Resource subj=(s!=null)?con.getValueFactory().createIRI(s):null;
	IRI pred=(p!=null)?con.getValueFactory().createIRI(p):null;
	try {
	    IRI obj=(o!=null)?con.getValueFactory().createIRI(o):null;
	    con.remove(subj, pred, obj);
	} catch (IllegalArgumentException e) {
	    // Not a IRI, try literal
	    Literal obj=processLiteral(o);
	    con.remove(subj, pred, obj);
	} catch (Exception e) {
	    e.printStackTrace();
	    return false;
	}
	return true;
    }

    public boolean post(String s, String p, String o, String in)
	    throws TripleRESTException {
	if (Utils.coordinates(s, p, o) != Utils.SPX) {
	    throw new TripleRESTException(
		    "Operation not allowed in this triple. "
			    + "POST can only handle /subject/predicate");
	}
	Resource subj=(s!=null)?con.getValueFactory().createIRI(s):null;
	IRI pred=(p!=null)?con.getValueFactory().createIRI(p):null;
	try {
	    String rootIRI=Utils.getRootIRI(in);
	    InputStream stream = new ByteArrayInputStream(in.getBytes("UTF-8"));
	    Model model = Rio.parse(stream, rootIRI, RDFFormat.TURTLE);
	    con.add(subj, pred, con.getValueFactory().createIRI(rootIRI)); // Add base stmt
	    con.add(model); // Add rest of stmts
	} catch (Exception e1) {
	    e1.printStackTrace();
	    return false;
	}
	return true;
    }

    public boolean put(String s, String p, String o, String in)
	    throws TripleRESTException {
	switch (Utils.coordinates(s, p, o)) {
	case Utils.SXX:
	    try {
		del(s, null, null);
		String rootIRI = Utils.getRootIRI(in);
		InputStream stream = new ByteArrayInputStream(
			in.getBytes("UTF-8"));
		Model model = Rio.parse(stream, rootIRI, RDFFormat.TURTLE);
		con.add(model); // Add rest of stmts
	    } catch (Exception e1) {
		e1.printStackTrace();
		return false;
	    }
	    break;
	case Utils.SPX:
	case Utils.SPO:
	    del(s, p, o); // In SPX o is null
	    put(s, p, null, in);
	    break;
	default:
	    throw new TripleRESTException(
		    "Operation not allowed in this triple. "
			    + "PUT can only handle /subject, /subject/predicate "
			    + "or /subject/predicate/object");
	}
	return true;
    }

    /**
     * Parses a serialized representation of a Literal in Turtle, in the format
     * "value"^^{@literal<}namespace#type{@literal>}
     * 
     * @param serial
     *            The serialization of the literal
     * @return The Literal value
     * @throws TripleRESTException
     *             If the serialization does not follow the right format, or
     *             some other issue happened during creation.
     */
    private Literal processLiteral(String serial) throws TripleRESTException {
	Literal obj;
	try {
	    String[] parts = serial.split("^^", 2);
	    if (parts[0].startsWith("\"")) {
		parts[0] = parts[0].substring(1, parts[0].length() - 1);
	    }
	    obj = con.getValueFactory().createLiteral(parts[0],
		    con.getValueFactory().createIRI(parts[1]));
	} catch (Exception ex) {
	    throw new TripleRESTException("Not a valid literal: " + serial, ex);
	}
	return obj;
    }
}
