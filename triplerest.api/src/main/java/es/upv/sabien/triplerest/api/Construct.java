/*
	Copyright 2018 ITACA-SABIEN, http://www.sabien.upv.es
	Instituto Tecnologico de Aplicaciones de Comunicacion
	Avanzadas - Grupo Tecnologias para la Salud y el
	Bienestar (SABIEN)
 */
package es.upv.sabien.triplerest.api;

/**
 * Utility class to build SPARQL Construct queries.
 * 
 * @author alfiva
 *
 */
public class Construct {

    private StringBuilder prefixes;
    private StringBuilder variables;
    private StringBuilder wheres;

    /**
     * Create a new empty Construct query builder.
     */
    public Construct() {
	prefixes = new StringBuilder();
	variables = new StringBuilder();
	wheres = new StringBuilder();
    }

    /**
     * Add prefixes to the prefix section.
     * 
     * @param prf
     *            Prefix to add.
     */
    public void prefix(String prf) {
	prefixes.append(prf);
    }

    /**
     * Add an arbitrary segment to the construct section.
     * 
     * @param con
     */
    public void construct(String con) {
	variables.append(" ").append(con).append(" ");
    }

    /**
     * Add graph elements to the construct section.
     * 
     * @param s
     *            Add a subject
     * @param p
     *            Add a predicate
     * @param o
     *            Add an object
     */
    public void construct(String s, String p, String o) {
	variables.append(" ").append(s != null ? s : "?s").append(" ")
		.append(p != null ? p : "?p").append(" ")
		.append(o != null ? o : "?o").append(" . ");
    }

    /**
     * Add an arbitrary segment to the where section
     * 
     * @param whe
     */
    public void where(String whe) {
	wheres.append(" ").append(whe).append(" ");
    }

    /**
     * Add conditions to the where section.
     * 
     * @param s
     *            Add a subject
     * @param p
     *            Add a predicate
     * @param o
     *            Add an object
     */
    public void where(String s, String p, String o) {
	wheres.append(" ").append(s != null ? s : "?s").append(" ")
		.append(p != null ? p : "?p").append(" ")
		.append(o != null ? o : "?o").append(" . ");
    }

    /**
     * Finalize the query and build it.
     * 
     * @return The built query
     */
    public String build() {
	return prefixes.toString() + " \n CONSTRUCT { " + variables.toString()
		+ " } WHERE { " + wheres.toString() + " }";
    }
}
