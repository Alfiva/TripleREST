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
package es.upv.sabien.triplerest.server.spark;

import java.io.IOException;
import java.util.Hashtable;
import java.util.StringTokenizer;

import org.osgi.service.http.HttpContext;

import spark.Request;
import spark.Response;

/**
 * Utility class used for simple authentication.
 * 
 * @author alfiva
 * 
 */
public class Authenticator{
    
    /**
     * Authentication realm
     */
    private static final String REALM = "universAAL";
    /**
     * In memory list of user-pwd pairs, to avoid constant use of the DB
     */
    private static Hashtable<String, String> users = new Hashtable<String, String>(); //TODO Clean from time to time?

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.service.http.HttpContext#handleSecurity(javax.servlet.http.
     * HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public boolean handleSecurity(Request rq,
	    Response rs) throws IOException {
	String authHeader = rq.headers("Authorization");

	if (authHeader != null) {
	    String[] userPass = getUserAndPass(authHeader);
	    if (authenticate(userPass[0],userPass[1])) {
		rq.session().attribute(HttpContext.AUTHENTICATION_TYPE, "Basic");
		rq.session().attribute(HttpContext.REMOTE_USER, userPass[0]);
		return true;
	    }
	}

	rs.header("WWW-Authenticate", "Basic realm=\"" + REALM + "\"");
	return false;
    }

    /**
     * Method that checks the proper authentication of a user-pwd pair.
     * 
     * @param user
     *            User
     * @param pass
     *            Password
     * @return true if authentication is correct and no errors occured
     */
    private boolean authenticate(String user, String pass) {
	String storedpass = users.get(user);
	if (storedpass != null) {
	    // user already in the memory list
	    return storedpass.equals(pass);
	} else {
	    //TODO Auth
	    users.put(user, pass);
	    return true;
//	    // user not in the memory list, check DB
//	    if (Activator.getPersistence().checkUser(user)) {
//		// user in the DB
//		if (Activator.getPersistence().checkUserPWD(user, pass)) {
//		    // good pwd
//		    users.put(user, pass);
//		    return true;
//		} else {
//		    // This user does not have the same PWD it registered.
//		    // Impostor!
//		    return false;
//		}
//	    } else {
//		// user not in DB
//		Activator.getPersistence().storeUserPWD(user, pass);
//		users.put(user, pass);
//		return true;
//		// New users are always welcome
//	    }
	}
    }
    
    /**
     * Parses the Authorization header for BASIC authentication
     * 
     * @param auth
     *            The BASE64 encoded user:pass values. If null or empty, returns
     *            false
     * @return A String array of two elements containing the user and pass as
     *         first and second element
     */
    public String[] getUserAndPass(String auth) {
	if (auth == null || auth.isEmpty())
	    return null;
	StringTokenizer authTokenizer = new StringTokenizer(auth, " ");
	if (authTokenizer.hasMoreTokens()) {
	    // assume BASIC authentication type
	    String authType = authTokenizer.nextToken();
	    if ("basic".equalsIgnoreCase(authType)) {
		String credentials = authTokenizer.nextToken();

		String userPassString = new String(Base64.decode(credentials));
		// The decoded string is in the form
		// "userID:password".
		int p=-1;
		if(userPassString.startsWith("http")){
		    //There will be a first : after http/s
		    p = userPassString.indexOf(":", userPassString.indexOf(":"));
		}else{
		    p = userPassString.indexOf(":");
		}
		if (p != -1) {
		    String userID = userPassString.substring(0, p);
		    String pass = userPassString.substring(p + 1);
		    return new String[] { userID, pass };
		}
	    }

	}
	return null;
    }

}
