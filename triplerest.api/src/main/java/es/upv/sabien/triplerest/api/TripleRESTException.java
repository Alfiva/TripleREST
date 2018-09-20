/*
	Copyright 2018 ITACA-SABIEN, http://www.sabien.upv.es
	Instituto Tecnologico de Aplicaciones de Comunicacion
	Avanzadas - Grupo Tecnologias para la Salud y el
	Bienestar (SABIEN)
 */
package es.upv.sabien.triplerest.api;

/**
 * Exception caused during processing of a request through the Triple REST API.
 * 
 * @author alfiva
 *
 */
public class TripleRESTException extends Exception {

    /**
     * Auto-generated.
     */
    private static final long serialVersionUID = 6538057393387959311L;

    public TripleRESTException(String string) {
	super(string);
    }

    public TripleRESTException(String string, Throwable e) {
	super(string, e);
    }
}
