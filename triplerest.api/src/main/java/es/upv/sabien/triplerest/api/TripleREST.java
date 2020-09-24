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
package es.upv.sabien.triplerest.api;

/**
 * Triple-based REST interface API.
 * 
 * @author alfiva
 *
 */
public interface TripleREST {
    /**
     * REST GET on path /s/p/o. Each s, p or o parameter can be a IRI or null,
     * for wildcard. Also, o can be a literal. Take into account however that
     * setting all values to null is not a valid combination.
     * <p>
     * IRIs must be expanded (no prefixes) and be a paresable IRI.
     * <p>
     * Literals must be written fully, in the form "value"^^{@literal 
     * <}namespace#type{@literal >} .
     * <p>
     * The extra "depth" parameters determine how many additional levels of
     * properties will be queried for the subject, predicate or object. For
     * instance if sdepth=2 means that the result will include (1) all
     * properties of the subject, and (2) all properties of each of its
     * properties objects. The minimum is the default, 0, which just like 1.
     * 
     * @param s
     *            The subject part of the path. Must be a IRI or null.
     * @param p
     *            The predicate part of the path. Must be a IRI or null.
     * @param o
     *            The object part of the path. Must be a IRI, a literal or null.
     * @param sdepth
     *            Number of extra levels to describe the subject.
     * @param pdepth
     *            Number of extra levels to describe the predicate.
     * @param odepth
     *            Number of extra levels to describe the object.
     * @return The Turtle serialization of the query result. Null if there was
     *         some unknown error. Empty string if the query was successful but
     *         there was nothing to return.
     * @throws TripleRESTException
     *             If the request could not be processed by the API.
     */
    public abstract String get(String s, String p, String o, int sdepth, int pdepth, int odepth) throws TripleRESTException;
    /**
     * Equivalent to get(String s, String p, String o, 0, 0, 0)
     * 
     * @param s
     *            The subject part of the path. Must be a IRI or null.
     * @param p
     *            The predicate part of the path. Must be a IRI or null.
     * @param o
     *            The object part of the path. Must be a IRI, a literal or null.
     * @return The Turtle serialization of the query result. Null if there was
     *         some unknown error. Empty string if the query was successful but
     *         there was nothing to return.
     * @throws TripleRESTException
     *             If the request could not be processed by the API.
     */
    public abstract String get(String s, String p, String o) throws TripleRESTException;
    
    /**
     * REST POST on path /s/p/o. Each s, p or o parameter can be a IRI or null,
     * for wildcard. Also, o can be a literal. Take into account however that
     * the only allowed combination is s!=null, p!=null, o==null.
     * <p>
     * IRIs must be expanded (no prefixes) and be a paresable IRI.
     * <p>
     * Literals must be written fully, in the form "value"^^{@literal 
     * <}namespace#type{@literal >} .
     * <p>
     * 
     * @param s
     *            The subject part of the path. Must be a IRI.
     * @param p
     *            The predicate part of the path. Must be a IRI.
     * @param o
     *            The object part of the path. Should be null.
     * @param input
     *            The Turtle serialization of the input value being POSTed. Must
     *            not be null. Exceptionally, can be only a IRI or a Literal,
     *            fully expanded (no prefixes).
     * @return True if the POST succeeded.
     * @throws TripleRESTException
     *             If the request could not be processed by the API.
     */
    public abstract boolean post(String s, String p, String o, String input) throws TripleRESTException;
    
    /**
     * REST POST on path /s/p/o. Each s, p or o parameter can be a IRI or null,
     * for wildcard. Also, o can be a literal. Take into account however that
     * the only allowed combinations are s!=null, p==null, o==null; s!=null,
     * p!=null, o==null; or none null.
     * <p>
     * IRIs must be expanded (no prefixes) and be a paresable IRI.
     * <p>
     * Literals must be written fully, in the form "value"^^{@literal 
     * <}namespace#type{@literal >} .
     * 
     * @param s
     *            The subject part of the path. Must be a IRI.
     * @param p
     *            The predicate part of the path. Must be a IRI or null.
     * @param o
     *            The object part of the path. Must be a IRI, a literal or null.
     * @param input
     *            The Turtle serialization of the input value being PUT. Must
     *            not be null. Exceptionally, can be only a IRI or a Literal,
     *            fully expanded (no prefixes).
     * @return True if the PUT succeeded.
     * @throws TripleRESTException
     *             If the request could not be processed by the API.
     */
    public abstract boolean put(String s, String p, String o, String input) throws TripleRESTException;
    /**
     * REST POST on path /s/p/o. Each s, p or o parameter can be a IRI or null,
     * for wildcard. Also, o can be a literal. Take into account however that
     * the only allowed combinations are s!=null, p==null, o==null; s!=null,
     * p!=null, o==null; or none null.
     * <p>
     * IRIs must be expanded (no prefixes) and be a paresable IRI.
     * <p>
     * Literals must be written fully, in the form "value"^^{@literal 
     * <}namespace#type{@literal >} .
     * 
     * @param s
     *            The subject part of the path. Must be a IRI.
     * @param p
     *            The predicate part of the path. Must be a IRI or null.
     * @param o
     *            The object part of the path. Must be a IRI, a literal or null.
     * @return True if the DELETE succeeded.
     * @throws TripleRESTException
     *             If the request could not be processed by the API.
     */
    public abstract boolean del(String s, String p, String o) throws TripleRESTException;
}
