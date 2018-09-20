/*
	Copyright 2018 ITACA-SABIEN, http://www.sabien.upv.es
	Instituto Tecnologico de Aplicaciones de Comunicacion
	Avanzadas - Grupo Tecnologias para la Salud y el
	Bienestar (SABIEN)
 */
package es.upv.sabien.triplerest.api;

import java.net.URI;

/**
 * Utility class with convenient methods that can be used not only by the
 * Triple REST interface implementations but also by any third party code
 * that uses the API.
 * 
 * @author alfiva
 *
 */
public class Utils {
    
    public static final int XXX = 0x0;
    public static final int XXO = 0x1;
    public static final int XPX = 0x2;
    public static final int XPO = 0x3;
    public static final int SXX = 0x4;
    public static final int SXO = 0x5;
    public static final int SPX = 0x6;
    public static final int SPO = 0x7;
    
    /**
     * Figure out what type of Triple-based path is being processed.
     * 
     * @param s
     *            The subject part of the path.
     * @param p
     *            The predicate part of the path.
     * @param o
     *            The object part of the path.
     * @return One of the "XXX" constants in Utils. E.g: SPX means s and p
     *         are specified and o is not (wildcard).
     */
    public static int coordinates(String s, String p, String o){
	int sPart=(s!=null)?SXX:XXX;
	int pPart=(p!=null)?XPX:XXX;
	int oPart=(o!=null)?XXO:XXX;
	return sPart+pPart+oPart;
    }
    
    /**
     * Determine if the passed parameter represents a resource IRI.
     * 
     * @param str
     *            Parameter to determine
     * @return true if it is a IRI
     */
    public static boolean isIRI(String str){
	try {
	    new URI(str);
	    return true;
	} catch (Exception e) {
	    return false;
	} 
    }
    
    /**
     * Given a Turtle-serialized representation of a resource, it splits it in
     * two parts, the first containing all the @prefix, and the second one
     * containing the triples.
     * 
     * @param serialized
     *            The serialized resource
     * @return A length 2 array with [prefix part, triples part]
     */
    public static String[] splitPrefixes(String serialized) {
	int lastprefix = 0, lastprefixdot = 0, lastprefixuri = 0;
	lastprefix = serialized.toLowerCase().lastIndexOf("@prefix");
	if (lastprefix >= 0) {
	    lastprefixuri = serialized.substring(lastprefix).indexOf(">");
	    lastprefixdot = serialized.substring(lastprefix + lastprefixuri)
		    .indexOf(".");
	}
	String[] result = new String[2];
	result[0] = serialized
		.substring(0, lastprefixuri + lastprefixdot + lastprefix + 1)
		.replace("@", " ").replace(">.", "> ").replace(" .", " ")
		.replace(". ", " ");
	result[1] = serialized.substring(lastprefixuri + lastprefixdot
		+ lastprefix + 1);
	return result;
    }

    public static String getRootIRI(String input) {
	String[] prefixes_triples=splitPrefixes(input);
	String iri = prefixes_triples[1].trim();
	iri=iri.substring(0, iri.indexOf(" ")).trim();
	if(iri.startsWith("<")){ // Expanded form
	    return iri.substring(1, iri.length()-1);
	}else{ // Prefixed form
	    String prefix=iri.substring(0, iri.indexOf(":"));
	    String namespace = prefixes_triples[0].substring(prefixes_triples[0].indexOf("@prefix "+prefix+":"));
	    namespace = namespace.substring(namespace.indexOf("<")+1, namespace.indexOf(">"));
	    return namespace+iri.substring(iri.indexOf(":")+1);
	}
    }

}
